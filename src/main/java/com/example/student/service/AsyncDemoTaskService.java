package com.example.student.service;

import com.example.student.dto.AsyncTaskStatus;
import com.example.student.dto.AsyncTaskStatusResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AsyncDemoTaskService {

    static final long RECEIVED_DELAY_MS = 8_000L;
    static final long PROCESSED_DELAY_MS = 8_000L;
    static final long READY_DELAY_MS = 6_000L;

    private final AsyncDemoTaskService self;
    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    public AsyncDemoTaskService(@Lazy AsyncDemoTaskService self) {
        this.self = self;
    }

    public String submitTask() {
        String taskId = "T-" + idSequence.incrementAndGet();
        tasks.put(taskId, new TaskState(AsyncTaskStatus.RECEIVED, "Задача получена"));
        self.runAsync(taskId);
        return taskId;
    }

    public String submitTask(int itemsCount) {
        String taskId = "T-" + idSequence.incrementAndGet();
        tasks.put(taskId, new TaskState(AsyncTaskStatus.RECEIVED, "Получено элементов: " + itemsCount));
        self.runAsync(taskId);
        return taskId;
    }

    @Async("demoTaskExecutor")
    public CompletableFuture<Void> runAsync(String taskId) {
        TaskState state = tasks.get(taskId);
        if (state == null) {
            return CompletableFuture.completedFuture(null);
        }
        state.status = AsyncTaskStatus.RECEIVED;
        state.detail = "Задача в очереди";
        try {
            Thread.sleep(RECEIVED_DELAY_MS);
            state.status = AsyncTaskStatus.PROCESSED;
            state.detail = "Данные обработаны";
            Thread.sleep(PROCESSED_DELAY_MS);
            state.detail = "Подготовка к выдаче результата";
            Thread.sleep(READY_DELAY_MS);
            state.status = AsyncTaskStatus.READY;
            state.detail = "Задача готова";
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            state.status = AsyncTaskStatus.FAILED;
            state.detail = "Прервано";
        }
        return CompletableFuture.completedFuture(null);
    }

    public Optional<AsyncTaskStatusResponse> findStatus(String taskId) {
        TaskState state = tasks.get(taskId);
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(new AsyncTaskStatusResponse(taskId, state.status, state.detail));
    }

    static final class TaskState {
        volatile AsyncTaskStatus status;
        volatile String detail;

        TaskState(AsyncTaskStatus status, String detail) {
            this.status = status;
            this.detail = detail;
        }
    }
}
