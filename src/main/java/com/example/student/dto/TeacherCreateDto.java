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
    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 50)
    @jakarta.validation.constraints.Pattern(
            regexp = "^[А-Яа-яA-Za-z]+$",
            message = "Имя должно содержать только буквы"
    )
    private String firstName;

    @Schema(description = "Фамилия", example = "Ковальчук")
    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 50)
    @jakarta.validation.constraints.Pattern(
            regexp = "^[А-Яа-яA-Za-z]+$",
            message = "Фамилия должна содержать только буквы"
    )
    private String lastName;

    @Schema(description = "Отчество", example = "Михайловна")
    @jakarta.validation.constraints.Pattern(
            regexp = "^[А-Яа-яA-Za-z]*$",
            message = "Отчество должно содержать только буквы"
    )
    private String middleName;
}