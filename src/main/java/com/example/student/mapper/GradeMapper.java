package com.example.student.mapper;

import com.example.student.dto.GradeResponseDto;
import com.example.student.entity.Grade;

public final class GradeMapper {

    private GradeMapper() {
    }

    public static GradeResponseDto toDto(Grade grade) {

        if (grade == null) {
            return null;
        }

        String studentName = null;
        String disciplineName = null;

        if (grade.getStudent() != null) {
            studentName = grade.getStudent().getFullName();
        }

        if (grade.getDiscipline() != null) {
            disciplineName = grade.getDiscipline().getName();
        }

        return new GradeResponseDto(
                grade.getId(),
                grade.getValue(),
                studentName,
                disciplineName
        );
    }
}