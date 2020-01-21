[English](README.md) | 简体中文

# refer-check-plugin

## 功能

检查所有字节码指令，看是否存在以下非法引用：

- 调用了不存在的方法；
- 所调用的方法没有访问权限（比如，调用了其它类的private方法，或在static方法内调用非static方法）；
- 访问了不存在的字段；
- 所访问的字段没有访问权限（比如，访问了其它类的private字段，或在static方法内调用非static字段）；



## 使用

```groovy
classpath "com.bytedance.android.byteX:refer-check-plugin:${plugin_version}"
```

```groovy
apply plugin: 'bytex.refer_check'
refer_check {
    enable true
    enableInDebug false
    logLevel "INFO"
    strictMode true // 控制是否在发现不存在方法时中止构建
    // check白名单。类名和方法名要用#号分隔开，都支持正则匹配。
    whiteList = [
            "com/google/+", // 跳过com/google为包名前缀的类
           // 跳过com/tellh/Foo这个类里方法名为init的检查
            "com/tellh/Foo#init",
           // 跳过com/tellh/Foo里的内部类Abc，方法名为init的检查，注意$符号要转义
            "com/tellh/Foo\$Abc#init",
    ]
}
```



## 错误日志

举个例子，一般报错日志会类似于以下这种：

![log](raw/log.png)

1. 首先，把这些日志全部copy，粘贴到AS的Analyze Stack Trace

![](raw/Analyze Stack Trace.png)

![](raw/Analyze Stack Trace2.png)

上面👆这个日志是什么意思呢？ 它说，在com.ss.ugc.android.plugin.testlib2.B这个类里，有个名为run的方法，在12行（我们点击蓝色的链接就可以定位到那一行），调用了com/ss/ugc/android/testlib/A这个类的a方法，这个方法没有入参，没有返回值。但是com/ss/ugc/android/testlib/A这个类的a方法并不存在。

![](raw/B#run().png)

这时候，需要看一下com/ss/ugc/android/testlib/A这个类在哪个库里面。

![](raw/A_class.png)

com/ss/ugc/android/testlib/A在com.tellh:testlib这个库里。

方法不存在，有两种排查思路，

- 第一种是com.tellh:testlib这个库没有include到构建流程里，比如这个库是以compileOnly的方式依赖，或者是被外面exclude了。 这种case，一般在报错的日志里会藏有这样的提示：

```
Tips: class [com/ss/ugc/android/testlib/A] was not packaged, please checkout if it was 'compileOnly' or excluded by some dependencies.
```

- 第二种是com.tellh:testlib这个库被多个地方依赖，并且被覆盖成其它版本了，在那个版本里，com/ss/ugc/android/testlib/A这个类的a方法（无返回值无入参）是不存在的。

如果确定是第二种，接下来需要review一下gradle的dependency，在命令行执行：

```
./gradlew app:dependencies --configuration ${variantName}RuntimeClasspath > dep.txt
```

如果没有flavor的话，执行：

```
./gradlew app:dependencies --configuration releaseRuntimeClasspath > dep.txt
```

依赖树会被输出到dep.txt里，搜索com.tellh:testlib这个库，看看这个库被哪些地方依赖了。

![](raw/dep.png)

我们看一下com.tellh:testlib:2.0，看看里面的com/ss/ugc/android/testlib/A是否没有a方法（无参无返回值）。

![](raw/A_class2.png)

真相大白了，原来在com.tellh:testlib:2.0里，com/ss/ugc/android/testlib/A这个类里的a方法被增加了一个int型的入参。

