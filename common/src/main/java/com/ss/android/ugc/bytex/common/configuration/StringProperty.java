package com.ss.android.ugc.bytex.common.configuration;

import javax.annotation.Nonnull;

/**
 * Created by tanlehua on 2019/4/23.
 */
public enum StringProperty implements Property<String> {
    EXCEPTION_IGNORE_LIST("bytex.exceptionIgnoreClassList", ""),
    GLOBAL_IGNORE_LIST("bytex.globalIgnoreClassList", "");

    @Nonnull
    private final String propertyName;
    private final String defaultValue;

    StringProperty(String propertyName, String defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String value() {
        return ProjectOptions.INSTANCE.getValue(this);
    }

    @Override
    public String parse(Object value) {
        if (value instanceof CharSequence || value instanceof Number) {
            return value.toString();
        }
        throw new IllegalArgumentException(
                "Cannot parse project property "
                        + this.getPropertyName()
                        + "='"
                        + value
                        + "' of type '"
                        + value.getClass()
                        + "' as string.");
    }
}

