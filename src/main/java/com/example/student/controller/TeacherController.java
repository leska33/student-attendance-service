package com.example.student.controller;

import com.example.student.dto.TeacherResponseDto;
import com.example.student.entity.Teacher;
import com.example.student.service.TeacherService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/lazy")
    public List<TeacherResponseDto> getTeachersLazy() {
        return teacherService.getAllTeachersDtoLazy();
    }

    @GetMapping("/optimized")
    public List<TeacherResponseDto> getTeachersOptimized() {
        return teacherService.getAllTeachersDtoOptimized();
    }

    @PostMapping
    public Teacher createTeacher(@RequestBody Teacher teacher) {
        return teacherService.createTeacher(teacher);
    }

    @DeleteMapping("/{id}")
    public void deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
    }
}