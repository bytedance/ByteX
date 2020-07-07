package com.ss.android.ugc.bytex.base;

import com.ss.android.ugc.bytex.common.CommonTransform;
import com.ss.android.ugc.bytex.common.IPlugin;
import com.ss.android.ugc.bytex.common.log.LevelLog;
import com.ss.android.ugc.bytex.transformer.TransformContext;

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
    protected void init(TransformContext transformContext) {
        context.init(transformContext);
        super.init(transformContext);
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
}
