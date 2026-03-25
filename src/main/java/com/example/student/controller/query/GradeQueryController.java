package com.example.student.controller.query;

import com.example.student.dto.GradeResponseDto;
import com.example.student.service.GradeQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/grades/filter")
public class GradeQueryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradeQueryController.class);

    private final GradeQueryService gradeQueryService;

    public GradeQueryController(GradeQueryService gradeQueryService) {
        this.gradeQueryService = gradeQueryService;
    }

    @GetMapping("/jpql")
    public List<GradeResponseDto> getByStudentAndDisciplineJPQL(
            @RequestParam String student,
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeHash = (student + discipline).hashCode();
        LOGGER.info("GRADE_JPQL request - key={}, page={}, size={}", safeHash, page, size);

        return gradeQueryService.getGradesByStudentAndDisciplineJPQL(student, discipline, page, size);
    }

    @GetMapping("/native")
    public List<GradeResponseDto> getByStudentAndDisciplineNative(
            @RequestParam String student,
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeHash = (student + discipline).hashCode();
        LOGGER.info("GRADE_NATIVE request - key={}, page={}, size={}", safeHash, page, size);

        return gradeQueryService.getGradesByStudentAndDisciplineNative(student, discipline, page, size);
    }
}