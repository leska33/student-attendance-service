package com.example.student.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TeacherResponseDto {

    private Long id;
    private String fullName;
    private List<String> disciplines;
}