package com.example.student.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GradeResponseDto {

    private Long id;
    private Integer value;
    private String studentName;
    private String disciplineName;
}