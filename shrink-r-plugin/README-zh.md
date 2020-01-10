[English](README.md) | 简体中文

## 功能

- R文件常量内联，R文件瘦身；
- 无用Resource资源检查；
- 无用assets检查。


## 接入方式

* add build classpath

  >classpath "com.bytedance.android.byteX:shrink-r-plugin:${plugin_version}"
* apply and configure the plugin in your build.gradle(application)

    ```groovy
    apply plugin: 'bytex.shrink_r_class'
    shrinkR {
        enable true
        enableInDebug false
        logLevel "DEBUG"
        keepList = [
            // keep android.support.constraint.R里所有id
            "android.support.constraint.R.id", 
            // keep 所有以im_e为前缀的drawable字段
            "R.drawable.im_e+",
        ]
        resCheck {
            enable true // 无用资源检查的开关
            // 根据资源所在的路径做模糊匹配（因为第三方库用到的冗余资源没法手动删）
            onlyCheck = [
                    // 只检查主工程里的资源
                    "app/build"
            ]
            // 检查白名单。这些资源就算是冗余资源也不会report出来
            keepRes = [
                "R.drawable.ic_list_dou_order",
                "R.string.snapchat_tiktok_client_id",
                "R.string.snapchat_musically_client_id",
                "R.string.fb_account_kit_client_token",
                "R.string.mapbox_*",
                "R.string.kakao*",
                "R.dimen",
                "R.color",
                "R.animator",
                "R.integer",
                "R.bool",
                "R.style",
                "R.styleable",
                "R.attr",
                "R.xml",
                "R.array",
                "R.string"
            ]
        }
    
        assetsCheck {
            enable true // 冗余assets资源检查开关
            keepBySuffix = [
                ".model",
                ".otf",
                ".ttf"
            ]
            keepAssets = [
                "start_anim/",
                "Contour_2D/",
            ]
        }
    }
    ```