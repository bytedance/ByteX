package com.ss.android.ugc.bytex.pluginconfig.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by tanlehua on 2019-05-01.
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface PluginConfig {
    String value();
}
