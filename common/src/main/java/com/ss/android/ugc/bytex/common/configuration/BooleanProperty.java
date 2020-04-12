package com.ss.android.ugc.bytex.common.configuration;

import com.android.annotations.NonNull;

/**
 * Created by tanlehua on 2019/4/23.
 */
public enum BooleanProperty implements Property<Boolean> {
    ENABLE_DUPLICATE_CLASS_CHECK("bytex.enableDuplicateClassCheck", true),
    ENABLE_HTML_LOG("bytex.enableHtmlLog", true),
    ENABLE_RAM_CACHE("bytex.enableRAMCache", true),
    ENABLE_ASYNC_CACHE("bytex.asyncSaveCache", true),
    ENABLE_VERIFY_PROGUARD_CONFIGURATION_CHANGED("bytex.verifyProguardConfigurationChanged", true);

    @NonNull
    private final String propertyName;
    private final boolean defaultValue;

    BooleanProperty(String propertyName, boolean defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Boolean getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Boolean value() {
        return ProjectOptions.INSTANCE.getValue(this);
    }


    @Override
    public Boolean parse(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof CharSequence) {
            return Boolean.parseBoolean(value.toString());
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        throw new IllegalArgumentException(
                "Cannot parse project property "
                        + this.getPropertyName()
                        + "='"
                        + value
                        + "' of type '"
                        + value.getClass()
                        + "' as boolean.");
    }

}
