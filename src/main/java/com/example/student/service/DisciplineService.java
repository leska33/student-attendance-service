package com.example.student.service;

import com.example.student.entity.Discipline;
import com.example.student.repository.DisciplineRepository;
import com.example.student.dto.DisciplineResponseDto;
import com.example.student.mapper.DisciplineMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;

    public DisciplineService(DisciplineRepository disciplineRepository) {
        this.disciplineRepository = disciplineRepository;
    }

    public List<DisciplineResponseDto> getAllDisciplinesDtoLazy() {
        return disciplineRepository.findAll()
                .stream()
                .map(DisciplineMapper::toDto)
                .toList();
    }

    public List<DisciplineResponseDto> getAllDisciplinesDtoOptimized() {
        return disciplineRepository.findAllWithRelations()
                .stream()
                .map(DisciplineMapper::toDto)
                .toList();
    }

    @Transactional
    public Discipline createDiscipline(Discipline discipline) {
        return disciplineRepository.save(discipline);
    }

    @Transactional
    public void deleteDiscipline(Long id) {
        disciplineRepository.deleteById(id);
    }
}