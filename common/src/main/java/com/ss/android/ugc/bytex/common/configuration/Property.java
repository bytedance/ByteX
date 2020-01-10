package com.ss.android.ugc.bytex.common.configuration;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

/**
 * Created by tanlehua on 2019/4/23.
 */
public interface Property<T> {
    T value();

    @NonNull
    String getPropertyName();

    @Nullable
    T getDefaultValue();

    @NonNull
    T parse(@NonNull Object value);
}
