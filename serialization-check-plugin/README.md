English | **[简体中文](README-zh.md)**

# serialization-check-plugin

## Feature

If a class implements the `java.io.Serializable` interface and one of it's fields does not implement the  `java.io.Serializable`  interface, it will crash when deserializing the this class.

```java
public class Foo implements Serializable {
    private Data data;
}

public class Data {
    
}
```

This plugin is used to detect such improper code. 👆

## Usage

```groovy
classpath "com.bytedance.android.byteX:serialization-check-plugin:${plugin_version}"
```

```groovy
apply plugin: 'bytex.serialization_check'
SerializationCheck {
    enable true
    // don't detact those class in whitelist.
    // (class's full name#(field's name)
    whiteList = [
            "com/ss/android/ugc/aweme/profile/model/User#userHonor"
    ]
    // Only check the classes under those package
    onlyCheck = [
            "com/ss/android/ugc/"
    ]
}
```
