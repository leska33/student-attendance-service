package com.example.student.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExecutionTimeAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionTimeAspect.class);

    @Around("execution(* com.example.student.service..*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            if (LOGGER.isInfoEnabled()) {
                long executionTime = System.currentTimeMillis() - start;
                LOGGER.info("время выполнения: {} мс | {}",
                        executionTime,
                        joinPoint.getSignature().toShortString());
            }
        }
    }
}