package com.example.student.service;

import com.example.student.cache.DisciplineQueryKey;
import com.example.student.dto.DisciplineResponseDto;
import com.example.student.mapper.DisciplineMapper;
import com.example.student.repository.DisciplineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DisciplineQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisciplineQueryService.class);

    private final DisciplineRepository repository;
    private final Map<DisciplineQueryKey, List<DisciplineResponseDto>> cache = new HashMap<>();

    public DisciplineQueryService(DisciplineRepository repository) {
        this.repository = repository;
    }

    public List<DisciplineResponseDto> getDisciplinesByTeacherJPQL(
            String firstName, String middleName, String lastName,
            int page, int size) {

        DisciplineQueryKey key = new DisciplineQueryKey(
                firstName, middleName, lastName, page, size, "JPQL"
        );

        if (cache.containsKey(key)) {
            LOGGER.info("DISCIPLINE_JPQL: FROM CACHE - firstName={}, lastName={}, page={}",
                    firstName, lastName, page);
            return cache.get(key);
        }

        LOGGER.info("DISCIPLINE_JPQL: FROM DATABASE - firstName={}, lastName={}, page={}",
                firstName, lastName, page);

        List<DisciplineResponseDto> result = repository
                .findByTeacherFullNameJPQL(firstName, middleName, lastName, PageRequest.of(page, size))
                .map(DisciplineMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public List<DisciplineResponseDto> getDisciplinesByTeacherNative(
            String firstName, String middleName, String lastName,
            int page, int size) {

        DisciplineQueryKey key = new DisciplineQueryKey(
                firstName, middleName, lastName, page, size, "NATIVE"
        );

        if (cache.containsKey(key)) {
            LOGGER.info("DISCIPLINE_NATIVE: FROM CACHE - firstName={}, lastName={}, page={}",
                    firstName, lastName, page);
            return cache.get(key);
        }

        LOGGER.info("DISCIPLINE_NATIVE: FROM DATABASE - firstName={}, lastName={}, page={}",
                firstName, lastName, page);

        List<DisciplineResponseDto> result = repository
                .findByTeacherFullNameNative(firstName, middleName, lastName, PageRequest.of(page, size))
                .map(DisciplineMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    @Transactional
    public void invalidateCache() {
        LOGGER.info("DISCIPLINE CACHE CLEARED");
        cache.clear();
    }
}