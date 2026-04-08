package com.example.student.service;

import com.example.student.aop.LogExecutionTime;
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
import java.util.Optional;

@Service
public class GradeService {

    private static final String GRADE_NOT_FOUND = "Grade not found";
    private static final String STUDENT_NOT_FOUND = "Student not found";
    private static final String DISCIPLINE_NOT_FOUND = "Discipline not found";

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

    @LogExecutionTime
    public List<GradeResponseDto> getAllGradesDtoLazy() {
        return gradeRepository.findAll().stream()
                .map(GradeMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public List<GradeResponseDto> getAllGradesDtoOptimized() {
        return gradeRepository.findAllWithRelations().stream()
                .map(GradeMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public GradeResponseDto getGradeById(Long id) {
        return GradeMapper.toDto(findGradeOrThrow(id));
    }

    @Transactional
    @LogExecutionTime
    public GradeResponseDto createGrade(GradeCreateDto dto) {
        Grade grade = buildGrade(dto);
        return GradeMapper.toDto(gradeRepository.save(grade));
    }

    @Transactional
    public List<GradeResponseDto> createGradesBulk(List<GradeCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .peek(this::assertBulkGradeRowValid)
                .map(this::createGradeEntity)
                .map(gradeRepository::save)
                .map(GradeMapper::toDto)
                .toList();
    }

    public List<GradeResponseDto> createGradesBulkWithoutTransaction(List<GradeCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .peek(this::assertBulkGradeRowValid)
                .map(this::createGradeEntity)
                .map(gradeRepository::save)
                .map(GradeMapper::toDto)
                .toList();
    }

    @Transactional
    @LogExecutionTime
    public GradeResponseDto updateGrade(Long id, GradeCreateDto dto) {
        Grade grade = findGradeOrThrow(id);
        grade.setValue(dto.getValue());
        return GradeMapper.toDto(gradeRepository.save(grade));
    }

    @Transactional
    @LogExecutionTime
    public void deleteGrade(Long id) {
        gradeRepository.delete(findGradeOrThrow(id));
    }

    private Grade findGradeOrThrow(Long id) {
        return Optional.ofNullable(id)
                .flatMap(gradeRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException(GRADE_NOT_FOUND));
    }

    private Student findStudentOrThrow(Long id) {
        return Optional.ofNullable(id)
                .flatMap(studentRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
    }

    private Discipline findDisciplineOrThrow(Long id) {
        return Optional.ofNullable(id)
                .flatMap(disciplineRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException(DISCIPLINE_NOT_FOUND));
    }

    private void assertBulkGradeRowValid(GradeCreateDto dto) {
        if (dto.getValue() != null && dto.getValue() < 0) {
            throw new IllegalStateException("Некорректная оценка");
        }
    }

    private Grade buildGrade(GradeCreateDto dto) {
        Grade grade = new Grade();
        grade.setValue(dto.getValue());
        grade.setStudent(findStudentOrThrow(dto.getStudentId()));
        grade.setDiscipline(findDisciplineOrThrow(dto.getDisciplineId()));
        return grade;
    }

    private Grade createGradeEntity(GradeCreateDto dto) {
        return buildGrade(dto);
    }
}