package com.example.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupCreateDto {

    @NotBlank(message = "Номер группы обязателен")
    @Size(min = 2, max = 7, message = "Номер группы должен быть от 2 до 7 символов")
    private String number;
}