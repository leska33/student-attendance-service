package com.example.student.controller.query;

import com.example.student.dto.TeacherResponseDto;
import com.example.student.service.TeacherQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teachers/filter")
public class TeacherQueryController {

    private final TeacherQueryService service;

    public TeacherQueryController(TeacherQueryService service) {
        this.service = service;
    }

    @GetMapping("/jpql")
    public List<TeacherResponseDto> getByDisciplineJPQL(
            @RequestParam String disciplineName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getTeachersByDisciplineJPQL(disciplineName, page, size);
    }

    @GetMapping("/native")
    public List<TeacherResponseDto> getByDisciplineNative(
            @RequestParam String disciplineName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getTeachersByDisciplineNative(disciplineName, page, size);
    }
}