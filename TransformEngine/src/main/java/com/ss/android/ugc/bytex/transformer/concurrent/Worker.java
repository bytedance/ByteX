package com.ss.android.ugc.bytex.transformer.concurrent;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class Worker {
    protected final LinkedList<Future<?>> futures = new LinkedList<Future<?>>() {
        @Override
        public synchronized boolean add(Future<?> future) {
            return super.add(future);
        }

        @Override
        public synchronized Future<?> pollFirst() {
            return super.pollFirst();
        }
    };
    protected ExecutorService executor;

    Worker(ExecutorService executor) {
        this.executor = executor;
    }

    public void execute(Runnable runnable) {
        futures.add(executor.submit(runnable));
    }

    public <T> Future<T> submit(Callable<T> callable) {
        Future<T> future = executor.submit(callable);
        futures.add(future);
        return future;
    }

    public void await() throws IOException {
        Future<?> future;
        while ((future = futures.pollFirst()) != null) {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                } else if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else if (e.getCause() instanceof Error) {
                    throw (Error) e.getCause();
                }
                throw new RuntimeException(e.getCause());
            }
        }
    }

    public <I> void submitAndAwait(Collection<I> is, Consumer<I> consumer) throws IOException {
        is.stream().map(f -> (Runnable) () -> consumer.accept(f)).forEach(this::execute);
        await();
    }
}
