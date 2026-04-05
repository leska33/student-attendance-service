package com.example.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "DTO для создания группы")
public class GroupCreateDto {

    @Schema(description = "Номер группы", example = "450502")
    @NotBlank
    @Size(min = 2, max = 7)
    private String number;
}