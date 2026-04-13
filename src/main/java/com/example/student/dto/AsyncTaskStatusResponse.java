package com.example.student.dto;

public record AsyncTaskStatusResponse(String taskId, AsyncTaskStatus status, String detail) {
}
