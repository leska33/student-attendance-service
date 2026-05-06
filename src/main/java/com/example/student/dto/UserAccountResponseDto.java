package com.example.student.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserAccountResponseDto {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String password;
    private String birthDate;
}
