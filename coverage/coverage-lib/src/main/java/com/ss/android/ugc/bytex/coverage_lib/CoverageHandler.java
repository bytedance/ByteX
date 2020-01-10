package com.ss.android.ugc.bytex.coverage_lib;

/**
 * Created by jiangzilai on 2019-07-19.
 * 把控制逻辑交给主工程实现，拥有统一线程管理等好处
 */
public class CoverageHandler {

    volatile static boolean hasInit = false;
    private static CoveragePlugin plugin;

    private CoverageHandler() {
    }

    /**
     *
     * @param plugin
     */
    public static void init(CoveragePlugin plugin) {
        if (!hasInit) {
            CoverageHandler.plugin = plugin;
            hasInit = true;
        }
    }

    static void addData(int id) {
        if (hasInit) {
            plugin.addData(id);
        }
    }
}
