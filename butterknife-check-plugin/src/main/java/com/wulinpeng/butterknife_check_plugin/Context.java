package com.wulinpeng.butterknife_check_plugin;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.BaseContext;

import org.gradle.api.Project;

/**
 * author：wulinpeng
 * date：2020-02-18 11:51
 * desc:
 */
public class Context extends BaseContext<ButterKnifeCheckExtension> {
    public Context(Project project, AppExtension android,
                   ButterKnifeCheckExtension extension) {
        super(project, android, extension);
    }
}
