English | **[简体中文](README-zh.md)**

# access-inline-plugin

## feature

An Android gradle plugin to inline methods that start with the prefix 'access$' in bytecode, for reducing apk methods and the apk size.

## What is it used for

Consider the following class definition:

```java
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

The JVM considers direct access to `Foo`'s private members from `Foo$Inner` to be illegal because `Foo` and `Foo$Inner`are different classes, even though the Java language allows an inner class to access an outer class' private members. To bridge the gap, the compiler generates a couple of synthetic methods:

```java
/*package*/ static int Foo.access$100(Foo foo) {
    return foo.mValue;
}
/*package*/ static void Foo.access$200(Foo foo, int value) {
    foo.doStuff(value);
}
```

The inner class code calls these static methods whenever it needs to access the `mValue` field or invoke the `doStuff()`method in the outer class.

However, This compiler syntactic sugar would substantially increase the method count. And this gradle plugin is used for inlining those methods to decrease the method count of release apk.



## How to use it

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



## You might want to ask?

### Why not just use proguard?

Q: As far as I know, if the short method inlining of proguard is enabled, the same optimization effect can be achieved, why not just use proguard directly?

A: That's a good question. If we add the following configuration to Proguard in the configuration file, Proguard is allowed  to modify the access of class members when inlining short methods. As a result,  Proguard can also inline the access methods for us, then there is no need to use this plugin .

```
-allowaccessmodification
```



However, because most large apps in China have ability to hotfix the bad methods without updating the app. And short methods inlined by Proguard cannot be hotfixed, so we usually disable the short method inline of proguard.

But the access methods are compiler-generated methods. Even if these methods are inlined, they will not affect hotfixes (because these methods are invisible to normal developers and the probability of being hotfixed is extremely low).  So we only want to inline those methods.

Unfortunately, developers cannot control Proguard which methods can be inlined and which methods should not be inlined either.

So the access-inline-plugin is born for this purpose, only focusing on automatically inlining the access method for your App during the apk packaging process.