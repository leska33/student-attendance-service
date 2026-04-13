package com.example.student.dto;

public record ThreadSafeCounterDemoResult(
        int threads,
        int incrementsPerThread,
        long expectedTotal,
        long threadSafeCounterResult
) {
}
