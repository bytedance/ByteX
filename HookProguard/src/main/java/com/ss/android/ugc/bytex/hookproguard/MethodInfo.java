package com.ss.android.ugc.bytex.hookproguard;

import java.util.ArrayList;
import java.util.List;

public class MethodInfo {
    private ClassInfo classInfo;
    private int access;
    private String name;
    private String desc;
    private List<String> annotations;

    public MethodInfo(ClassInfo classInfo, int access, String name, String desc) {
        this.classInfo = classInfo;
        this.access = access;
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public int getAccess() {
        return access;
    }

    public void addAnnotation(String annotation) {
        if (annotations == null) {
            annotations = new ArrayList<>();
        }
        annotations.add(annotation);
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
