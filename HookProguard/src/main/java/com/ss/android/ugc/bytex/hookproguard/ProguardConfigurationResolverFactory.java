package com.ss.android.ugc.bytex.hookproguard;

import com.android.builder.model.Version;
import com.android.repository.Revision;
import com.ss.android.ugc.bytex.proguardconfigurationresolver.ProguardConfigurationResolver;
import com.ss.android.ugc.bytex.proguardconfigurationresolver.task.ProguardConfigurableTaskResolver;
import com.ss.android.ugc.bytex.proguardconfigurationresolver.transform.ProguardConfigurableTransformResolver;

import org.gradle.api.Project;

/**
 * Created by yangzhiqian on 2020/7/27<br/>
 */
class ProguardConfigurationResolverFactory {
    public static ProguardConfigurationResolver createProguardConfigurationResolver(Project project, String variantName) {
        Revision revision = Revision.parseRevision(Version.ANDROID_GRADLE_PLUGIN_VERSION);
        if (revision.getMajor() >= 3 && revision.getMinor() >= 6) {
            return new ProguardConfigurableTaskResolver(project, variantName);
        } else {
            return new ProguardConfigurableTransformResolver(project, variantName);
        }
    }
}
