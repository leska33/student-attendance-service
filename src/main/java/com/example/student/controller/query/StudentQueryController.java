package com.example.student.controller.query;

import com.example.student.dto.StudentResponseDto;
import com.example.student.service.StudentQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/students/filter")
public class StudentQueryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentQueryController.class);

    private final StudentQueryService studentQueryService;

    public StudentQueryController(StudentQueryService studentQueryService) {
        this.studentQueryService = studentQueryService;
    }

    @GetMapping("/jpql")
    public List<StudentResponseDto> getByDisciplineJPQL(
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeHash = discipline.hashCode();
        LOGGER.info("STUDENT_JPQL request - key={}, page={}, size={}", safeHash, page, size);

        return studentQueryService.getStudentsByDisciplineJPQL(discipline, page, size);
    }

    @GetMapping("/native")
    public List<StudentResponseDto> getByDisciplineNative(
            @RequestParam String discipline,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeHash = discipline.hashCode();
        LOGGER.info("STUDENT_NATIVE request - key={}, page={}, size={}", safeHash, page, size);

        return studentQueryService.getStudentsByDisciplineNative(discipline, page, size);
    }
}