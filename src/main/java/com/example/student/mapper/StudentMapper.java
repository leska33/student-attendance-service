package com.example.student.mapper;

import com.example.student.dto.StudentResponseDto;
import com.example.student.entity.Student;
import com.example.student.entity.Discipline;

import java.util.List;
import java.util.stream.Collectors;

public final class StudentMapper {

    private StudentMapper() {
    }

    public static StudentResponseDto toDto(Student student) {

        if (student == null) {
            return null;
        }

        String groupNumber = null;

        if (student.getGroup() != null) {
            groupNumber = student.getGroup().getGroupNumber();
        }

        List<String> disciplineNames = null;

        if (student.getDisciplines() != null) {
            disciplineNames = student.getDisciplines()
                    .stream()
                    .map(Discipline::getName)
                    .collect(Collectors.toList());
        }
        return new StudentResponseDto(
                student.getId(),
                student.getFullName(),
                groupNumber,
                disciplineNames
        );
    }
}