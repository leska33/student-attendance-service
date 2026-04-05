package com.example.student.controller;

import com.example.student.dto.TeacherCreateDto;
import com.example.student.dto.TeacherResponseDto;
import com.example.student.service.TeacherQueryService;
import com.example.student.service.TeacherService;
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

@Tag(name = "Преподаватели", description = "Операции с преподавателями")
@RestController
@RequestMapping("/teachers")
public class TeacherController {

    private final TeacherService teacherService;
    private final TeacherQueryService queryService;

    public TeacherController(TeacherService teacherService, TeacherQueryService queryService) {
        this.teacherService = teacherService;
        this.queryService = queryService;
    }

    @Operation(summary = "Создать преподавателя")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Создано"),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    })
    @PostMapping
    public ResponseEntity<TeacherResponseDto> createTeacher(@Valid @RequestBody TeacherCreateDto dto) {
        TeacherResponseDto response = teacherService.createTeacher(dto);
        queryService.invalidateCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Получить всех преподавателей")
    @GetMapping
    public ResponseEntity<List<TeacherResponseDto>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachersDtoOptimized());
    }

    @Operation(summary = "Получить преподавателя по ID")
    @ApiResponse(responseCode = "404", description = "Преподаватель не найден")
    @GetMapping("/{id}")
    public ResponseEntity<TeacherResponseDto> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    @Operation(summary = "Обновить преподавателя")
    @ApiResponse(responseCode = "404", description = "Преподаватель не найден")
    @PutMapping("/{id}")
    public ResponseEntity<TeacherResponseDto> updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody TeacherCreateDto dto) {

        TeacherResponseDto response = teacherService.updateTeacher(id, dto);
        queryService.invalidateCache();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Удалить преподавателя")
    @ApiResponse(responseCode = "404", description = "Преподаватель не найден")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        queryService.invalidateCache();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить преподавателей (lazy)")
    @GetMapping("/lazy")
    public ResponseEntity<List<TeacherResponseDto>> getTeachersLazy() {
        return ResponseEntity.ok(teacherService.getAllTeachersDtoLazy());
    }
}