package com.example.student.service;

import com.example.student.cache.DisciplineQueryKey;
import com.example.student.dto.DisciplineResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.mapper.DisciplineMapper;
import com.example.student.repository.DisciplineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DisciplineQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisciplineQueryService.class);

    private final DisciplineRepository repository;
    private final Map<DisciplineQueryKey, List<DisciplineResponseDto>> cache = new ConcurrentHashMap<>();

    public DisciplineQueryService(DisciplineRepository repository) {
        this.repository = repository;
    }

    public List<DisciplineResponseDto> getDisciplinesByTeacherJPQL(String firstName, String middleName,
                                                                   String lastName, int page, int size) {
        DisciplineQueryKey key = new DisciplineQueryKey(firstName, middleName, lastName, page, size, "JPQL");

        List<DisciplineResponseDto> cached = cache.get(key);
        if (cached != null) {
            LOGGER.info("DISCIPLINE_JPQL: FROM CACHE");
            return cached;
        }

        LOGGER.info("DISCIPLINE_JPQL: FROM DATABASE");

        List<DisciplineResponseDto> result = repository
                .findByTeacherFullNameJPQL(firstName, middleName, lastName, PageRequest.of(page, size))
                .map(DisciplineMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public List<DisciplineResponseDto> getDisciplinesByTeacherNative(String firstName, String middleName,
                                                                     String lastName, int page, int size) {
        DisciplineQueryKey key = new DisciplineQueryKey(firstName, middleName, lastName, page, size, "NATIVE");

        List<DisciplineResponseDto> cached = cache.get(key);
        if (cached != null) {
            LOGGER.info("DISCIPLINE_NATIVE: FROM CACHE");
            return cached;
        }

        LOGGER.info("DISCIPLINE_NATIVE: FROM DATABASE");

        List<DisciplineResponseDto> result = repository
                .findByTeacherFullNameNative(firstName, middleName, lastName, PageRequest.of(page, size))
                .map(DisciplineMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public void invalidateCache() {
        LOGGER.info("DISCIPLINE CACHE CLEARED");
        cache.clear();
    }

    @Transactional
    public Discipline saveOrUpdate(Discipline discipline) {
        Discipline saved = repository.save(discipline);
        invalidateCache();
        return saved;
    }

    @Transactional
    public void delete(Discipline discipline) {
        repository.delete(discipline);
        invalidateCache();
    }
}