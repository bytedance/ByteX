package com.ss.android.ugc.bytex.hookproguard;

import java.util.ArrayList;
import java.util.List;

public class ClassInfo {
    protected int access;
    protected String name;
    protected String superName;
    protected String[] interfaces;
    private List<String> annotations;

    public ClassInfo(int access, String name, String superName, String[] interfaces) {
        this.access = access;
        this.name = name;
        this.superName = superName;
        this.interfaces = interfaces;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(String annotation) {
        if (annotations == null) {
            annotations = new ArrayList<>();
        }
        annotations.add(annotation);
    }

    public String getName() {
        return name;
    }

    public String getSuperName() {
        return superName;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public int getAccess() {
        return access;
    }
}
