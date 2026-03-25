package com.example.student.controller.query;

import com.example.student.dto.GradeResponseDto;
import com.example.student.service.GradeQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/grades/filter")
public class GradeQueryController {

    private final GradeQueryService service;

    public GradeQueryController(GradeQueryService service) {
        this.service = service;
    }

    @GetMapping("/jpql")
    public List<GradeResponseDto> getByStudentAndDisciplineJPQL(
            @RequestParam String studentLastName,
            @RequestParam String disciplineName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getGradesByStudentAndDisciplineJPQL(studentLastName, disciplineName, page, size);
    }

    @GetMapping("/native")
    public List<GradeResponseDto> getByStudentAndDisciplineNative(
            @RequestParam String studentLastName,
            @RequestParam String disciplineName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getGradesByStudentAndDisciplineNative(studentLastName, disciplineName, page, size);
    }
}