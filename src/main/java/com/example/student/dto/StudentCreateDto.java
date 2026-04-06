package com.example.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "DTO для создания студента")
public class StudentCreateDto extends BasePersonDto {

    @NotNull(message = "groupId обязателен")
    private Long groupId;

    @NotEmpty(message = "Список дисциплин не должен быть пустым")
    private List<Long> disciplineIds;
}