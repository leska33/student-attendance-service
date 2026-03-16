package com.example.student.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StudentCreateDto {

    private String firstName;
    private String lastName;
    private String middleName;
    private Long groupId;
    private List<Long> disciplineIds;

}