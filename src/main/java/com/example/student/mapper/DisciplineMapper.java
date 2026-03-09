package com.example.student.mapper;

import com.example.student.dto.DisciplineResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Student;

import java.util.List;
import java.util.stream.Collectors;

public final class DisciplineMapper {

    private DisciplineMapper() {
    }

    public static DisciplineResponseDto toDto(Discipline discipline) {

        if (discipline == null) {
            return null;
        }

        String teacherName = null;

        if (discipline.getTeacher() != null) {
            teacherName = discipline.getTeacher().getFullName();
        }

        List<String> studentNames = null;

        if (discipline.getStudents() != null) {
            studentNames = discipline.getStudents()
                    .stream()
                    .map(Student::getFullName)
                    .collect(Collectors.toList());
        }

        return new DisciplineResponseDto(
                discipline.getId(),
                discipline.getName(),
                teacherName,
                studentNames
        );
    }
}