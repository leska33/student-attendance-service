package com.example.student.controller;

import com.example.student.dto.GradeResponseDto;
import com.example.student.entity.Grade;
import com.example.student.service.GradeService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping("/lazy")
    public List<GradeResponseDto> getGradesLazy() {
        return gradeService.getAllGradesDtoLazy();
    }

    @GetMapping("/optimized")
    public List<GradeResponseDto> getGradesOptimized() {
        return gradeService.getAllGradesDtoOptimized();
    }

    @PostMapping
    public Grade createGrade(@RequestBody Grade grade) {
        return gradeService.createGrade(grade);
    }

    @DeleteMapping("/{id}")
    public void deleteGrade(@PathVariable Long id) {
        gradeService.deleteGrade(id);
    }
}