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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void waitAllWrapsFutureExceptionInIllegalState() throws Exception {
        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("test"));

        Method waitAll = RaceConditionDemoService.class.getDeclaredMethod("waitAll", List.class);
        waitAll.setAccessible(true);

        InvocationTargetException ite =
                assertThrows(
                        InvocationTargetException.class,
                        () -> waitAll.invoke(null, List.of(failed)));
        assertInstanceOf(IllegalStateException.class, ite.getCause());
        assertTrue(ite.getCause().getMessage().contains("Ошибка при ожидании"));
    }
}
