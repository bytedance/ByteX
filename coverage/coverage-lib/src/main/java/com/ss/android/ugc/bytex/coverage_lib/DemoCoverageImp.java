package com.ss.android.ugc.bytex.coverage_lib;

import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by jiangzilai on 2019-12-09.
 * Avoid use this Implementation, just a demo
 */
public class DemoCoverageImp implements CoveragePlugin {

    private ConcurrentHashMap<Integer, Integer> hashMap;
    private int interval = 2 * 60 * 1000;
    private long nextReportTime;
    private static final String TAG = "CoveragePlugin";
    private static final float K = 1024f;
    private static ExecutorService service;

    private DemoCoverageImp() {
        hashMap = new ConcurrentHashMap<Integer, Integer>(10000);
        nextReportTime = System.currentTimeMillis() + interval;
        service = Executors.newSingleThreadExecutor();
        initIdleHandler();
    }

    private void initIdleHandler() {
        Log.d(TAG, "init coverage plugin idleHandler");
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override public boolean queueIdle() {
                if (System.currentTimeMillis() > nextReportTime) {
                    // Sometimes the user do nothing, we skip this situation
                    if (hashMap.size() >= 20) {
                        service.execute(new Runnable() {
                            @Override public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for (Map.Entry<Integer, Integer> entry : hashMap.entrySet()) {
                                    stringBuilder.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
                                }
                                // you need to report to your service here
                                Log.d(TAG, "coverage report length : " + stringBuilder.length() + " cost byte before compress: "
                                        + (stringBuilder.length() / K) + " KB");
                                hashMap.clear();
                            }
                        });
                    }
                    nextReportTime = System.currentTimeMillis() + interval;
                }
                return true;
            }
        });
    }

    public static DemoCoverageImp getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override public void addData(int id) {
        Integer value = hashMap.get(id);
        if (value == null) {
            hashMap.put(id, 1);
        } else {
            hashMap.put(id, value + 1);
        }
    }

    private static class SingletonHolder {
        private static final DemoCoverageImp INSTANCE = new DemoCoverageImp();
    }
}
