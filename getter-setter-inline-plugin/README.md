English | **[简体中文](README-zh.md)**

# getter-setter-inline-plugin

## Feature

An Android gradle plugin to inline Getters and Setters, for reducing apk methods and the apk size.

## What is it used for

In daily development, we often write some Getters or Setters methods for some classes, as shown in the following code.

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

Through such Getters and Setters, class fields can be controlled while external reading and writing. A field is "encapsulated" with a `private` modifier and is only accessible through Getters and Setters. Encapsulation is one of the basic characteristics of object-oriented programming (OOP), and using Getter and Setter methods is one of the aproaches to achieve encapsulation in program code.



## How to use it

```groovy
classpath "com.bytedance.android.byteX:getter_setter-inline-plugin:${plugin_version}"
```



```groovy
apply plugin: 'bytex.getter_setter_inline'
getter_setter_inline {
    enable true
    enableInDebug false
    logLevel "DEBUG"
    shouldInline = [
            "com/ss/android/ugc/aweme/challenge/model/",
            "com/ss/android/ugc/aweme/comment/model/",
    ]
}
```



## You might want to ask?

### Why not just use proguard?

Q: As far as I know, if the short method inlining of proguard is enabled, the same optimization effect can be achieved, why not just use proguard directly?

A: That's a good question. If we add the following configuration to Proguard in the configuration file, Proguard is allowed  to modify the access of class members when inlining short methods. As a result,  Proguard can also inline the Getters and Setters for us, then there is no need to use this plugin .

```
-allowaccessmodification
```



However, because most large apps in China have ability to hotfix the bad methods without updating the app. And short methods inlined by Proguard cannot be hotfixed, so we usually disable the short method inline of proguard.

But Getters and Setters have very few instructions, and the probability of being hotfixed is extremely low. So we only want to inline those methods.

Unfortunately, developers cannot control Proguard which methods can be inlined and which methods should not be inlined either.

So the getter-setter-inline-plugin is born for this purpose, only focusing on automatically inlining the Getters and Setters for your App during the apk packaging process.