package com.ss.android.ugc.bytex.shrinkR;

import com.ss.android.ugc.bytex.common.CommonTransform;
import com.ss.android.ugc.bytex.common.IPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Created by tlh on 2018/8/29.
 */

public class ShrinkRFileTransform extends CommonTransform<Context> {

    private IPlugin plugin;

    public ShrinkRFileTransform(Context context, IPlugin plugin) {
        super(context);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ShrinkRFile";
    }

    @Override
    protected List<IPlugin> getPlugins() {
        return Collections.singletonList(plugin);
    }

}
