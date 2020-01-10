package com.ss.android.ugc.bytex.common.log;

import java.util.HashMap;

public class Timer {
    private final HashMap<String, Long> records;
    private long start;

    public Timer() {
        records = new HashMap<>();
        start = System.currentTimeMillis();
    }

    public void startRecord(String key) {
        records.put(key, System.currentTimeMillis());
    }

    public void stopRecord(String key, String format) {
        Long start = records.get(key);
        if (start == null) {
            return;
        }
        long cost = System.currentTimeMillis() - start;
        LevelLog.sDefaultLogger.i(String.format(format, cost));
    }

    public void record(String format) {
        long cost = System.currentTimeMillis() - start;
        LevelLog.sDefaultLogger.i(String.format(format, cost));
    }

    public void reset() {
        start = System.currentTimeMillis();
        records.clear();
    }
}
