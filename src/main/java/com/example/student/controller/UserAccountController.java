package com.example.student.controller;

import com.example.student.dto.UserAccountCreateDto;
import com.example.student.dto.UserAccountResponseDto;
import com.example.student.service.UserAccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class UserAccountController {

    private final UserAccountService service;

    public UserAccountController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UserAccountResponseDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<UserAccountResponseDto> create(@Valid @RequestBody UserAccountCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccountResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UserAccountCreateDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
}
