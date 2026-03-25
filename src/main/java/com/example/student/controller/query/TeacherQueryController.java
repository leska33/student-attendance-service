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

    private final TeacherQueryService teacherQueryService;

    public TeacherQueryController(TeacherQueryService teacherQueryService) {
        this.teacherQueryService = teacherQueryService;
    }

    @GetMapping("/jpql")
    public List<TeacherResponseDto> getByDisciplineJPQL(
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return teacherQueryService.getTeachersByDisciplineJPQL(discipline, page, size);
    }

    @GetMapping("/native")
    public List<TeacherResponseDto> getByDisciplineNative(
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return teacherQueryService.getTeachersByDisciplineNative(discipline, page, size);
    }
}