# 线上无用代码分析

[English](README.md) | **[简体中文](README-zh.md)**

> 基于线上用户上报的无用代码分析

由于代码设计不合理以及keep规则限制等原因，静态代码检查无法找出所有的无用代码。

我们可以从用户的角度去分析，对每个类插桩，执行时将信息上报到服务器。基于大量用户上报，用户没有用到的类可以被定义为无用类。

在抖音项目中，我们发现了1/6的无用类，不包含其引用的资源，共计3M（dex大小20M），如果能全部删除，将减少5%包大小。

## 接入方式：

#### 1. 添加classpath

```groovy
classpath "com.bytedance.android.byteX:coverage-lib:${Versions.BYTEX_VERSION}"
classpath "com.bytedance.android.byteX:coverage-plugin:${Versions.BYTEX_VERSION}"
```

#### 2. 应用插件

应用插件后，在打包过程中会插桩，在init中插入CoverageHandler.addData(123);

这个数字记录了类或者方法的信息，可以通过生成的映射表来解析

```groovy
apply plugin: "bytex.coverage"
CoveragePlugin{
    logLevel "DEBUG"
    // we perfer input from CI
    enable true
    enableInDebug false
  	// only report <clinit> for better performance
  	clInitOnly true
  	whiteList = ["com/ss/android/ugc/bytex/+"]
}
```

#### 3. 添加依赖

插桩会调用一个接口，我们添加依赖，实现这个接口用来处理上报信息

```groovy
implementation "com.bytedance.android.byteX:coverage_lib:${Versions.BYTEX_VERSION}"
```

#### 4. 注册上报

~~~java
// Avoid use this Implementation, just a demo
CoverageHandler.init(DemoCoverageImp.getInstance());
~~~


#### 5. 添加混淆
```
-keep class com.ss.android.ugc.bytex.coverage_lib.** { *; }
```



## 映射文件

默认保存在app/build/ByteX/${variant}Release/CoveragePlugin/coverage中

其中mapping_latest.txt记录了id和类的映射关系，可用来解析上报的int类型的数据

graph文件是类图，用来梳理父类子类的关系

## 解析数据

将id转换为类信息，并计算出无用类、类调用热度等，可参考test目录下相关代码