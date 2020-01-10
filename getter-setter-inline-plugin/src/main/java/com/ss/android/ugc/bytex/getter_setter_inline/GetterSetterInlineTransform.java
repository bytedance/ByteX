package com.ss.android.ugc.bytex.getter_setter_inline;

import com.ss.android.ugc.bytex.common.CommonTransform;
import com.ss.android.ugc.bytex.common.IPlugin;

import java.util.Collections;
import java.util.List;

public class GetterSetterInlineTransform extends CommonTransform<Context> {
    private final IPlugin plugin;

    public GetterSetterInlineTransform(Context context, IPlugin plugin) {
        super(context);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Getter&SetterInline";
    }

    @Override
    protected List<IPlugin> getPlugins() {
        return Collections.singletonList(plugin);
    }
}
