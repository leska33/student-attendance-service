package com.example.student.controller;

import com.example.student.dto.AsyncTaskStatusResponse;
import com.example.student.dto.StudentCreateDto;
import com.example.student.service.AsyncDemoTaskService;
import com.example.student.service.RaceConditionDemoService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Демо: асинхронность и конкурентность", description = "Async, счётчик, гонка потоков")
@RestController
@RequestMapping("/api/concurrency")
public class ConcurrencyDemoController {

    private final AsyncDemoTaskService asyncDemoTaskService;
    private final RaceConditionDemoService raceConditionDemoService;

    public ConcurrencyDemoController(
            AsyncDemoTaskService asyncDemoTaskService,
            RaceConditionDemoService raceConditionDemoService) {
        this.asyncDemoTaskService = asyncDemoTaskService;
        this.raceConditionDemoService = raceConditionDemoService;
    }

    @Operation(summary = "Демонстрация асинхронной бизнес-операции через @Async")
    @ApiResponse(responseCode = "202", description = "Задача принята в обработку")
    @PostMapping("/async")
    public ResponseEntity<Map<String, String>> submitStudentsAsync(@Valid @RequestBody List<StudentCreateDto> students) {
        String taskId = asyncDemoTaskService.submitTask(students.size());
        return ResponseEntity.accepted().body(Map.of("taskId", taskId));
    }

    @Operation(summary = "Демонстрация получения статуса выполнения асинхронной бизнес-операции")
    @ApiResponse(responseCode = "404", description = "Задача не найдена")
    @GetMapping("/async/status/{taskId}")
    public ResponseEntity<AsyncTaskStatusResponse> getAsyncStatus(@PathVariable String taskId) {
        return asyncDemoTaskService.findStatus(taskId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Демонстрация race condition", description = "Демонстрация ошибки Race Condition при 60 потоках")
    @GetMapping("/test_problem")
    public ResponseEntity<String> testRaceProblem() {
        return ResponseEntity.ok(raceConditionDemoService.runRaceConditionProblemDemoText());
    }

    @Operation(summary = "Продемонстрировать решение Race Condition", description = "Демонстрация корректной работы с AtomicLong")
    @GetMapping("/test_solution")
    public ResponseEntity<String> testRaceSolution() {
        return ResponseEntity.ok(raceConditionDemoService.runRaceConditionSolutionDemoText());
    }
}
