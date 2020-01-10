package com.ss.android.ugc.bytex.base;

import com.ss.android.ugc.bytex.common.CommonTransform;
import com.ss.android.ugc.bytex.common.IPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Created by tlh on 2018/8/29.
 */

public class ByteXTransform extends CommonTransform<Context> {
    private List<IPlugin> plugins;

    public ByteXTransform(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return "ByteX";
    }

    @Override
    public boolean shouldSaveCache() {
        return context.extension.isShouldSaveCache() && super.shouldSaveCache();
    }

    @Override
    protected List<IPlugin> getPlugins() {
        if (!context.isEnable()) {
            return Collections.emptyList();
        }
        if (plugins == null || plugins.isEmpty()) {
            plugins = context.extension.getPlugins();
        }
        return plugins;
    }
}
