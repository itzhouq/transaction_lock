package org.example.util;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Slf4j
public class ThreadUtils {

    public static <T> void multiThreadRun(int threadNum, List<T> list, Consumer<List<T>> consumer, Executor executor) {
        CompletableFuture[] futures = new CompletableFuture[threadNum];
        int batch = list.size() / threadNum, indx = 0;
        while (indx < threadNum - 1) {
            int start = indx * batch, end = start + batch;
            futures[indx++] = CompletableFuture.runAsync(() -> consumer.accept(list.subList(start, end)), executor);
        }
        int start = indx * batch;
        futures[indx] = CompletableFuture.runAsync(() -> consumer.accept(list.subList(start, list.size())), executor);
        CompletableFuture.allOf(futures).join();
    }
}
