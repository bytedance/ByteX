package com.ss.android.ugc.bytex.proguardconfigurationresolver.transform;

import com.android.build.gradle.internal.pipeline.TransformTask;
import com.android.build.gradle.internal.transforms.ProguardConfigurable;
import com.ss.android.ugc.bytex.proguardconfigurationresolver.ProguardConfigurationResolver;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.lang.reflect.Method;

/**
 * Created by yangzhiqian on 2020/7/6<br/>
 */
public class ProguardConfigurableTransformResolver extends ProguardConfigurationResolver {

    public ProguardConfigurableTransformResolver(Project project, String variantName) {
        super(project, variantName);
    }

    @Override
    public Task getTask() {
        for (Task task : project.getTasks()) {
            if (task instanceof TransformTask) {
                TransformTask transformTask = (TransformTask) task;
                String name = transformTask.getTransform().getClass().getName();
                if (transformTask.getVariantName().equals(variantName) &&
                        ("com.android.build.gradle.internal.transforms.ProGuardTransform".equals(name) ||
                                "com.android.build.gradle.internal.transforms.R8Transform".equals(name))) {
                    return task;
                }
            }
        }
        return null;
    }

    @Override
    public FileCollection getAllConfigurationFiles() {
        Task task = getTask();
        if (task == null) {
            return null;
        }
        ProguardConfigurable proguardConfigurable = (ProguardConfigurable) ((TransformTask) task).getTransform();
        try {
            Method method = ProguardConfigurable.class.getDeclaredMethod("getAllConfigurationFiles");
            method.setAccessible(true);
            return (FileCollection) method.invoke(proguardConfigurable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
