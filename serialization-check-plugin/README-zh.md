[English](README.md) | 简体中文

# serialization-check-plugin

## 功能

一个类如果实现了 `java.io.Serializable` 接口，它的field里所引用的类没有实现 `java.io.Serializable` 接口，那么对这个类做反序列化时就会crash。

这个序列化插件就是用来检测出这样不当的使用场景。

```java
public class Foo implements Serializable {
    private Data data;
}

public class Data {
    
}
```



## 使用

```groovy
classpath "com.bytedance.android.byteX:serialization-check-plugin:${plugin_version}"
```

```groovy
apply plugin: 'bytex.serialization_check'
SerializationCheck {
    enable true
    // 白名单内不检查
    // 全类名#字段名
    whiteList = [
            "com/ss/android/ugc/aweme/profile/model/User#userHonor"
    ]
    // 对包名前缀做模糊匹配，只检查这些类
    onlyCheck = [
            "com/ss/android/ugc/"
    ]
}
```


