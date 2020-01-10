package com.ss.android.ugc.bytex.field_assign_opt;

import com.android.build.gradle.AppExtension;
import com.ss.android.ugc.bytex.common.BaseContext;
import com.ss.android.ugc.bytex.common.utils.Utils;

import org.gradle.api.Project;

public class Context extends BaseContext<FieldAssignOptExtension> {
    Context(Project project, AppExtension android, FieldAssignOptExtension extension) {
        super(project, android, extension);
    }

    /**
     * @return true means inside the whitelist
     */
    public boolean inWhiteList(String owner, String name, String desc) {
        return extension.getWhiteList().contains(Utils.replaceSlash2Dot(owner) + "." + name);
    }
}
