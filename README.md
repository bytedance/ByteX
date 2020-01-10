English | **[简体中文](README_zh.md)**

# ByteX（Infinite Possibilities）



<h1 align="center">
  <img src="wiki/bytex-logo.png" height="111" width="340"  alt="logo" />
 </h1>

> Powered by bytedance douyin android team.


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
  	ext.plugin_version="0.1.0"
    repositories {
        maven { url "https://dl.bintray.com/tellh/maven" }
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

Note: If you do not apply the ByteX host, those ByteX plugins have no difference from ordinary plugins and each will form a separate Transform. On the contrary, all ByteX plugins will automatically merge into a Transform with the help of the ByteX host.

The following are the plugins based on ByteX.  You can learn more usage information about those plugins in their own README.md.

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


## Known Users

<!--<img src="https://sf1-ttcdn-tos.pstatp.com/img/ee-finolhu/a0ca113c9c6d4fb49c9b8bb54a392a00~noop.image" height="60" width="60"  alt="抖音" style="margin:10px"/>-->
<!--<img src="wiki/icons/tiktok.png" height="60" width="150"  alt="tiktok" style="margin: 10px"/>-->
<!--<img src="https://sf6-ttcdn-tos.pstatp.com/img/ee-finolhu/034e2e9d3cfe49f8bb0a3367c9afec47~noop.image" height="60" width="60"  alt="今日头条" style="margin: 10px"/>-->
<!--<img src="https://sf1-ttcdn-tos.pstatp.com/img/ee-finolhu/6f2b3dc9b3e945a89565dd67a3e1a3b3~noop.image" height="60" width="60"  alt="火山小视频" style="margin: 10px"/>-->
<!--<img src="https://sf1-ttcdn-tos.pstatp.com/img/ee-finolhu/d9a7c17402164799becb3b62676e5f88~noop.image" height="60" width="60"  alt="Lark" style="margin: 10px"/><br/>-->
<!--<img src="wiki/icons/duoshan.png" height="60" width="120"  alt="多闪" style="margin: 10px"/>-->
<!--<img src="https://sf1-ttcdn-tos.pstatp.com/img/ee-finolhu/2b49de98334a4c05b875a7d56df9abab~noop.image" height="60" width="60"  alt="FaceU" style="margin: 10px"/>-->
<!--<img src="https://sf3-ttcdn-tos.pstatp.com/img/ee-finolhu/5f2b63d1fc904c47a37c89dd439e2b7a~noop.image" height="60" width="60"  alt="轻颜" style="margin: 10px"/>-->
<!--<img src="wiki/icons/feiliao.png" height="45" width="122"  alt="飞聊" style="margin:17px"/>-->

<img src="wiki/KnownUsers.png" height="171" width="503"  alt="飞聊" style="margin:10px"/>

## Contribution

If you have some creative ideas and demands related to bytecode, come to join us to develop a new bytecode plugin based on ByteX!

Please read through our [Developer API](wiki/ByteX-Developer-API-en.md).



## License

Apache 2.0


