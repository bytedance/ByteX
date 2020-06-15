package com.ss.android.ugc.bytex.common;

import com.android.build.api.transform.Transform;
import com.android.build.gradle.AppExtension;
import com.google.common.reflect.TypeToken;
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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public abstract class AbsPlugin<E extends BaseExtension> implements Plugin<Project>, IPlugin {
    protected Project project;
    protected AppExtension android;
    protected E extension;

    protected Transform getTransform() {
        return new SimpleTransform<>(new BaseContext<>(project, android, extension), this);
    }

    @Override
    public boolean enable(TransformContext transformContext) {
        return extension.isEnable() && (extension.isEnableInDebug() || transformContext.isReleaseBuild());
    }

    @Override
    public boolean alone() {
        return false;
    }

    @Override
    public boolean shouldSaveCache() {
        return extension.isShouldSaveCache();
    }

    @Override
    public final void apply(@NotNull Project project) {
        if(!transformConfiguration().isIncremental()){
            System.err.println("[ByteX Warning]:"+this.getClass().getName()+" does not yet support incremental build");
        }
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
                } catch (UnknownDomainObjectException e) {
                    android.registerTransform(getTransform());
                }
            } else {
                android.registerTransform(getTransform());
            }
        }
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
    public void afterExecute() throws Throwable {
        project = null;
        android = null;
        extension = null;
    }
}
