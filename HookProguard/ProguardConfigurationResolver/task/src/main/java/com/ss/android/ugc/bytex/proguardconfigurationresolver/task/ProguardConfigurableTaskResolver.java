package com.ss.android.ugc.bytex.proguardconfigurationresolver.task;

import com.android.build.gradle.internal.tasks.ProguardConfigurableTask;
import com.android.build.gradle.internal.tasks.ProguardTask;
import com.android.build.gradle.internal.tasks.R8Task;
import com.ss.android.ugc.bytex.proguardconfigurationresolver.ProguardConfigurationResolver;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

/**
 * Created by yangzhiqian on 2020/7/6<br/>
 */
public class ProguardConfigurableTaskResolver extends ProguardConfigurationResolver {

    public ProguardConfigurableTaskResolver(Project project, String variantName) {
        super(project, variantName);
    }

    @Override
    public Task getTask() {
        for (Task task : project.getTasks()) {
            if ((task instanceof ProguardTask || task instanceof R8Task) &&
                    ((ProguardConfigurableTask) task).getVariantName().equals(variantName)) {
                return task;
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
        return ((ProguardConfigurableTask) getTask()).getConfigurationFiles();
    }
}
