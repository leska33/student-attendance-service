package com.example.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupCreateDto {

    @NotBlank(message = "Номер группы обязателен")
    private String number;
}