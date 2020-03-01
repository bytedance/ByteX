English | [简体中文](README-zh.md)

## Purpose
&emsp;&emsp;Able to detect exceptions caused by cross-module use of ButterKnife during compilation<br/>

## Problem
&emsp;&emsp;When using ButterKnife across modules, if the following conditions are satisfied<br/>
1. Class A is injected by ButterKnife
2. Class B that is parent of Class A is also injected by ButterKnife
3. A and B are in different modules

&emsp;&emsp;Under the condition that instance of A uses ButterKnife injection, the injection of the parent class B will be ignored, which may cause a null pointer exception or a potential business logic exception<br/>
&emsp;&emsp;This plugin hopes to check this situation during compilation. If an abnormal case is detected, the compilation process will be interrupted, and an error message will be output to propel the developer to fix it.<br/>

## TODO
&emsp;&emsp;Automatic repair for wrong cases with bytecode technology<br/>

## Quick Start

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