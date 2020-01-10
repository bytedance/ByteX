English | **[简体中文](README-zh.md)**

# access-inline-plugin

## Feature

An Android gradle plugin designed for killing synthetic 'access$' methods in bytecode, for reducing apk methods and size.

## What is it used for?

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

The JVM thinks it is illegal to access `Foo`'s private members from `Foo$Inner` directly because `Foo` and `Foo$Inner`are different classes, even though the Java language standard allows an inner class to access an outer class' private members. To bridge the gap, the compiler generates a couple of synthetic methods:

```java
/*package*/ static int Foo.access$100(Foo foo) {
    return foo.mValue;
}
/*package*/ static void Foo.access$200(Foo foo, int value) {
    foo.doStuff(value);
}
```

The inner class code calls these static methods whenever it needs to access the `mValue` field or invoke the `doStuff()`method in the outer class.

Actually, the compiler-generated syntactic sugar would substantially increase the method count. This gradle plugin is used for killng those methods to decrease the method count of release apk.



## How to use it?

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

### Why not proguard?

Q: As far as I know, if the short method inlining of proguard is enabled, the same optimization effect can be achieved, why not just use proguard directly?

A: That's a good question. If we add the following line to Proguard configuration file, the optimizer is allowed  to modify the access of class members when inlining short methods. As a result,  Proguard can also inline the access methods for us, then there is no need to use this plugin .

```
-allowaccessmodification
```



However, because most large apps in China have the ability to apply hotfix patches based on loading Java classes without releasing a new version in Play Store, short methods inlined by Proguard cannot be hotfixed. In fact, it is a common practive to disable the short method inlining of proguard.

But the access methods are compiler-generated methods. Even if these methods are inlined, they will not affect hotfixes (because these methods are invisible to normal developers and the probability of being hotfixed is extremely low).  So we only want to inline those methods.

Unfortunately, developers cannot tell Proguard which method should be inlined and which method should not.

So the access-inline-plugin is born for this purpose, only focusing on automatically inlining the access methods for your App during the apk packaging process.