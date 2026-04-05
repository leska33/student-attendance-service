package com.example.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "DTO для создания преподавателя")
public class TeacherCreateDto {

    @Schema(description = "Имя", example = "Анна")
    @NotBlank
    @Size(min = 2, max = 50)
    private String firstName;

    @Schema(description = "Фамилия", example = "Ковальчук")
    @NotBlank
    @Size(min = 2, max = 50)
    private String lastName;

    @Schema(description = "Отчество", example = "Михайловна")
    private String middleName;
}