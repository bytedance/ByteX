package com.ss.android.ugc.bytex.common.builder;

import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInvocation;
import com.ss.android.ugc.bytex.common.IPlugin;
import com.ss.android.ugc.bytex.common.flow.TransformFlowListener;

import org.gradle.api.Project;

import javax.annotation.Nullable;

/**
 * Created by yangzhiqian on 2020/8/26<br/>
 * Desc:
 */
public interface ByteXBuildListener extends TransformFlowListener {
    void onByteXPluginApply(Project project, IPlugin plugin);

    void onByteXPluginApplied(IPlugin plugin);

    void onProjectBuildStart(Project project);

    void onByteXPluginTransformStart(Transform transform, TransformInvocation transformInvocation);

    void onByteXPluginStart(IPlugin plugin);

    void onByteXPluginFinished(IPlugin plugin);

    void onByteXPluginTransformFinished(Transform transform, @Nullable Exception exception);

    void onProjectBuildFinished(Project project, @Nullable Throwable throwable);
}
