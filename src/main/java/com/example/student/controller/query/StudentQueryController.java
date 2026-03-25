package com.example.student.controller.query;

import com.example.student.dto.StudentResponseDto;
import com.example.student.service.StudentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/students/filter")
public class StudentQueryController {

    private final StudentQueryService service;

    public StudentQueryController(StudentQueryService service) {
        this.service = service;
    }

    @GetMapping("/jpql")
    public List<StudentResponseDto> getByDisciplineJPQL(
            @RequestParam String disciplineName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getStudentsByDisciplineJPQL(disciplineName, page, size);
    }

    @GetMapping("/native")
    public List<StudentResponseDto> getByDisciplineNative(
            @RequestParam String disciplineName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getStudentsByDisciplineNative(disciplineName, page, size);
    }
}