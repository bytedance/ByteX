English | **[简体中文](README_zh.md)**

# ByteX（Infinite Possibilities）



<h1 align="center">
  <img src="wiki/bytex-logo.png" height="111" width="340"  alt="logo" />
 </h1>

[![Download](https://api.bintray.com/packages/tellh/maven/common/images/download.svg)](https://bintray.com/tellh/maven/common/_latestVersion) [![GitHub license](https://img.shields.io/badge/license-Apache%202-blue)](https://github.com/bytedance/ByteX/blob/master/LICENSE)

> Powered by ByteDance TikTok & Douyin Android team.


ByteX is a bytecode plugin platform based on Android Gradle Transform Api and ASM. 

 (Maybe you can think of it as a socket with unlimited plugs?)


In the apk building process, each plugin is completely independent. It not only can run independently from the ByteX host, but also can be automatically integrated into the host into a single Transform along with other plugins. 

What's more, each plugin's code is decoupled from one another, as well as their host, which makes it extensible and highly efficient for the development of new plugins.

### Background

If all features are developed as a separate plugin, each plugin will cost 10+s, and the compilation time will increase linearly. 

But if  the iterative development in only one plugin module will make it more and more cluttered, for the code is deeply coupled. 

So an idea was raised. It could make sense to build a bytecode plugin platform, and the new feature can be extended based on it as a new plugin.

### Feature

- Code reuse. The common code sinks to `common` module and is reused by all plugins, so that each plugin only needs to focus on bytecode operation.

- Plugin code is isolated and decoupled from each other. Based on ByteX, each feature is independent as a single plugin, and the code of each plugin is isolated from each other, which is conducive to develop a new plugin.

- Platformization makes `Transform` more efficient.

  - Class files are processed in multiple threads concurrently, making full use of the CPU resources of your machine.

  - Plugins are automatically and seamlessly integrated into a single `Transform` to improve the efficiency of processing. During the `Transform` process, the IO of the class file is time-consuming. Integrating all the plugins into a single transform can avoid the costing  time increasing linearly. It makes "1 + 1 = 2"  become "1 + 1 <2" or approximately equal to 1.

- Plugin portability is outstanding. Each plugin can be separated from the ByteX host, working as a transform independently.

### Structure

![structure](wiki/structure.png)



**TransformEngine**

Reading all the class files in the project and Android SDK, and writing back to the specified directory.

**base-plugin**

ByteX host.

**common**

Basic code library, including class diagram construction, logs, interfaces provided to various plugins.

**other plugin modules**

Depending on `common` module and focusing on bytecode operation.



## Quick Start

Add those configuration code to your build.gradle, and apply your plugins on demand.

```groovy
buildscript {
    ext.plugin_version="0.1.9"
    repositories {
        google()
        jcenter()
    }
  
    dependencies {
        classpath "com.bytedance.android.byteX:base-plugin:${plugin_version}"
      	// Add bytex plugins' dependencies on demand
        classpath "com.bytedance.android.byteX:refer-check-plugin:${plugin_version}"
      	// ...
    }
}

apply plugin: 'com.android.application'
// apply bytex host
apply plugin: 'bytex'
ByteX {
    enable true
    enableInDebug false
    logLevel "DEBUG"
}

// apply bytex plugins on demand...
apply plugin: 'bytex.refer_check'
// ...
```

Note: If ByteX host is not applied, there is no difference between ByteX plugins and ordinary ones, and all of them will run separately. On the contrary, all ByteX plugins can be automatically merged into one single Transform with the help of the ByteX host.

## Plugins

- [access-inline-plugin](access-inline-plugin/README.md)（Inline access method）
- [shrink-r-plugin](shrink-r-plugin/README.md)（Slimming R files&Unused resource check）
- [closeable-check-plugin](closeable-check-plugin/README.md)（Detect opening stream without close）
- [const-inline-plugin](const-inline-plugin/README.md)（Inline constants）
- [field-assign-opt-plugin](field-assign-opt-plugin/README.md)（Optimize redundant assignment instructions）
- [getter-setter-inline-plugin](getter-setter-inline-plugin/README.md) （Inline Getters and Setters）
- [method-call-opt-plugin](method-call-opt-plugin/README.md)（Delete some method invoking instructions clearly e.g. `Log.d`）
- [coverage-plugin](coverage/README.md)（Online code coverage）
- [refer-check-plugin](refer-check-plugin/README.md)（Detect if there is a invocation to a non-existent method and a reference to a non-existent field）
- [serialization-check-plugin](serialization-check-plugin/README.md)（Serialization check）
- [SourceFileKiller](SourceFileKiller/README.md)（Shrink SourceFile and linenumber）
- [ButterKnifeChecker](butterknife-check-plugin/README.md)（detect exceptions caused by cross-module use of ButterKnife）
- [RFileKnife](shrink-r-plugin/RFileKnife-README.md)（Fix R.java code too large）


## Apps Using ByteX

| <img src="https://sf1-ttcdn-tos.pstatp.com/img/ee-finolhu/a0ca113c9c6d4fb49c9b8bb54a392a00~noop.image" alt="抖音" height="60"/> | <img src="https://sf1-ttcdn-tos.pstatp.com/img/ee-finolhu/a0ca113c9c6d4fb49c9b8bb54a392a00~noop.image" alt="tiktok" height="60"/> | <img src="https://sf6-ttcdn-tos.pstatp.com/img/ee-finolhu/034e2e9d3cfe49f8bb0a3367c9afec47~noop.image" alt="今日头条" height="60"/> |
|:-----------:|:-------:|:-------:|
| 抖音 | Tik Tok | 今日头条 |
| <img src="https://sf1-ttcdn-tos.pstatp.com/img/ee-finolhu/6f2b3dc9b3e945a89565dd67a3e1a3b3~noop.image" height="60" width="60"  alt="火山小视频" style="margin: 10px"/> | <img src="https://sf1-ttcdn-tos.pstatp.com/img/ee-finolhu/d9a7c17402164799becb3b62676e5f88~noop.image"  alt="lark" height="60"/> | <img src="wiki/icons/duoshan.png" height="60" width="60"  alt="多闪" style="margin: 10px"/> | 
| 火山小视频 | 飞书 | 多闪 |
|<img src="https://sf1-ttcdn-tos.pstatp.com/img/ee-finolhu/2b49de98334a4c05b875a7d56df9abab~noop.image" height="60" alt="FaceU" /> | <img src="https://sf3-ttcdn-tos.pstatp.com/img/ee-finolhu/5f2b63d1fc904c47a37c89dd439e2b7a~noop.image" height="60" alt="轻颜"/> | <img src="wiki/icons/feiliao.png" height="60" width="60"  alt="飞聊" style="margin:17px"/> |
| FaceU激萌 | 轻颜| 飞聊|

## What else can the ByteX do?
There are more than 25 plugins developed based on ByteX in ByteDance (We only open sourced some of them). You can quickly develop the following related plugins based on ByteX:
- Performance optimization(SPI...)
- Optimize apk size(xxx-inline...)
- Fix Bug
- Code analysis / security scan
- AOP(replace SharedPreferences...)
- and so on

## Contribution

If you have some creative ideas and demands related to bytecode, come to join us to develop a new bytecode plugin based on ByteX!

Please read through our [Developer API](wiki/ByteX-Developer-API-en.md).

## Thanks

- [lancet](https://github.com/eleme/lancet) 
- [booster](https://github.com/didi/booster)
 


## Contact us

If you have any question or advice about ByteX, feel free to join our WeChat group.

<img src="https://github.com/yangzhiqian/StaticResource/blob/master/ByteX/wechat_group.jpeg" alt="WeChat Group" />

Besides,  sending email to yangzhiqian@bytedance.com or  tanlehua@bytedance.com  is also available for you.

## Change Log

[Change Log](CHANGELOG.md)

## License

Apache 2.0


