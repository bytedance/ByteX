[English](README.md) | 简体中文

# getter-setter-inline-plugin

## 功能

内联Getter和Setter方法，减少apk方法数和包大小。 

在日常的开发中，我们常常会为一些类手写一些getter或setter方法，如下代码所示。

```java
public class People {
    private int age;
    public int getAge() {
        return this.age;
    }
    public void setAge(int age) {
        this.age = age;
    }
}
```

通过这样的Getter和Setter方法，让类变量被外部读写变得可控。当变量被 `private` 修饰符隐藏并且只能通过 Getter 和 Setter 访问时，它就被“封装”起来了。封装是面向对象编程(OOP)的基本特性之一，使用Getter和Setter 方法是在程序代码中强制执行封装的方法之一。

但这些方法的数量其实在整包里还是相当可观的，在一般的大型App里一般会有7k~9k个。



## 使用

```groovy
classpath "com.bytedance.android.byteX:getter-setter-inline-plugin:${plugin_version}"
```



```groovy
apply plugin: 'bytex.getter_setter_inline'
getter_setter_inline {
    enable true
    enableInDebug false
    logLevel "DEBUG"
    shouldInline = [
        "com/ss/android/ugc/bytex/example/getter_setter/"
    ]
}
```



## 你可能会问？

### 为什么不用proguard？

Q：据我所知，如果开启了proguard的短方法内联也可以达到一样的优化效果，为什么不直接用proguard来做呢？ 

A：这是个好问题。 如果我们在配置文件里为proguard添加以下这个配置，允许它在内联短方法的时候能修改类成员的方法权限，proguard也就能为我们内联getters setters方法，那么也就没必要用这个插件了。

```
-allowaccessmodification
```



但是，因为国内大多数大型app都接入了hotfix，而proguard的短方法内联会导致线上被内联的方法不能被hotfix，所以我们一般会把proguard的短方法内联禁用。

但是像Getter和Setter方法指令极少，被hotfix的概率极低。

然而，开发者并不能控制哪些方法能内联，哪些方法不要内联。

所以getter-setter-inline-plugin正是为此而生，只专注于为你的App在打包过程中自动内联getters setters方法。



### 如何配置某些方法不被插件内联？

Q：如果Getter和Setter需要被运行时反射或者被native代码调用，如何配置这些方法不被内联？

A：该插件在打包时会自动收集所有的Proguard文件，解析所有`-keep`的配置项，用于排除某些class和method的内联。

因此，你只需要把你的方法在Proguard配置文件里配好就OK啦。
