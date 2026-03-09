package com.example.student.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DisciplineResponseDto {

    private Long id;
    private String name;
    private String teacherName;
    private List<String> students;
}