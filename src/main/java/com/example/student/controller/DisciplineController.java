package com.example.student.controller;

import com.example.student.dto.DisciplineCreateDto;
import com.example.student.dto.DisciplineResponseDto;
import com.example.student.service.DisciplineQueryService;
import com.example.student.service.DisciplineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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

@Tag(name = "Дисциплины", description = "Операции с дисциплинами")
@RestController
@RequestMapping("/disciplines")
public class DisciplineController {

    private final DisciplineService disciplineService;
    private final DisciplineQueryService queryService;

    public DisciplineController(DisciplineService disciplineService, DisciplineQueryService queryService) {
        this.disciplineService = disciplineService;
        this.queryService = queryService;
    }

    @Operation(summary = "Создать дисциплину")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Создано"),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
                    @ApiResponse(responseCode = "404", description = "Учитель не найден")
    })
    @PostMapping
    public ResponseEntity<DisciplineResponseDto> createDiscipline(@Valid @RequestBody DisciplineCreateDto dto) {
        DisciplineResponseDto response = disciplineService.createDiscipline(dto);
        queryService.invalidateCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Получить все дисциплины")
    @GetMapping
    public ResponseEntity<List<DisciplineResponseDto>> getAllDisciplines() {
        return ResponseEntity.ok(disciplineService.getAllDisciplinesDtoOptimized());
    }

    @Operation(summary = "Получить дисциплину по ID")
    @GetMapping("/{id}")
    public ResponseEntity<DisciplineResponseDto> getDisciplineById(@PathVariable Long id) {
        return ResponseEntity.ok(disciplineService.getDisciplineById(id));
    }

    @Operation(summary = "Обновить дисциплину")
    @PutMapping("/{id}")
    public ResponseEntity<DisciplineResponseDto> updateDiscipline(
            @PathVariable Long id,
            @Valid @RequestBody DisciplineCreateDto dto) {

        DisciplineResponseDto response = disciplineService.updateDiscipline(id, dto);
        queryService.invalidateCache();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Удалить дисциплину")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiscipline(@PathVariable Long id) {
        disciplineService.deleteDiscipline(id);
        queryService.invalidateCache();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить дисциплины (lazy загрузка)")
    @GetMapping("/lazy")
    public ResponseEntity<List<DisciplineResponseDto>> getDisciplinesLazy() {
        return ResponseEntity.ok(disciplineService.getAllDisciplinesDtoLazy());
    }
}