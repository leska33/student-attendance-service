package com.example.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Schema(description = "DTO для создания студента")
@Getter
@Setter
public class StudentCreateDto {

    @Schema(description = "Имя", example = "Алеся")
    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 50)
    @jakarta.validation.constraints.Pattern(
            regexp = "^[А-Яа-яA-Za-z]+$",
            message = "Имя должно содержать только буквы"
    )
    private String firstName;

    @Schema(description = "Фамилия", example = "Басюк")
    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 50)
    @jakarta.validation.constraints.Pattern(
            regexp = "^[А-Яа-яA-Za-z]+$",
            message = "Фамилия должна содержать только буквы"
    )
    private String lastName;

    @Schema(description = "Отчество", example = "Владимировна")
    @jakarta.validation.constraints.Pattern(
            regexp = "^[А-Яа-яA-Za-z]*$",
            message = "Отчество должно содержать только буквы"
    )
    private String middleName;

    @Schema(description = "ID группы", example = "1")
    @NotNull(message = "groupId обязателен")
    private Long groupId;

    @Schema(description = "ID дисциплин", example = "[1,2,3]")
    @NotEmpty(message = "Список дисциплин не должен быть пустым")
    private List<Long> disciplineIds;
}