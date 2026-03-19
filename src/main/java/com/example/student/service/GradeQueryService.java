package com.example.student.service;

import com.example.student.cache.GradeQueryKey;
import com.example.student.dto.GradeResponseDto;
import com.example.student.mapper.GradeMapper;
import com.example.student.repository.GradeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GradeQueryService {

    private final GradeRepository repository;
    private final Map<GradeQueryKey, List<GradeResponseDto>> cache = new HashMap<>();

    public GradeQueryService(GradeRepository repository) {
        this.repository = repository;
    }

    public List<GradeResponseDto> getGradesByStudentAndDisciplineJPQL(
            String studentLastName, String disciplineName, int page, int size) {

        GradeQueryKey key = new GradeQueryKey(studentLastName, disciplineName, page, size);

        if (cache.containsKey(key)) return cache.get(key);

        List<GradeResponseDto> result = repository
                .findByStudentLastNameJPQL(studentLastName, PageRequest.of(page, size))
                .stream()
                .filter(g -> g.getDiscipline().getName().equals(disciplineName))
                .map(GradeMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public List<GradeResponseDto> getGradesByStudentAndDisciplineNative(
            String studentLastName, String disciplineName, int page, int size) {

        GradeQueryKey key = new GradeQueryKey(studentLastName, disciplineName, page, size);

        if (cache.containsKey(key)) return cache.get(key);

        List<GradeResponseDto> result = repository
                .findByStudentLastNameNative(studentLastName, PageRequest.of(page, size))
                .stream()
                .filter(g -> g.getDiscipline().getName().equals(disciplineName))
                .map(GradeMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    @Transactional
    public void invalidateCache() {
        cache.clear();
    }
}