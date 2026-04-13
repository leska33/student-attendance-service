package com.example.student.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadSafeCounterTest {

    @Test
    void parallelIncrementsPreserveTotal() throws Exception {
        ThreadSafeCounter counter = new ThreadSafeCounter();
        int threads = 54;
        int perThread = 500;
        long expected = (long) threads * perThread;

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < perThread; i++) {
                        counter.incrementAndGet();
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        }

        assertEquals(expected, counter.get());
    }

    @Test
    void resetClearsValue() {
        ThreadSafeCounter counter = new ThreadSafeCounter();
        counter.incrementAndGet();
        counter.reset();
        assertEquals(0L, counter.get());
    }

    @Test
    void incrementAndGetIsMonotonic() {
        ThreadSafeCounter counter = new ThreadSafeCounter();
        long a = counter.incrementAndGet();
        long b = counter.incrementAndGet();
        assertTrue(b > a);
    }
}
