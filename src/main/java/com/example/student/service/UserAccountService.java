package com.example.student.service;

import com.example.student.dto.UserAccountCreateDto;
import com.example.student.dto.UserAccountResponseDto;
import com.example.student.entity.UserAccount;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.UserAccountMapper;
import com.example.student.repository.UserAccountRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private static final String ACCOUNT_NOT_FOUND = "User account not found";
    private static final String EMAIL_ALREADY_EXISTS = "Email already exists";

    private final UserAccountRepository repository;

    public UserAccountService(UserAccountRepository repository) {
        this.repository = repository;
    }

    public List<UserAccountResponseDto> getAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getId))
                .map(UserAccountMapper::toDto)
                .toList();
    }

    @Transactional
    public UserAccountResponseDto create(UserAccountCreateDto dto) {
        if (repository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new AlreadyExistsException(EMAIL_ALREADY_EXISTS);
        }
        UserAccount account = new UserAccount();
        map(account, dto);
        return UserAccountMapper.toDto(repository.save(account));
    }

    @Transactional
    public UserAccountResponseDto update(Long id, UserAccountCreateDto dto) {
        UserAccount account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND));

        String incomingEmail = dto.getEmail().trim();
        if (!account.getEmail().equalsIgnoreCase(incomingEmail)
                && repository.existsByEmailIgnoreCase(incomingEmail)) {
            throw new AlreadyExistsException(EMAIL_ALREADY_EXISTS);
        }

        map(account, dto);
        return UserAccountMapper.toDto(repository.save(account));
    }

    private void map(UserAccount account, UserAccountCreateDto dto) {
        account.setFullName(dto.getFullName().trim());
        account.setPhone(dto.getPhone().trim());
        account.setEmail(dto.getEmail().trim());
        account.setPassword(dto.getPassword());
        account.setBirthDate(dto.getBirthDate());
    }
}
