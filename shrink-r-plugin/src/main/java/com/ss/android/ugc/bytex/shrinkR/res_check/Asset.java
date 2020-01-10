package com.ss.android.ugc.bytex.shrinkR.res_check;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Asset implements Substance {
    @Expose
    private String path;
    @Expose
    private String name;
    private AtomicInteger reference = new AtomicInteger(0);
    @Expose
    private List<String> attributions = new ArrayList<>(1);

    public Asset(String path, String name) {
        this.name = name;
        this.path = path;
    }

    @Override
    public String toString() {
        return String.format("Unused asset name is [%s], at path [%s], from aar [%s] \n", name, path, attributions.toString());
    }

    @Override
    public String getName() {
        return name;
    }

    public void refer() {
        reference.getAndAdd(1);
    }

    @Override
    public void define(String path) {
        attributions.add(path);


    }

    public boolean canReach() {
        return reference.get() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return Objects.equals(name, asset.name) &&
                Objects.equals(path, asset.path);
    }

    public List<String> getAttributions() {
        return attributions;
    }

    public String getPath() {
        return path;
    }
}
