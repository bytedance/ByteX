package com.ss.android.ugc.bytex.common.flow.main;

import com.ss.android.ugc.bytex.transformer.processor.FileProcessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by tanlehua on 2019/4/28.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Processors.class)
public @interface Processor {
    Process process() default Process.TRANSFORM;

    Class<? extends FileProcessor> implement();
}
