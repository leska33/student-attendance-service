package com.example.student.controller;

import com.example.student.dto.GradeCreateDto;
import com.example.student.dto.GradeResponseDto;
import com.example.student.service.GradeService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PostMapping
    public ResponseEntity<GradeResponseDto> createGrade(@RequestBody GradeCreateDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(gradeService.createGrade(dto));
    }

    @GetMapping
    public ResponseEntity<List<GradeResponseDto>> getAllGrades() {
        return ResponseEntity.ok(gradeService.getAllGradesDtoOptimized());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GradeResponseDto> getGradeById(@PathVariable Long id) {
        return ResponseEntity.ok(gradeService.getGradeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GradeResponseDto> updateGrade(
            @PathVariable Long id,
            @RequestBody GradeCreateDto dto) {

        return ResponseEntity.ok(gradeService.updateGrade(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long id) {
        gradeService.deleteGrade(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lazy")
    public ResponseEntity<List<GradeResponseDto>> getGradesLazy() {
        return ResponseEntity.ok(gradeService.getAllGradesDtoLazy());
    }
}