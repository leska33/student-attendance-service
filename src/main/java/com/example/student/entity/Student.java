package com.example.student.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Student {

    private String studentId;
    private String fullName;
    private String groupNumber;
    private int attendanceCount;
    private double averageGrade;
}