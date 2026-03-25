package com.example.student.controller;

import com.example.student.dto.DisciplineCreateDto;
import com.example.student.dto.DisciplineResponseDto;
import com.example.student.service.DisciplineQueryService;
import com.example.student.service.DisciplineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/disciplines")
public class DisciplineController {

    private final DisciplineService disciplineService;
    private final DisciplineQueryService queryService;

    public DisciplineController(DisciplineService disciplineService, DisciplineQueryService queryService) {
        this.disciplineService = disciplineService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<DisciplineResponseDto> createDiscipline(@RequestBody DisciplineCreateDto dto) {
        DisciplineResponseDto response = disciplineService.createDiscipline(dto);
        queryService.invalidateCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DisciplineResponseDto>> getAllDisciplines() {
        return ResponseEntity.ok(disciplineService.getAllDisciplinesDtoOptimized());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplineResponseDto> getDisciplineById(@PathVariable Long id) {
        return ResponseEntity.ok(disciplineService.getDisciplineById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisciplineResponseDto> updateDiscipline(
            @PathVariable Long id,
            @RequestBody DisciplineCreateDto dto) {

        DisciplineResponseDto response = disciplineService.updateDiscipline(id, dto);
        queryService.invalidateCache();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiscipline(@PathVariable Long id) {
        disciplineService.deleteDiscipline(id);
        queryService.invalidateCache();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lazy")
    public ResponseEntity<List<DisciplineResponseDto>> getDisciplinesLazy() {
        return ResponseEntity.ok(disciplineService.getAllDisciplinesDtoLazy());
    }
}