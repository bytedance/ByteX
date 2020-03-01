package com.ss.android.ugc.bytex.common;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class ByteXExtension extends BaseExtension {

    private final List<IPlugin> plugins = new ArrayList<>();

    public void registerPlugin(IPlugin plugin) {
        plugins.add(plugin);
    }

    public List<IPlugin> getPlugins() {
        return ImmutableList.copyOf(plugins);
    }

    public void clearPlugins() {
        plugins.clear();
    }

    @Override
    public String getName() {
        return "byteX";
    }
}
