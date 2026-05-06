package com.example.student.mapper;

import com.example.student.dto.UserAccountResponseDto;
import com.example.student.entity.UserAccount;

public final class UserAccountMapper {

    private UserAccountMapper() {
    }

    public static UserAccountResponseDto toDto(UserAccount account) {
        return new UserAccountResponseDto(
                account.getId(),
                account.getFullName(),
                account.getPhone(),
                account.getEmail(),
                account.getPassword(),
                account.getBirthDate()
        );
    }
}
