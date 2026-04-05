package com.example.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "DTO для создания оценки")
public class GradeCreateDto {

    @Schema(description = "Оценка", example = "8")
    @NotNull
    @Min(0)
    @Max(10)
    private Integer value;

    @Schema(description = "ID студента", example = "1")
    @NotNull
    private Long studentId;

    @Schema(description = "ID дисциплины", example = "1")
    @NotNull
    private Long disciplineId;
}