English | **[简体中文](README_zh.md)**

# ByteX（Infinite Possibilities）



<h1 align="center">
  <img src="wiki/bytex-logo.png" height="111" width="340"  alt="logo" />
 </h1>

> Powered by ByteDance Douyin Android team.


ByteX is a bytecode plugin platform based on Android Gradle Transform API and ASM. 

 (Maybe you can think of it as a socket with unlimited plugs?)


ByteX plugin family consists of several bytecode plugins. Each of them can not only run separately, but also be automatically integrated into the ByteX host,  along with other plugins as a single Transform in the apk building process.

What's more, each plugin is independent from one another, as well as their ByteX host. This architecture makes ByteX flexible, extensible and highly efficient for new plugins development.

### Background

If a project applies several separate bytecode plugins with 10s build time for each, the build time will get a linear increase.

However, if the iterative development in only one plugin module will make it more and more cluttered, for the code is deeply coupled. 

So an idea was raised. It could make sense to build a bytecode plugin platform, and the new feature can be extended based on it as a new plugin.

### Feature

- Code sharing. The common code sinks to `common` module and is shared by all plugins, so that developers could take more focus on bytecode operations.

- Plugin code is isolated and decoupled from each other. Long ago in ByteDance, all of the features related to bytecode operations were bundled together in a single plugin. As time goes by, the code went incomprehensible and could not be maintained anymore. However, based on ByteX, each feature is a standalone plugin, which makes it conducive to developing a new plugin.

- Platformization makes `Transform` more efficient.

  - Class files are processed in parallel, making full use of CPU resources of your computer.

  - Plugins are automatically and seamlessly integrated into one single `Transform` to improve the efficiency of processing. During the `Transform` process, the IO of the class file is time-consuming. Integrating all the plugins into a single transform can avoid linear increase of build time. It makes "1 + 1 = 2"  become "1 + 1 < 2" or approximately equal to 1.

- Plugin portability is outstanding. Each plugin can be separated from the ByteX host, working as a standalone transform.

### Structure

![structure](wiki/structure.png)



**TransformEngine**

Reading all the class files from both the project sources and Android SDK, as well as writing back to a specified directory.

**base-plugin**

ByteX host.

**common**

Basic code library, including class diagram construction, logs, interfaces provided to various plugins.

**other plugin modules**

Depending on `common` module and focusing on bytecode operations.



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

Note: If ByteX host is not applied, there is no difference between ByteX plugins and ordinary ones, and all of them will run separately. On the contrary, all ByteX plugins can be automatically merged into one single Transform with the help of the ByteX host.

The following are the plugins based on ByteX. You can read their own README.md to get more information.

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

If you have some creative ideas and demands related to bytecode, come to join us to and it is appreciated to expand ByteX plugin family!

Please refer to our [Developer API](wiki/ByteX-Developer-API-en.md) to get more information.



## License

Apache 2.0


