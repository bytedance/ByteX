package com.ss.android.ugc.bytex.coverage_lib;

/**
 * Created by jiangzilai on 2019-12-09.
 * must keep this class
 */
public class CoverageLogger {
    private CoverageLogger() {
    }

    public static void Log(int mapping) {
        CoverageHandler.addData(mapping);
    }
}
