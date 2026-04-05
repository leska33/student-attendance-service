package com.example.student.controller;

import com.example.student.dto.GradeCreateDto;
import com.example.student.dto.GradeResponseDto;
import com.example.student.service.GradeQueryService;
import com.example.student.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Оценки", description = "Операции с оценками")
@RestController
@RequestMapping("/grades")
public class GradeController {

    private final GradeService gradeService;
    private final GradeQueryService queryService;

    public GradeController(GradeService gradeService, GradeQueryService queryService) {
        this.gradeService = gradeService;
        this.queryService = queryService;
    }

    @Operation(summary = "Создать оценку")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Создано"),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
                    @ApiResponse(responseCode = "404", description = "Студент или дисциплина не найдены")
    })
    @PostMapping
    public ResponseEntity<GradeResponseDto> createGrade(@Valid @RequestBody GradeCreateDto dto) {
        GradeResponseDto response = gradeService.createGrade(dto);
        queryService.invalidateCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Получить все оценки")
    @GetMapping
    public ResponseEntity<List<GradeResponseDto>> getAllGrades() {
        return ResponseEntity.ok(gradeService.getAllGradesDtoOptimized());
    }

    @Operation(summary = "Получить оценку по ID")
    @GetMapping("/{id}")
    public ResponseEntity<GradeResponseDto> getGradeById(@PathVariable Long id) {
        return ResponseEntity.ok(gradeService.getGradeById(id));
    }

    @Operation(summary = "Обновить оценку")
    @PutMapping("/{id}")
    public ResponseEntity<GradeResponseDto> updateGrade(
            @PathVariable Long id,
            @Valid @RequestBody GradeCreateDto dto) {

        GradeResponseDto response = gradeService.updateGrade(id, dto);
        queryService.invalidateCache();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Удалить оценку")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long id) {
        gradeService.deleteGrade(id);
        queryService.invalidateCache();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить оценки (lazy)")
    @GetMapping("/lazy")
    public ResponseEntity<List<GradeResponseDto>> getGradesLazy() {
        return ResponseEntity.ok(gradeService.getAllGradesDtoLazy());
    }
}