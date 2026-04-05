package com.example.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "DTO для создания студента")
public class StudentCreateDto {

    @Schema(description = "Имя", example = "Алеся")
    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 50)
    private String firstName;

    @Schema(description = "Фамилия", example = "Басюк")
    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 50)
    private String lastName;

    @Schema(description = "Отчество", example = "Владимировна")
    private String middleName;

    @Schema(description = "ID группы", example = "1")
    @NotNull(message = "groupId обязателен")
    private Long groupId;

    @Schema(description = "ID дисциплин", example = "[1,2,3]")
    @NotEmpty(message = "Список дисциплин не должен быть пустым")
    private List<Long> disciplineIds;
}