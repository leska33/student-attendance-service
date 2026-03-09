package com.example.student.mapper;

import com.example.student.dto.TeacherResponseDto;
import com.example.student.entity.Teacher;
import com.example.student.entity.Discipline;

import java.util.List;
import java.util.stream.Collectors;

public final class TeacherMapper {

    private TeacherMapper() {
    }

    public static TeacherResponseDto toDto(Teacher teacher) {

        if (teacher == null) {
            return null;
        }

        List<String> disciplineNames = null;

        if (teacher.getDisciplines() != null) {
            disciplineNames = teacher.getDisciplines()
                    .stream()
                    .map(Discipline::getName)
                    .collect(Collectors.toList());
        }

        return new TeacherResponseDto(
                teacher.getId(),
                teacher.getFullName(),
                disciplineNames
        );
    }
}