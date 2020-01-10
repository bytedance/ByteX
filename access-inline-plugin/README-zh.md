[English](README.md) | 简体中文

# access-inline-plugin

## 功能

内联access$方法，减少apk方法数和包大小。

我们来看看下面这种case：

``` java
public class Foo {
    private int mValue;

    private void doStuff(int value) {
        System.out.println("Value is " + value);
    }

    private class Inner {
        void stuff() {
            Foo.this.doStuff(Foo.this.mValue);
        }
    }
}
```


JVM认为从内部类`Foo$Inner`直接访问外部类`Foo`的私有方法是非法的，因为`Foo`和`Foo$Inner` 是两个不同的类，尽管Java语法里允许内部类直接访问外部类的私有成员。编译器为了能实现这种语法，会在编译期生成以下方法：

``` java
/*package*/ static int Foo.access$100(Foo foo) {
    return foo.mValue;
}
/*package*/ static void Foo.access$200(Foo foo, int value) {
    foo.doStuff(value);
}
```

当内部类需要访问外部类的`mValue` 或调用`doStuff()`方法时，会借助这些静态方法来间接实现。

然而，编译器的这种语法糖对方法数的增加还是很可观的。而这个gradle插件会内联这些方法来减少release版本apk的方法数。



## 使用

```groovy
classpath "com.bytedance.android.byteX:access-inline-plugin:${plugin_version}"
```



```groovy
apply plugin: 'bytex.access_inline'
access_inline {
    enable true
    enableInDebug false
    logLevel "DEBUG"
}
```



## 你可能会问？

### 为什么不用proguard？

Q：据我所知，如果开启了proguard的短方法内联也可以达到一样的优化效果，为什么不直接用proguard来做呢？ 

A：这是个好问题。 如果我们在配置文件里为proguard添加以下这个配置，允许它在内联短方法的时候能修改类成员的方法权限，proguard也就能为我们内联access方法，那么也就没必要用这个插件了。

```
-allowaccessmodification
```



但是，因为国内大多数大型app都接入了hotfix，而proguard的短方法内联会导致线上被内联的方法不能被hotfix，所以我们一般会把proguard的短方法内联禁用。

但是像access方法是编译器生成的方法，就算内联了这些方法也不会影响hotfix（因为这些方法是对开发者透明的，被hotfix的概率极低）。

然而，开发者并不能控制哪些方法能内联，哪些方法不要内联。

所以access-inline-plugin正是为此而生，只专注于为你的App在打包过程中自动内联access方法。
