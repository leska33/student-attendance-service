package com.example.student.exception.handler;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {

    private String message;
    private String error;
    private int status;
    private String imageUrl;
    private LocalDateTime timestamp;
}