package com.example.student.dto;

import com.example.student.validation.ValidName;
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
    @ValidName
    private String firstName;

    @Schema(description = "Фамилия", example = "Ковальчук")
    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 50)
    @ValidName
    private String lastName;

    @Schema(description = "Отчество", example = "Михайловна")
    private String middleName;
}