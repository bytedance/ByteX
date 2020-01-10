[English](README.md) | 简体中文

## 功能
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;编译期间删除项目中的自己码的SourceFile和行号属性信息
## 接入方式
* 添加插件classpath

  >classpath "com.bytedance.android.byteX:SourceFileKiller:${plugin_version}"
* 在application的build.gradle中apply插件并配置

	```groovy
	apply plugin: 'bytex.sourcefile'
	SourceFile {
    	enable true//整体开关
    	enableInDebug true//debug模式开关
    	deleteSourceFile true//是否删除文件的SourceFile信息
    	deleteLineNumber true//是否删除行号信息
	}
	```