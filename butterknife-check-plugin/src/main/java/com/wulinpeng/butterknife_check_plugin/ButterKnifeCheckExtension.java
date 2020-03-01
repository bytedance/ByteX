package com.wulinpeng.butterknife_check_plugin;

import com.ss.android.ugc.bytex.common.BaseExtension;

/**
 * author：wulinpeng
 * date：2020-02-18 11:33
 * desc:
 */
public class ButterKnifeCheckExtension extends BaseExtension {

    {
        enableInDebug(true);
    }

    @Override public String getName() {
        return "butterknife-check-plugin";
    }
}
