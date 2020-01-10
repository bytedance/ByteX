package com.ss.android.ugc.bytex.access_inline;

import com.ss.android.ugc.bytex.common.CommonTransform;
import com.ss.android.ugc.bytex.common.IPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Created by tlh on 2018/8/29.
 */

public class InlineAccessTransform extends CommonTransform<Context> {
    private final IPlugin plugin;

    public InlineAccessTransform(Context context, IPlugin plugin) {
        super(context);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "InlineAccess";
    }

    @Override
    protected List<IPlugin> getPlugins() {
        return Collections.singletonList(plugin);
    }
}
