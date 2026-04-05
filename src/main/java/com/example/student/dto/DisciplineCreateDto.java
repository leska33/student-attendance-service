package com.example.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "DTO для создания дисциплины")
public class DisciplineCreateDto {

    @Schema(description = "Название дисциплины", example = "Математика")
    @NotBlank(message = "Название дисциплины обязательно")
    @Size(min = 2, max = 100)
    private String name;

    @Schema(description = "ID преподавателя", example = "1")
    @NotNull(message = "teacherId обязателен")
    private Long teacherId;
}