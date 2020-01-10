**[English](README.md)** | [简体中文](README-zh.md)
## Feature
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Inline and optimize constant fields in the project during compiling. 
## Principle
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; A constant field is a field that does not change during runtime，and whose value is permanently fixed after assignment (Eliminates reflections and modify jvm memory modification).There are two types of constants:

 - **compile-time constant**:Constants whose value can be determined at compile time, or fields that contain the ConstantValues in the bytecode. 

	>For example:public static final String TAG = “MainActivity”; 
- **Run-time constant**: A constant that needs to be initialized at run time to determine the value. A special point about this constant relative to the compile-time constant is that it is assigned to the PUTSTATIC instruction.
    >For example:public static final String TAG = MainActivity.class.getSimpleName();
 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; For the instrcutions which operate with compile-time constants, the plugin will inline the fields(replace GETFIELD with LDC) and then remove them in the classes. The plugin will analyzes the codes which perhaps use reflection, and skip optimization that get run-time constant fields directly by reflection.
 

## Quick Start
* add build classpath

  >classpath "com.bytedance.android.byteX:const-inline-plugin:${plugin_version}"
* apply and configure the plugin in your build.gradle(application)

	```groovy
    apply plugin: 'bytex.const_inline'
    const_inline {
            enable true
            enableInDebug true
            logLevel "INFO"
            autoFilterReflectionField = true  //Use the plugin's built-in reflection check to filter out possible reflection constants,  true is recommended
            //supposesReflectionWithString = false //Use plugin built-in string matching may reflect constants, false is recommended
            skipWithRuntimeAnnotation true //Filter out constants with runtime annotations, true is recommended
            skipWithAnnotations = [
                    //Filter out constants annotated, including class annotations
                    "android/support/annotation/Keep",
            ]
            whiteList = [
                    //Skip optimized list
                    "com/meizu/cloud/*",
            ]
    }
    ```
## Optimization Results
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;More than 85000 constant fields are cut down from Douyin apk, resulting in more than 200KB slimmer than not.<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The optimized result will be printed into the corresponding `app/build/ByteX/ByteX_report_ByteX.html` by default.The result may look like the picture showed below:<br/>
![Result](img/优化结果.png)