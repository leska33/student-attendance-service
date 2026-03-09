package com.example.student.service;

import com.example.student.entity.Grade;
import com.example.student.repository.GradeRepository;
import com.example.student.dto.GradeResponseDto;
import com.example.student.mapper.GradeMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;

    public GradeService(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    public List<GradeResponseDto> getAllGradesDtoLazy() {
        return gradeRepository.findAll()
                .stream()
                .map(GradeMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<GradeResponseDto> getAllGradesDtoOptimized() {
        return gradeRepository.findAllWithRelations()
                .stream()
                .map(GradeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Grade createGrade(Grade grade) {
        return gradeRepository.save(grade);
    }

    @Transactional
    public void deleteGrade(Long id) {
        gradeRepository.deleteById(id);
    }
}