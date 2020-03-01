English | [简体中文](RFileKnife-README-zh.md)

## Purpose
&emsp;&emsp;Fix the problem that javac reports an error of `code too large`  because the project has too many resources and the number of fields in the generated R.java is too large.The error stack looks like the following:<br/>

> /root/workspace/xxxx/main/build/generated/not_namespaced_r_class_sources/douyinCnRelease/generateDouyinCnReleaseRFile/out/com/ss/android/ugc/aweme/R.java:21511: error: code too large  public static int abc = 0x7f150001;

## Causes

### First:Constant Pool Overflow
&emsp;&emsp;Every symbol, field, and method in Java code will occupy an index in the constant pool in the Class structure, and the constant index in a class has a limit of 65535, which can be known in the Class file structure:<br/>
<img src="img/ClassFile.png" width="60%" height="60%" align=center><Br/>
&emsp;&emsp;At this point we can know that a `public static int xxx = yyy` occupied by the constant pool size is 4:

1. Utf8:which is field name(xxx)
2. Fieldref : introduced by field assignment statements in class init method
3. NameAndType: introduced by Fieldref
4. Integer: which is field value(yyy） 

&emsp;&emsp;In addition, if it is R in the application module, the fields in R will be `public static final int xxx = yyy;`, under this condition, xxx is a constant field, and there is no initialized assignment instructions. The field's assignment is completed by the field's attributes. A `public static final int xxx = yyy` occupied by the constant pool size is 2:

1. Utf8:which is field name(xxx)
2. Integer: which is field value(yyy） 

&emsp;&emsp;**Conclusion**:Due to the limitation of the constant pool,the number of single type resources in the Library must not exceed 16378, and the number of single type resources in the Application must not exceed 32760.
    
### Second:Method Instruction Count Overflow
&emsp;&emsp;When there is an assignment code in a non-final field, a corresponding assignment statement is generated in the (class) constructor method. An assignment requires an instruction length of 6 bytes (ldc_w + putstatic), and the maximum instruction size of a method is 65535:<br/>
<img src="img/CodeStruct.png" width="60%" height="60%" align=center><Br/>
&emsp;&emsp;So if there are more than 10943 non-static final fields(with assignment) in a class, the number of class constructor method instructions will overflow, and `code too large` will appear in javac。

## Solutions
&emsp;&emsp;Core idea: split Java code.There are two implements in the plugin

### Option One:Split Assignments
&emsp;&emsp;Initialize some of the fields that cannot be placed in the class initialization method into other methods. At the same time, we constructed some initMethod fields and methods We constructed. So that bypassing the method length limitation while ensuring the logic of the original code.The effect is similar to the following figure:<br/>
<img src="img/AssignValue.png" width="60%" height="60%" align=center><Br/>
&emsp;&emsp;Defect: At this time, we avoided the problem of method instruction overflow, but there is another problem is the constant pool limitation above. One field requires 4 constant pools(R in Library), so the maximum field limit of this solution will mount up to 16378<br/>
### Option Two:Class Inheritance Cut On Fields
&emsp;&emsp;Cut all fields into multiple classes, and then use inheritance to include all field values. This solution can make the field expand infinitely, the effect is similar to the following figure：<br/>
<img src="img/AssignInherit.png" width="60%" height="60%" align=center><Br/>
&emsp;&emsp;Defect:Need to be compatible with the ShrinkR plugin defined in ByteX, Take care  the situation of the reflected field.<br/>

## Q&A
- Q:Why problem occurs in the library first, not the app?

> A:The id in R under app is `public static final int`, and there will be no assignment instructions. A field only needs two constant index (Upper limit is 32760), but the `public static int` under library, and a field requires 6 bytes of assignment instructions. (Upper limit is 10943)

- Q:Will modifying R.java source code affect R2 of ButterKnife?

> A:No, the generation of R2 is not directly copying the code of R.java to generate a final version, but reading the textSymbolOutputFile file of processResourcesTask and then using javapoet to generate a new R2.java file. The id value is regenerated. Nor will it have any intersection with R.java.

- Q:Will the `Option Two` conflict with constraintlayout?

> A:No, constraintlayout uses `Class.getField(String)` to reflect the field value first, and fails or `Resource.getIdentifier` to get the id. The plugin has no affect to these two cases.

## Quick Start

* add build classpath(Same as ShrinkR)

  >classpath "com.bytedance.android.byteX:shrink-r-plugin:${plugin_version}"
  
* apply and configure the plugin in your build.gradle(application or library)

    ```groovy
    apply plugin: "bytex.RFileKnife"
        RFileKnife {
        // the upper limit number of fields in one class. Default is 10000 and which is recommended
        limitSize 10000
        //Options：[value|inherit].'value' by default；Note:When using inherit mode in app module and using the ShrinkR plugin of byteX at the same time, you need to open the compatRFileAssignInherit in ShrinkR configuation.
        assignType "inherit"
        //Whether to do a verification to prevent compatibility issues. false by default .
        verifyParse true
        //By default, only the R.java of the package of the current module is processed. Other packages need to be configured.
        packageNames = [
            //"com.ss.android.ugc.aweme",
        ]
        //Fields which will be not moved in inherit mode.Need to configure when there is reflection
        whiteList = [
            // style/abc
            // abc
        ]
    }
    ```