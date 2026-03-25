package com.example.student.service;

import com.example.student.cache.DisciplineQueryKey;
import com.example.student.dto.DisciplineResponseDto;
import com.example.student.mapper.DisciplineMapper;
import com.example.student.repository.DisciplineRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DisciplineQueryService {

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
            System.out.println("DISCIPLINE_JPQL: FROM CACHE - firstName=" + firstName + ", " +
                    "lastName=" + lastName + ", page=" + page);
            return cache.get(key);
        }

        System.out.println("DISCIPLINE_JPQL: FROM DATABASE - firstName=" + firstName + ", " +
                "lastName=" + lastName + ", page=" + page);
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
            System.out.println("DISCIPLINE_NATIVE: FROM CACHE - firstName=" + firstName + ", " +
                    "lastName=" + lastName + ", page=" + page);
            return cache.get(key);
        }

        System.out.println("DISCIPLINE_NATIVE: FROM DATABASE - firstName=" + firstName + ", " +
                "lastName=" + lastName + ", page=" + page);
        List<DisciplineResponseDto> result = repository
                .findByTeacherFullNameNative(firstName, middleName, lastName, PageRequest.of(page, size))
                .map(DisciplineMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    @Transactional
    public void invalidateCache() {
        System.out.println("DISCIPLINE: CACHE INVALIDATED - size before=" + cache.size());
        cache.clear();
        System.out.println("DISCIPLINE: CACHE INVALIDATED - size after=" + cache.size());
    }
}