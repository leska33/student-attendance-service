package com.example.student.service;

import com.example.student.dto.RaceDemoResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class RaceConditionDemoServiceTest {

    @Autowired
    private RaceConditionDemoService raceConditionDemoService;

    @Test
    void atomicCounterMatchesExpectedUnsafeShowsLostUpdates() {
        RaceDemoResult result = raceConditionDemoService.runRaceDemo();

        assertEquals(
                (long) RaceConditionDemoService.DEFAULT_THREADS
                        * RaceConditionDemoService.DEFAULT_INCREMENTS_PER_THREAD,
                result.expectedTotal());
        assertEquals(result.expectedTotal(), result.atomicCounterResult());
        assertTrue(
                result.unsafeCounterResult() < result.expectedTotal(),
                "Ожидалась потеря обновлений у небезопасного счётчика: "
                        + result.unsafeCounterResult() + " vs " + result.expectedTotal());
    }

    @Test
    void runThreadSafeCounterDemoReturnsExpectedValue() {
        var result = raceConditionDemoService.runThreadSafeCounterDemo();

        assertEquals(
                (long) RaceConditionDemoService.DEFAULT_THREADS
                        * RaceConditionDemoService.DEFAULT_INCREMENTS_PER_THREAD,
                result.expectedTotal());
        assertEquals(result.expectedTotal(), result.threadSafeCounterResult());
    }

    @Test
    void runThreadSafeCounterDemoWithCustomInputReturnsExpectedValue() {
        var result = raceConditionDemoService.runThreadSafeCounterDemo(4, 25);

        assertEquals(4, result.threads());
        assertEquals(25, result.incrementsPerThread());
        assertEquals(100L, result.expectedTotal());
        assertEquals(100L, result.threadSafeCounterResult());
    }

    @Test
    void raceConditionTextDemosContainExpectedLabels() {
        String problemText = raceConditionDemoService.runRaceConditionProblemDemoText();
        assertTrue(problemText.contains("Демонстрация проблемы Race Condition:"));
        assertTrue(problemText.contains("Ожидание: 10000"));
        assertTrue(problemText.contains("Получили: "));

        String solutionText = raceConditionDemoService.runRaceConditionSolutionDemoText();
        assertTrue(solutionText.contains("Решение проблемы Race Condition:"));
        assertTrue(solutionText.contains("Ожидание: 10000"));
        assertTrue(solutionText.contains("Получили: 10000"));
    }

    @Test
    void waitAllWrapsFutureExceptionInIllegalState() {
        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("test"));

        Method waitAll = assertDoesNotThrow(() -> RaceConditionDemoService.class.getDeclaredMethod("waitAll", List.class));
        waitAll.setAccessible(true);

        InvocationTargetException ite =
                assertThrows(
                        InvocationTargetException.class,
                        () -> waitAll.invoke(null, List.of(failed)));
        assertInstanceOf(IllegalStateException.class, ite.getCause());
        assertTrue(ite.getCause().getMessage().contains("Ошибка при выполнении"));
    }

    @Test
    void waitAllWrapsInterruptedExceptionAndSetsInterruptFlag() {
        Future<Object> interruptedFuture = new InterruptedFutureStub();
        Method waitAll = assertDoesNotThrow(() -> RaceConditionDemoService.class.getDeclaredMethod("waitAll", List.class));
        waitAll.setAccessible(true);

        InvocationTargetException ite =
                assertThrows(
                        InvocationTargetException.class,
                        () -> waitAll.invoke(null, List.of(interruptedFuture)));
        assertInstanceOf(IllegalStateException.class, ite.getCause());
        assertTrue(ite.getCause().getMessage().contains("Поток был прерван"));
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    private static final class InterruptedFutureStub implements Future<Object> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Object get() throws InterruptedException {
            throw new InterruptedException("interrupted-for-test");
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
            throw new InterruptedException("interrupted-for-test");
        }
    }
}
