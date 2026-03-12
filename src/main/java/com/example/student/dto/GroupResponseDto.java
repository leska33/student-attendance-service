package com.example.student.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GroupResponseDto {

    private Long id;
    private String Number;
    private List<String> students;
}