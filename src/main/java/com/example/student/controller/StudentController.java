package com.example.student.controller;

import com.example.student.dto.StudentResponseDto;
import com.example.student.service.StudentService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<StudentResponseDto> getStudentById(
            @PathVariable String studentId) {

        StudentResponseDto student = studentService.findStudentById(studentId);

        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(student);
    }

    @GetMapping
    public ResponseEntity<List<StudentResponseDto>> getStudentsByGroup(
            @RequestParam(required = false) String groupNumber) {

        List<StudentResponseDto> students =
                studentService.findStudentsByGroup(groupNumber);

        return ResponseEntity.ok(students);
    }
}