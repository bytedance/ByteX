English | **[简体中文](README-zh.md)**

## Feature

- Inline R files' constants and slimming the files size;
- Inspect unused resource;
- Inspect unused assets;

## Quick Start

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
            // keep android.support.constraint.R all id constant fields.
            "android.support.constraint.R.id", 
            // keep all drawable fields prefix with im_e.
            "R.drawable.im_e+",
        ]
        resCheck {
            enable true // switch of unused resource check.
            // Make fuzzy matching based on the file path where the unused resources are located (because some redundant resources in third-party libraries cannot be manually deleted)
            onlyCheck = [
                    // Only check unused resource in the project module.
                    "app/build"
            ]
            // whitelist
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
            enable true // switch of unused assets check.
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