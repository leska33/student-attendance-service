package com.example.student.controller.query;

import com.example.student.dto.TeacherResponseDto;
import com.example.student.service.TeacherQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teachers/filter")
public class TeacherQueryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherQueryController.class);

    private final TeacherQueryService teacherQueryService;

    public TeacherQueryController(TeacherQueryService teacherQueryService) {
        this.teacherQueryService = teacherQueryService;
    }

    @GetMapping("/jpql")
    public List<TeacherResponseDto> getByDisciplineJPQL(
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeHash = discipline.hashCode();
        LOGGER.info("TEACHER_JPQL request - key={}, page={}, size={}", safeHash, page, size);

        return teacherQueryService.getTeachersByDisciplineJPQL(discipline, page, size);
    }

    @GetMapping("/native")
    public List<TeacherResponseDto> getByDisciplineNative(
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeHash = discipline.hashCode();
        LOGGER.info("TEACHER_NATIVE request - key={}, page={}, size={}", safeHash, page, size);

        return teacherQueryService.getTeachersByDisciplineNative(discipline, page, size);
    }
}