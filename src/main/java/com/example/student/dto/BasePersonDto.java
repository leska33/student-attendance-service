package com.example.student.dto;

import com.example.student.validation.ValidName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BasePersonDto {

    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 50)
    @ValidName
    protected String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 50)
    @ValidName
    protected String lastName;

    protected String middleName;
}