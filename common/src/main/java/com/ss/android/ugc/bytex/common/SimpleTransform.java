package com.ss.android.ugc.bytex.common;

import java.util.Collections;
import java.util.List;

/**
 * Created by yangzhiqian on 2019/4/2<br/>
 * Desc: handle with a single plugin.
 * Note: Transform can not be an instance of innerclass!!!
 */
public class SimpleTransform<Context extends BaseContext> extends CommonTransform<Context> {
    private IPlugin plugin;

    SimpleTransform(Context context, IPlugin plugin) {
        super(context);
        this.plugin = plugin;
    }

    @Override
    protected List<IPlugin> getPlugins() {
        return Collections.singletonList(plugin);
    }
}
