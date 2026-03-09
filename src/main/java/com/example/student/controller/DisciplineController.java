package com.example.student.controller;

import com.example.student.dto.DisciplineResponseDto;
import com.example.student.service.DisciplineService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/disciplines")
public class DisciplineController {

    private final DisciplineService disciplineService;

    public DisciplineController(DisciplineService disciplineService) {
        this.disciplineService = disciplineService;
    }

    @GetMapping("/lazy")
    public List<DisciplineResponseDto> getDisciplinesLazy() {
        return disciplineService.getAllDisciplinesDtoLazy();
    }

    @GetMapping("/optimized")
    public List<DisciplineResponseDto> getDisciplinesOptimized() {
        return disciplineService.getAllDisciplinesDtoOptimized();
    }
}