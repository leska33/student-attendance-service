package com.example.student.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StudentResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String groupNumber;
    private List<String> disciplines;
}