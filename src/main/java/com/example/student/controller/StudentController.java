package com.example.student.controller;

import com.example.student.dto.StudentResponseDto;
import com.example.student.service.StudentService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/lazy")
    public List<StudentResponseDto> getStudentsLazy() {
        return studentService.getAllStudentsDtoLazy();
    }

    @GetMapping("/optimized")
    public List<StudentResponseDto> getStudentsOptimized() {
        return studentService.getAllStudentsDtoOptimized();
    }
}