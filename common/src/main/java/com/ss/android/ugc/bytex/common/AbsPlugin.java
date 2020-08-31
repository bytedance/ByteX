package com.ss.android.ugc.bytex.common;

import com.android.build.api.transform.Transform;
import com.android.build.gradle.AppExtension;
import com.google.common.reflect.TypeToken;
import com.ss.android.ugc.bytex.common.builder.internal.GlobalByteXBuildListener;
import com.ss.android.ugc.bytex.common.configuration.BooleanProperty;
import com.ss.android.ugc.bytex.common.configuration.ProjectOptions;
import com.ss.android.ugc.bytex.common.exception.GlobalWhiteListManager;
import com.ss.android.ugc.bytex.common.hook.TransformHook;
import com.ss.android.ugc.bytex.transformer.TransformContext;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.invocation.DefaultGradle;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public abstract class AbsPlugin<E extends BaseExtension> implements Plugin<Project>, IPlugin {
    protected Project project;
    protected AppExtension android;
    protected E extension;
    private boolean isRunningAlone = false;

    protected Transform getTransform() {
        return new SimpleTransform<>(new BaseContext<>(project, android, extension), this);
    }

    @Override
    public String name() {
        return extension == null ? getClass().getSimpleName() : extension.getName();
    }

    @Override
    public BaseExtension getExtension() {
        return extension;
    }

    @Override
    public boolean enable(TransformContext transformContext) {
        return extension.isEnable() && (extension.isEnableInDebug() || transformContext.isReleaseBuild());
    }

    @Override
    public boolean alone() {
        boolean alone = false;
        Object aloneProperty = project.findProperty("bytex." + extension.getName() + ".alone");
        if (aloneProperty != null) {
            alone = Boolean.parseBoolean(aloneProperty.toString());
        }
        if (!alone && !transformConfiguration().isIncremental() && BooleanProperty.ENABLE_SEPARATE_PROCESSING_NOTINCREMENTAL.value()) {
            alone = true;
        }
        return alone;
    }

    @Override
    public boolean isRunningAlone() {
        return isRunningAlone;
    }

    @Override
    public final void apply(@NotNull Project project) {
        GlobalByteXBuildListener.INSTANCE.onByteXPluginApply(project, this);
        this.project = project;
        this.android = project.getExtensions().getByType(AppExtension.class);
        ProjectOptions.INSTANCE.init(project);
        GlobalWhiteListManager.INSTANCE.init(project);
        Class<E> extensionClass = getExtensionClass();
        if (extensionClass != null) {
            Instantiator instantiator = ((DefaultGradle) project.getGradle()).getServices().get(Instantiator.class);
            extension = createExtension(instantiator, extensionClass);
            project.getExtensions().add(extension.getName(), extension);
        }
        onApply(project);
        String hookTransformName = hookTransformName();
        if (hookTransformName != null) {
            TransformHook.inject(project, android, this);
        } else {
            if (!alone()) {
                try {
                    ByteXExtension byteX = project.getExtensions().getByType(ByteXExtension.class);
                    byteX.registerPlugin(this);
                    isRunningAlone = false;
                } catch (UnknownDomainObjectException e) {
                    android.registerTransform(getTransform());
                    isRunningAlone = true;
                }
            } else {
                android.registerTransform(getTransform());
                isRunningAlone = true;
            }
        }
        GlobalByteXBuildListener.INSTANCE.onByteXPluginApplied(this);
    }


    /**
     * provide a class which extends BaseExtension for plugin registering
     *
     * @return a BaseExtension class.
     */
    @SuppressWarnings("unchecked")
    protected Class<E> getExtensionClass() {
        return (Class<E>) new TypeToken<E>(getClass()) {
        }.getRawType();
    }

    protected E createExtension(Instantiator instantiator, Class<E> clazz) {
        return instantiator.newInstance(clazz);
    }

    protected void onApply(@Nonnull Project project) {
    }


    @Override
    public void startExecute(TransformContext transformContext) {
        GlobalByteXBuildListener.INSTANCE.onByteXPluginStart(this);
    }

    @Override
    public void afterExecute() throws Throwable {
        GlobalByteXBuildListener.INSTANCE.onByteXPluginFinished(this);
    }
}
