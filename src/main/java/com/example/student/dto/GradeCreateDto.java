package com.example.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeCreateDto {

    private Integer value;
    private Long studentId;
    private Long disciplineId;

}