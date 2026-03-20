package com.example.student.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeCreateDto {

    @NotNull(message = "Оценка обязательна")
    @Min(value = 0, message = "Минимальная оценка 1")
    @Max(value = 10, message = "Максимальная оценка 10")
    private Integer value;

    @NotNull(message = "studentId обязателен")
    private Long studentId;

    @NotNull(message = "disciplineId обязателен")
    private Long disciplineId;
}