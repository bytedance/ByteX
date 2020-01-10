package com.ss.android.ugc.bytex.shrinkR.res_check;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Resource implements Substance {
    @Expose
    private String type;
    @Expose
    private String name;
    private int resId;
    @Expose
    private List<String> rClass = new ArrayList<>(1);
    @Expose
    private List<String> attributions = new ArrayList<>(1);
    private AtomicInteger reference = new AtomicInteger(0);
    private final List<Resource> referringResources = new LinkedList<>();

    public Resource(String type, String name, int resId) {
        this.type = type;
        this.name = name;
        this.resId = resId;
    }

    public void refer() {
        reference.getAndAdd(1);
    }

    public int decreaseReference() {
        return reference.decrementAndGet();
    }

    public void reach(Resource resource) {
        referringResources.add(resource);
    }

    public List<Resource> getReferringResources() {
        return Collections.unmodifiableList(referringResources);
    }

    public boolean canReach() {
        return reference.get() > 0;
    }

    public String getName() {
        return name;
    }

    public int getResId() {
        return resId;
    }

    public List<String> getrClass() {
        return rClass;
    }

    public void addRClass(String rClass) {
        this.rClass.add(rClass);
    }

    public String getType() {
        return type;
    }

    public List<String> getAttributions() {
        return attributions;
    }

    public void define(String path) {
        attributions.add(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(type, resource.type) &&
                Objects.equals(name, resource.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    @Override
    public String toString() {
        return String.format("Unused resource name is [%s], type is [%s], from aar %s\n", name, type, attributions.toString());
    }
}
