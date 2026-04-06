package com.example.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "DTO для создания преподавателя")
public class TeacherCreateDto extends BasePersonDto {
}