package com.example.student.controller;

import com.example.student.dto.StudentCreateDto;
import com.example.student.dto.StudentResponseDto;
import com.example.student.service.StudentQueryService;
import com.example.student.service.StudentService;
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

@Tag(name = "Студенты", description = "Операции со студентами")
@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentQueryService queryService;

    public StudentController(StudentService studentService, StudentQueryService queryService) {
        this.studentService = studentService;
        this.queryService = queryService;
    }

    @Operation(summary = "Создать студента")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Создано"),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
                    @ApiResponse(responseCode = "404", description = "Группа или дисциплины не найдены")
    })
    @PostMapping
    public ResponseEntity<StudentResponseDto> createStudent(@Valid @RequestBody StudentCreateDto dto) {
        StudentResponseDto response = studentService.createStudent(dto);
        queryService.invalidateCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Получить всех студентов")
    @GetMapping
    public ResponseEntity<List<StudentResponseDto>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudentsDtoOptimized());
    }

    @Operation(summary = "Получить студента по ID")
    @ApiResponse(responseCode = "404", description = "Студент не найден")
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @Operation(summary = "Обновить студента")
    @ApiResponse(responseCode = "404", description = "Студент не найден")
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDto> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentCreateDto dto) {

        StudentResponseDto response = studentService.updateStudent(id, dto);
        queryService.invalidateCache();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Удалить студента")
    @ApiResponse(responseCode = "404", description = "Студент не найден")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        queryService.invalidateCache();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить студентов (lazy)")
    @GetMapping("/lazy")
    public ResponseEntity<List<StudentResponseDto>> getStudentsLazy() {
        return ResponseEntity.ok(studentService.getAllStudentsDtoLazy());
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<StudentResponseDto>> createBulk(
            @RequestBody List<StudentCreateDto> dtos) {

        return ResponseEntity.ok(studentService.createStudentsBulk(dtos));
    }

    @PostMapping("/bulk/no-transaction")
    public ResponseEntity<List<StudentResponseDto>> createBulkNoTransaction(
            @RequestBody List<StudentCreateDto> dtos) {

        return ResponseEntity.ok(studentService.createStudentsBulkWithoutTransaction(dtos));
    }
}