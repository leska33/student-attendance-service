package com.example.student.controller;

import com.example.student.dto.TeacherCreateDto;
import com.example.student.dto.TeacherResponseDto;
import com.example.student.service.TeacherQueryService;
import com.example.student.service.TeacherService;
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
@RequestMapping("/teachers")
public class TeacherController {

    private final TeacherService teacherService;
    private final TeacherQueryService queryService;

    public TeacherController(TeacherService teacherService, TeacherQueryService queryService) {
        this.teacherService = teacherService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<TeacherResponseDto> createTeacher(@RequestBody TeacherCreateDto dto) {
        TeacherResponseDto response = teacherService.createTeacher(dto);
        queryService.invalidateCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TeacherResponseDto>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachersDtoOptimized());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherResponseDto> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeacherResponseDto> updateTeacher(
            @PathVariable Long id,
            @RequestBody TeacherCreateDto dto) {

        TeacherResponseDto response = teacherService.updateTeacher(id, dto);
        queryService.invalidateCache();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        queryService.invalidateCache();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lazy")
    public ResponseEntity<List<TeacherResponseDto>> getTeachersLazy() {
        return ResponseEntity.ok(teacherService.getAllTeachersDtoLazy());
    }
}