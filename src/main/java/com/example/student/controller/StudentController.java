package com.example.student.controller;

import com.example.student.dto.StudentCreateDto;
import com.example.student.dto.StudentResponseDto;
import com.example.student.service.StudentService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public void createStudent(@RequestBody StudentCreateDto dto) {
        studentService.createStudent(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
    }

    @PostMapping("/transaction-off")
    public void saveWithoutTransaction(@RequestBody StudentCreateDto dto) {
        studentService.saveWithoutTransaction(dto);
    }

    @PostMapping("/transaction-on")
    public void saveWithTransaction(@RequestBody StudentCreateDto dto) {
        studentService.saveWithTransaction(dto);
    }
}