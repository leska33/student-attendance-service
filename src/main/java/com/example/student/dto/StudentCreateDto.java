package com.example.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "DTO для создания студента")
public class StudentCreateDto extends BasePersonDto {

    private Long groupId;

    private List<Long> disciplineIds;
}