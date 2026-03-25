package com.example.student.controller;

import com.example.student.dto.StudentCreateDto;
import com.example.student.dto.StudentResponseDto;
import com.example.student.service.StudentQueryService;
import com.example.student.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentQueryService queryService;

    public StudentController(StudentService studentService, StudentQueryService queryService) {
        this.studentService = studentService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<StudentResponseDto> createStudent(@RequestBody StudentCreateDto dto) {
        StudentResponseDto response = studentService.createStudent(dto);
        queryService.invalidateCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<StudentResponseDto>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudentsDtoOptimized());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDto> updateStudent(
            @PathVariable Long id,
            @RequestBody StudentCreateDto dto) {

        StudentResponseDto response = studentService.updateStudent(id, dto);
        queryService.invalidateCache();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        queryService.invalidateCache();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lazy")
    public ResponseEntity<List<StudentResponseDto>> getStudentsLazy() {
        return ResponseEntity.ok(studentService.getAllStudentsDtoLazy());
    }
}