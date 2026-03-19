package com.example.student.controller.query;

import com.example.student.dto.StudentResponseDto;
import com.example.student.service.StudentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/students/filter")
public class StudentQueryController {

    private final StudentQueryService studentQueryService;

    public StudentQueryController(StudentQueryService studentQueryService) {
        this.studentQueryService = studentQueryService;
    }

    @GetMapping("/jpql")
    public List<StudentResponseDto> getByDisciplineJPQL(
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return studentQueryService.getStudentsByDisciplineJPQL(discipline, page, size);
    }

    @GetMapping("/native")
    public List<StudentResponseDto> getByDisciplineNative(
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return studentQueryService.getStudentsByDisciplineNative(discipline, page, size);
    }
}