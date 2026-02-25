package com.example.student.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentResponseDto {

    private final String studentId;
    private final String fullName;
    private final String groupNumber;
    private final int attendanceCount;
    private final double averageGrade;
}