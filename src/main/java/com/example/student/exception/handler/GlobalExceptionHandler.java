package com.example.student.exception.handler;

import com.example.student.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String NOT_FOUND_IMAGE =
            "https://img.freepik.com/premium-photo/page-found-404-design-404-error-web-page-concept-minimal-style-3d-rendering_554821-1754.jpg?semt=ais_hybrid&w=740";

    private static final String BAD_REQUEST_IMAGE =
            "https://i.pinimg.com/originals/f0/62/a9/f062a90c879b72716c6b4a19b9de2171.png";

    private static final String INTERNAL_ERROR_IMAGE =
            "https://reklama.tochka.com/reklama/blog/images/tild6435-3235-4134-b138-316366316132__7-oshibka_1.png";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("NOT_FOUND")
                .status(HttpStatus.NOT_FOUND.value())
                .imageUrl(NOT_FOUND_IMAGE)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation error");

        ErrorResponse response = ErrorResponse.builder()
                .message(message)
                .error("VALIDATION_ERROR")
                .status(HttpStatus.BAD_REQUEST.value())
                .imageUrl(BAD_REQUEST_IMAGE)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobal(Exception ex) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse response = ErrorResponse.builder()
                .message("Internal server error")
                .error("INTERNAL_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .imageUrl(INTERNAL_ERROR_IMAGE)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}