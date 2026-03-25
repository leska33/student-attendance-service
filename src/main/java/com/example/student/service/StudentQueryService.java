package com.example.student.service;

import com.example.student.cache.StudentQueryKey;
import com.example.student.dto.StudentResponseDto;
import com.example.student.entity.Student;
import com.example.student.mapper.StudentMapper;
import com.example.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StudentQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentQueryService.class);

    private final StudentRepository repository;
    private final Map<StudentQueryKey, List<StudentResponseDto>> cache = new ConcurrentHashMap<>();

    public StudentQueryService(StudentRepository repository) {
        this.repository = repository;
    }

    public List<StudentResponseDto> getStudentsByDisciplineJPQL(String disciplineName, int page, int size) {
        StudentQueryKey key = new StudentQueryKey(disciplineName, page, size, "JPQL");

        List<StudentResponseDto> cached = cache.get(key);
        if (cached != null) {
            LOGGER.info("STUDENT_JPQL: FROM CACHE");
            return cached;
        }

        LOGGER.info("STUDENT_JPQL: FROM DATABASE");

        List<StudentResponseDto> result = repository
                .findByDisciplineNameJPQL(disciplineName, PageRequest.of(page, size))
                .map(StudentMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public List<StudentResponseDto> getStudentsByDisciplineNative(String disciplineName, int page, int size) {
        StudentQueryKey key = new StudentQueryKey(disciplineName, page, size, "NATIVE");

        List<StudentResponseDto> cached = cache.get(key);
        if (cached != null) {
            LOGGER.info("STUDENT_NATIVE: FROM CACHE");
            return cached;
        }

        LOGGER.info("STUDENT_NATIVE: FROM DATABASE");

        List<StudentResponseDto> result = repository
                .findByDisciplineNameNative(disciplineName, PageRequest.of(page, size))
                .map(StudentMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public void invalidateCache() {
        LOGGER.info("STUDENT CACHE CLEARED");
        cache.clear();
    }

    @Transactional
    public Student saveOrUpdate(Student student) {
        Student saved = repository.save(student);
        invalidateCache();
        return saved;
    }

    @Transactional
    public void delete(Student student) {
        repository.delete(student);
        invalidateCache();
    }
}