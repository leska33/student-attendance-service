package com.example.student.service;

import com.example.student.dto.AsyncTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AsyncDemoTaskServiceTest {

    @Autowired
    private AsyncDemoTaskService asyncDemoTaskService;

    @Test
    void submitReturnsIdAndCompletesAsync() {
        String id = asyncDemoTaskService.submitTask();
        assertTrue(id.startsWith(""));

        AsyncTaskStatus last = pollUntilTerminalOrTimeout(asyncDemoTaskService, id, 25_000);
        assertEquals(AsyncTaskStatus.READY, last);
    }

    @Test
    void submitWithItemsCountStoresDetailAndCompletesAsync() {
        String id = asyncDemoTaskService.submitTask(3);
        assertTrue(id.startsWith(""));

        var initial = asyncDemoTaskService.findStatus(id);
        assertTrue(initial.isPresent());
        assertTrue(initial.get().detail() != null && !initial.get().detail().isBlank());

        AsyncTaskStatus last = pollUntilTerminalOrTimeout(asyncDemoTaskService, id, 25_000);
        assertEquals(AsyncTaskStatus.READY, last);
    }

    @Test
    void runAsyncWithUnknownTaskIdCompletesWithoutError() {
        CompletableFuture<Void> future = asyncDemoTaskService.runAsync("missing-id");
        assertDoesNotThrow(() -> future.get(3, TimeUnit.SECONDS));
        assertTrue(future.isDone());
    }

    @Test
    void findStatusUnknownReturnsEmpty() {
        assertTrue(asyncDemoTaskService.findStatus("unknown-task-id").isEmpty());
    }

    @Test
    void runAsyncInterruptedMarksTaskFailed() throws Exception {
        AsyncDemoTaskService svc = new AsyncDemoTaskService(null);
        Field selfField = AsyncDemoTaskService.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(svc, svc);

        String taskId = "T-interrupt";
        Field tasksField = AsyncDemoTaskService.class.getDeclaredField("tasks");
        tasksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, AsyncDemoTaskService.TaskState> tasks =
                (Map<String, AsyncDemoTaskService.TaskState>) tasksField.get(svc);
        tasks.put(taskId, new AsyncDemoTaskService.TaskState(AsyncTaskStatus.RECEIVED, null));

        Thread worker =
                new Thread(
                        () -> {
                            try {
                                svc.runAsync(taskId).join();
                            } catch (Exception _) {
                                // future завершается сразу после установки статуса FAILED
                            }
                        });
        worker.start();
        waitUntilStatusChangesFromReceived(svc, taskId, 1_000L);
        worker.interrupt();
        worker.join(10_000L);

        var opt = svc.findStatus(taskId);
        assertTrue(opt.isPresent());
        assertEquals(AsyncTaskStatus.FAILED, opt.get().status());
        assertEquals("Прервано", opt.get().detail());
    }

    private static AsyncTaskStatus pollUntilTerminalOrTimeout(
            AsyncDemoTaskService service, String taskId, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        AsyncTaskStatus status = AsyncTaskStatus.RECEIVED;
        while (System.currentTimeMillis() < deadline) {
            var opt = service.findStatus(taskId);
            if (opt.isEmpty()) {
                throw new AssertionError("Task disappeared: " + taskId);
            }
            status = opt.get().status();
            if (status == AsyncTaskStatus.READY || status == AsyncTaskStatus.FAILED) {
                return status;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25));
        }
        throw new AssertionError("Timeout, last status: " + status);
    }

    private static void waitUntilStatusChangesFromReceived(
            AsyncDemoTaskService service, String taskId, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            var status = service.findStatus(taskId).map(s -> s.status()).orElse(AsyncTaskStatus.FAILED);
            if (status != AsyncTaskStatus.RECEIVED) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
    }
}
