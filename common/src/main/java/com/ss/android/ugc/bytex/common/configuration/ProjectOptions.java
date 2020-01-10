package com.ss.android.ugc.bytex.common.configuration;

import com.android.annotations.NonNull;
import com.google.common.collect.ImmutableMap;

import org.gradle.api.Project;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by tanlehua on 2019/4/23.
 */
public enum ProjectOptions {
    INSTANCE;
    private ImmutableMap<BooleanProperty, Boolean> booleanOptions;
    private ImmutableMap<StringProperty, String> stringOptions;

    public void init(@NonNull Project project) {
        if (booleanOptions != null || stringOptions != null) return;
        Map<String, Object> properties = project.getExtensions().getExtraProperties().getProperties();
        booleanOptions = readOptions(BooleanProperty.values(), properties);
        stringOptions = readOptions(StringProperty.values(), properties);
        project.getGradle().buildFinished(p -> {
            booleanOptions = null;
            stringOptions = null;
        });
    }

    @NonNull
    private static <OptionT extends Property<ValueT>, ValueT>
    ImmutableMap<OptionT, ValueT> readOptions(
            @NonNull OptionT[] values,
            @NonNull Map<String, ?> properties) {
        Map<String, OptionT> optionLookup =
                Arrays.stream(values).collect(Collectors.toMap(Property::getPropertyName, v -> v));
        ImmutableMap.Builder<OptionT, ValueT> valuesBuilder = ImmutableMap.builder();
        for (Map.Entry<String, ?> property : properties.entrySet()) {
            OptionT option = optionLookup.get(property.getKey());
            if (option != null) {
                ValueT value = option.parse(property.getValue());
                valuesBuilder.put(option, value);
            }
        }
        return valuesBuilder.build();
    }

    boolean getValue(BooleanProperty option) {
        if (booleanOptions == null) return option.getDefaultValue();
        return booleanOptions.getOrDefault(option, option.getDefaultValue());
    }

    String getValue(StringProperty option) {
        if (stringOptions == null) return option.getDefaultValue();
        return stringOptions.getOrDefault(option, option.getDefaultValue());
    }
}
