package com.ss.android.ugc.bytex.base;

import com.ss.android.ugc.bytex.common.CommonTransform;
import com.ss.android.ugc.bytex.common.IPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Created by tlh on 2018/8/29.
 */

public class ByteXTransform extends CommonTransform<Context> {

    ByteXTransform(Context context) {
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
        if (!context.extension.isEnable()) {
            return Collections.emptyList();
        }
        return context.extension.getPlugins();
    }

    @Override
    protected void release() {
        context.extension.clearPlugins();
        super.release();
    }
}
