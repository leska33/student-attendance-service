package com.example.student.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ThreadSafeCounter {

    private final AtomicLong value = new AtomicLong(0L);

    public long incrementAndGet() {
        return value.incrementAndGet();
    }

    public long get() {
        return value.get();
    }

    public void reset() {
        value.set(0L);
    }
}
