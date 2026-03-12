package com.example.student.mapper;

import com.example.student.dto.StudentResponseDto;
import com.example.student.entity.Student;
import com.example.student.entity.Discipline;

import java.util.List;
import java.util.stream.Collectors;

public final class StudentMapper {

    private StudentMapper() {}

    public static StudentResponseDto toDto(Student student) {

        if (student == null) {
            return null;
        }

        List<String> disciplineNames = null;
        if (student.getDisciplines() != null) {
            disciplineNames = student.getDisciplines()
                    .stream()
                    .map(Discipline::getName)
                    .collect(Collectors.toList());
        }
        Long groupId = null;
        if (student.getGroup() != null) {
            groupId = student.getGroup().getId();
        }

        return new StudentResponseDto(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getMiddleName(),
                groupId,
                disciplineNames
        );
    }
}