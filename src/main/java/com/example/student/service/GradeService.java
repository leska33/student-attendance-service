package com.example.student.service;

import com.example.student.dto.GradeCreateDto;
import com.example.student.dto.GradeResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Grade;
import com.example.student.entity.Student;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.GradeMapper;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.StudentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final DisciplineRepository disciplineRepository;

    public GradeService(GradeRepository gradeRepository,
                        StudentRepository studentRepository,
                        DisciplineRepository disciplineRepository) {
        this.gradeRepository = gradeRepository;
        this.studentRepository = studentRepository;
        this.disciplineRepository = disciplineRepository;
    }

    public List<GradeResponseDto> getAllGradesDtoLazy() {
        return gradeRepository.findAll()
                .stream()
                .map(GradeMapper::toDto)
                .toList();
    }

    public List<GradeResponseDto> getAllGradesDtoOptimized() {
        return gradeRepository.findAllWithRelations()
                .stream()
                .map(GradeMapper::toDto)
                .toList();
    }

    public GradeResponseDto getGradeById(Long id) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade not found"));
        return GradeMapper.toDto(grade);
    }

    @Transactional
    public GradeResponseDto createGrade(GradeCreateDto dto) {
        Grade grade = new Grade();
        grade.setValue(dto.getValue());
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Discipline discipline = disciplineRepository.findById(dto.getDisciplineId())
                .orElseThrow(() -> new ResourceNotFoundException("Discipline not found"));
        grade.setStudent(student);
        grade.setDiscipline(discipline);
        Grade saved = gradeRepository.save(grade);
        return GradeMapper.toDto(saved);
    }

    @Transactional
    public GradeResponseDto updateGrade(Long id, GradeCreateDto dto) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade not found"));
        grade.setValue(dto.getValue());
        if (dto.getStudentId() != null) {
            Student student = studentRepository.findById(dto.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            grade.setStudent(student);
        }
        if (dto.getDisciplineId() != null) {
            Discipline discipline = disciplineRepository.findById(dto.getDisciplineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Discipline not found"));
            grade.setDiscipline(discipline);
        }
        Grade updated = gradeRepository.save(grade);
        return GradeMapper.toDto(updated);
    }

    @Transactional
    public void deleteGrade(Long id) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade not found"));
        gradeRepository.delete(grade);
    }
}