# Online Unused Code Analysis

[English](README.md) | **[简体中文](README-zh.md)**

> Analysis of useless codes reported by online users

Due to unreasonable code design or the keep rules, static code inspection cannot find all useless code.

So we analyze from the perspective of the user, instrument each class, and report the information to the server during execution. Based on a large number of user reports, classes that are not used \can be defined as unused classes.

In Tiktok, we found 1/6 of the useless classes, excluding the resources they reference, for a total of 3M (Dex size 20M). If delete all, it will reduce the package size by 5%.

## how to use

#### 1. Add classpath

```groovy
classpath "com.bytedance.android.byteX:coverage_lib:${Versions.BYTEX_VERSION}"
classpath "com.bytedance.android.byteX:coverage_plugin:${Versions.BYTEX_VERSION}"
```

#### 2. Apply plugin

After the plugin is applied, the code like `CoverageHandler.addData(123)` will be inserted during `init`;

The input number records the class or method information, which can be decoded by the generated mapping table file.

```groovy
apply plugin: "bytex.coverage"
CoveragePlugin{
    logLevel "DEBUG"
    // we perfer input from CI
    enable System.getProperty("constructor_coverage", "false") == 'true'
    enableInDebug false
  	// only report <clinit> for better performance
  	clInitOnly true
  	whiteList = ["com/ss/android/ugc/bytex/+"]
}
```

#### 3. Add dependency

Instrumentation will call an interface, we add dependencies, and implement this interface to handle reporting information.

```groovy
implementation "com.bytedance.android.byteX:coverage_lib:${Versions.BYTEX_VERSION}"
```

#### 4. Regist report

~~~java
// Avoid to use this Implementation, just a demo
CoverageHandler.init(DemoCoverageImp.getInstance());
~~~


#### 5. Add proguard rules
```
-keep class com.ss.android.ugc.bytex.coverage_lib.** { *; }
```


## Mapping file

The files are saved in `app/build/ByteX/${variant}Release/CoveragePlugin/coverage`.

Mapping_latest.txt is the id-class mapping table file, and the graph file records the parent-child relationship.

## retrace the data

You can convert id to class info with the mapping. And you can locate the useless classes, frequency of code call, etc.

More information can be referred to the code under the test dir.