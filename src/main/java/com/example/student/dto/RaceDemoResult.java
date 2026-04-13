package com.example.student.dto;

public record RaceDemoResult(
        int threads,
        int incrementsPerThread,
        long expectedTotal,
        long unsafeCounterResult,
        long atomicCounterResult
) {
}
