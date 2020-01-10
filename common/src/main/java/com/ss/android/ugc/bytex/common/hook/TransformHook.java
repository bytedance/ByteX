package com.ss.android.ugc.bytex.common.hook;

import com.android.build.api.transform.Transform;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.internal.pipeline.TransformTask;
import com.ss.android.ugc.bytex.common.IPlugin;
import com.ss.android.ugc.bytex.common.log.LevelLog;

import org.gradle.api.Project;
import org.gradle.api.Task;

import java.lang.reflect.Field;

public class TransformHook {
    public static void inject(Project project, AppExtension android, IPlugin plugin) {
        String transformName = plugin.hookTransformName();
        project.afterEvaluate(ignore -> {
            boolean hookSucceed = false;
            try {
                for (Task task : project.getTasks()) {
                    if (task instanceof TransformTask) {
                        Transform targetTransform = ((TransformTask) task).getTransform();
                        if (!transformName.equalsIgnoreCase(targetTransform.getName()) &&
                                !(transformName + "Hook").equalsIgnoreCase(targetTransform.getName())) {
                            continue;
                        }
                        ProxyTransform proxyTransform;
                        if (targetTransform instanceof ProxyTransform) {
                            proxyTransform = (ProxyTransform) targetTransform;
                        } else {
                            LevelLog.DEFAULT.i(String.format("Find %s handle. handle class: %s, task name: %s",
                                    transformName, targetTransform.getClass(), task.getName()));
                            Field field = TransformTask.class.getDeclaredField("transform");
                            field.setAccessible(true);
                            proxyTransform = new ProxyTransform(project, android, transformName, targetTransform);
                            field.set(task, proxyTransform);
                        }
                        proxyTransform.appendPlugin(plugin);
                        hookSucceed = true;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Can not hook transform:" + transformName, e);
            }
            if (!hookSucceed) {
                throw new RuntimeException("Can not hook transform:" + transformName);
            }
        });
    }
}
