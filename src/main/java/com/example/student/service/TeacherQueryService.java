package com.example.student.service;

import com.example.student.cache.TeacherQueryKey;
import com.example.student.dto.TeacherResponseDto;
import com.example.student.mapper.TeacherMapper;
import com.example.student.repository.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeacherQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherQueryService.class);

    private final TeacherRepository repository;
    private final Map<TeacherQueryKey, List<TeacherResponseDto>> cache = new HashMap<>();

    public TeacherQueryService(TeacherRepository repository) {
        this.repository = repository;
    }

    public List<TeacherResponseDto> getTeachersByDisciplineJPQL(String disciplineName, int page, int size) {
        TeacherQueryKey key = new TeacherQueryKey(disciplineName, page, size, "JPQL");

        if (cache.containsKey(key)) {
            LOGGER.info("TEACHER_JPQL: FROM CACHE - discipline={}, page={}", disciplineName, page);
            return cache.get(key);
        }

        LOGGER.info("TEACHER_JPQL: FROM DATABASE - discipline={}, page={}", disciplineName, page);

        List<TeacherResponseDto> result = repository
                .findByDisciplineNameJPQL(disciplineName, PageRequest.of(page, size))
                .map(TeacherMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public List<TeacherResponseDto> getTeachersByDisciplineNative(String disciplineName, int page, int size) {
        TeacherQueryKey key = new TeacherQueryKey(disciplineName, page, size, "NATIVE");

        if (cache.containsKey(key)) {
            LOGGER.info("TEACHER_NATIVE: FROM CACHE - discipline={}, page={}", disciplineName, page);
            return cache.get(key);
        }

        LOGGER.info("TEACHER_NATIVE: FROM DATABASE - discipline={}, page={}", disciplineName, page);

        List<TeacherResponseDto> result = repository
                .findByDisciplineNameNative(disciplineName, PageRequest.of(page, size))
                .map(TeacherMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    @Transactional
    public void invalidateCache() {
        LOGGER.info("TEACHER CACHE CLEARED");
        cache.clear();
    }
}