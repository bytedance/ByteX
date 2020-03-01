[English](README.md) | 简体中文

## 目的
&emsp;&emsp;在编译期间能够检查出ButterKnife跨模块使用造成的异常情况<br/>

## 解决问题
&emsp;&emsp;在跨模块使用ButterKnife时，如果满足以下条件<br/>
1. 类A使用ButterKnife注入
2. A存在父类B也使用ButterKnife注入
3. A和B在不同的module中

&emsp;&emsp;那么A的实例使用ButterKnife注入时将会忽略父类B的注入，可能造成空指针异常或者潜在的业务逻辑异常<br/>
&emsp;&emsp;本插件希望在编译期间能够针对这种情况进行检查，若检查出异常case则中断编译流程，输出错误信息提示开发者修复。<br/>

## TODO
&emsp;&emsp;针对错误的case使用字节码进行自动修复<br/>

## 接入方式

* add build classpath

  >classpath "com.bytedance.android.byteX:butterknife-check-plugin:${plugin_version}"
* apply and configure the plugin in your build.gradle(application)

    ```groovy
    apply plugin: 'bytex.butterknife-check'
    butterknife-check-plugin {
        enable true
        enableInDebug true
        logLevel "DEBUG"
    }
    ```