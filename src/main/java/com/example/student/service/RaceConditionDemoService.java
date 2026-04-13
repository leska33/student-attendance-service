package com.example.student.service;

import com.example.student.dto.RaceDemoResult;
import com.example.student.dto.ThreadSafeCounterDemoResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;


@Service
public class RaceConditionDemoService {

    public static final int DEFAULT_THREADS = 52;
    public static final int DEFAULT_INCREMENTS_PER_THREAD = 2000;
    private static final int DEMO_THREADS = 60;
    private static final int DEMO_TOTAL_INCREMENTS = 10_000;

    private final ThreadSafeCounter threadSafeCounter;

    public RaceConditionDemoService(ThreadSafeCounter threadSafeCounter) {
        this.threadSafeCounter = threadSafeCounter;
    }

    public RaceDemoResult runRaceDemo() {
        return runRaceDemo(DEFAULT_THREADS, DEFAULT_INCREMENTS_PER_THREAD);
    }

    public ThreadSafeCounterDemoResult runThreadSafeCounterDemo() {
        return runThreadSafeCounterDemo(DEFAULT_THREADS, DEFAULT_INCREMENTS_PER_THREAD);
    }

    public ThreadSafeCounterDemoResult runThreadSafeCounterDemo(int threadCount, int incrementsPerThread) {
        long expected = (long) threadCount * (long) incrementsPerThread;
        threadSafeCounter.reset();

        try (ExecutorService pool = Executors.newFixedThreadPool(threadCount)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        threadSafeCounter.incrementAndGet();
                    }
                }));
            }
            waitAll(futures);
        }

        return new ThreadSafeCounterDemoResult(
                threadCount,
                incrementsPerThread,
                expected,
                threadSafeCounter.get()
        );
    }

    public RaceDemoResult runRaceDemo(int threadCount, int incrementsPerThread) {
        long expected = (long) threadCount * (long) incrementsPerThread;

        UnsafeCounter unsafe = new UnsafeCounter();
        AtomicLong safe = new AtomicLong(0L);

        try (ExecutorService pool = Executors.newFixedThreadPool(threadCount)) {
            List<Future<?>> unsafeFutures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                unsafeFutures.add(pool.submit(() -> {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        unsafe.increment();
                    }
                }));
            }
            waitAll(unsafeFutures);

            List<Future<?>> safeFutures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                safeFutures.add(pool.submit(() -> {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        safe.incrementAndGet();
                    }
                }));
            }
            waitAll(safeFutures);
        }

        return new RaceDemoResult(
                threadCount,
                incrementsPerThread,
                expected,
                unsafe.get(),
                safe.get()
        );
    }

    public String runRaceConditionProblemDemoText() {
        long result = runUnsafeOnlyDemo(DEMO_THREADS, DEMO_TOTAL_INCREMENTS);
        return "Демонстрация проблемы Race Condition:\n"
                + "Ожидание: " + DEMO_TOTAL_INCREMENTS + "\n"
                + "Получили: " + result + "\n";
    }

    public String runRaceConditionSolutionDemoText() {
        long result = runAtomicOnlyDemo(DEMO_THREADS, DEMO_TOTAL_INCREMENTS);
        return "Решение проблемы Race Condition:\n"
                + "Ожидание: " + DEMO_TOTAL_INCREMENTS + "\n"
                + "Получили: " + result + "\n";
    }

    private long runUnsafeOnlyDemo(int threadCount, int totalIncrements) {
        UnsafeCounter unsafe = new UnsafeCounter();
        try (ExecutorService pool = Executors.newFixedThreadPool(threadCount)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < totalIncrements; i++) {
                futures.add(pool.submit(unsafe::increment));
            }
            waitAll(futures);
        }
        return unsafe.get();
    }

    private long runAtomicOnlyDemo(int threadCount, int totalIncrements) {
        AtomicLong safe = new AtomicLong(0L);
        try (ExecutorService pool = Executors.newFixedThreadPool(threadCount)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < totalIncrements; i++) {
                futures.add(pool.submit(safe::incrementAndGet));
            }
            waitAll(futures);
        }
        return safe.get();
    }

    private static void waitAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Поток был прерван при ожидании задач демо гонки", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Ошибка при выполнении задач демо гонки", e);
            }
        }
    }

    static final class UnsafeCounter {
        private int value;

        void increment() {
            value++;
        }

        long get() {
            return value;
        }
    }
}
