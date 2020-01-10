package com.ss.android.ugc.bytex.transformer.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Schedulers {
    private static final int cpuCount = Runtime.getRuntime().availableProcessors();
    private final static ExecutorService IO = new ThreadPoolExecutor(0, cpuCount * 3,
            30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    private static final ExecutorService COMPUTATION = Executors.newWorkStealingPool(cpuCount);

    public static Worker IO() {
        return new Worker(IO);
    }

    public static Worker COMPUTATION() {
        return new Worker(COMPUTATION);
    }

    public static ForkJoinPool FORKJOINPOOL() {
        return (ForkJoinPool) COMPUTATION;
    }
}
