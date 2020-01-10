[English](README.md) | **[简体中文](README-zh.md)**

## Feature

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Delete SourceFile and LineNumber attributes of the class in the project during compilation
## Quick Start

* add build classpath

  >classpath "com.bytedance.android.byteX:SourceFileKiller:${plugin_version}"
* apply and configure the plugin in your build.gradle(application)

    ```groovy
    apply plugin: 'bytex.sourcefile'
    SourceFile {
        enable true//enable state
        enableInDebug true//whether enable in debug build
        deleteSourceFile true//whether to delete SourceFile info
        deleteLineNumber true//whether to delete LineNumber info
    }
    ```