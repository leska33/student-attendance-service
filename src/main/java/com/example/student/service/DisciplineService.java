package com.example.student.service;

import com.example.student.aop.LogExecutionTime;
import com.example.student.dto.DisciplineCreateDto;
import com.example.student.dto.DisciplineResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Student;
import com.example.student.entity.Teacher;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.DisciplineMapper;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DisciplineService {

    private static final String DISCIPLINE_NOT_FOUND = "Discipline not found";
    private static final String TEACHER_NOT_FOUND = "Teacher not found";

    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;
    private final GradeRepository gradeRepository;

    public DisciplineService(DisciplineRepository disciplineRepository,
                             TeacherRepository teacherRepository,
                             GradeRepository gradeRepository) {
        this.disciplineRepository = disciplineRepository;
        this.teacherRepository = teacherRepository;
        this.gradeRepository = gradeRepository;
    }

    @LogExecutionTime
    public List<DisciplineResponseDto> getAllDisciplinesDtoLazy() {
        return disciplineRepository.findAll()
                .stream()
                .map(DisciplineMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public List<DisciplineResponseDto> getAllDisciplinesDtoOptimized() {
        return disciplineRepository.findAllWithRelations()
                .stream()
                .map(DisciplineMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public DisciplineResponseDto getDisciplineById(Long id) {
        Discipline discipline = findDisciplineOrThrow(id);
        return DisciplineMapper.toDto(discipline);
    }

    @Transactional
    @LogExecutionTime
    public DisciplineResponseDto createDiscipline(DisciplineCreateDto dto) {
        Discipline discipline = buildDiscipline(dto);
        return DisciplineMapper.toDto(disciplineRepository.save(discipline));
    }

    @Transactional
    public List<DisciplineResponseDto> createDisciplinesBulk(List<DisciplineCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .peek(this::assertBulkDisciplineRowValid)
                .map(this::createDisciplineEntity)
                .map(disciplineRepository::save)
                .map(DisciplineMapper::toDto)
                .toList();
    }

    public List<DisciplineResponseDto> createDisciplinesBulkWithoutTransaction(List<DisciplineCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .peek(this::assertBulkDisciplineRowValid)
                .map(this::createDisciplineEntity)
                .map(disciplineRepository::save)
                .map(DisciplineMapper::toDto)
                .toList();
    }

    @Transactional
    @LogExecutionTime
    public DisciplineResponseDto updateDiscipline(Long id, DisciplineCreateDto dto) {
        Discipline discipline = findDisciplineOrThrow(id);

        discipline.setName(dto.getName());
        discipline.setTeacher(findTeacherOrThrow(dto.getTeacherId()));

        return DisciplineMapper.toDto(disciplineRepository.save(discipline));
    }

    @Transactional
    @LogExecutionTime
    public void deleteDiscipline(Long id) {
        Discipline discipline = findDisciplineOrThrow(id);

        gradeRepository.deleteAllByDiscipline(discipline);

        if (discipline.getStudents() != null) {
            for (Student student : discipline.getStudents()) {
                student.getDisciplines().remove(discipline);
            }
            discipline.getStudents().clear();
        }

        if (discipline.getTeacher() != null) {
            discipline.getTeacher().getDisciplines().remove(discipline);
        }

        disciplineRepository.delete(discipline);
    }

    private Discipline findDisciplineOrThrow(Long id) {
        return Optional.ofNullable(id)
                .flatMap(disciplineRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException(DISCIPLINE_NOT_FOUND));
    }

    private Teacher findTeacherOrThrow(Long id) {
        return Optional.ofNullable(id)
                .flatMap(teacherRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_NOT_FOUND));
    }

    private void assertBulkDisciplineRowValid(DisciplineCreateDto dto) {
        if ("ERROR".equals(dto.getName())) {
            throw new IllegalStateException("Ошибка в bulk операции");
        }
    }

    private Discipline buildDiscipline(DisciplineCreateDto dto) {
        Discipline discipline = new Discipline();
        discipline.setName(dto.getName());
        discipline.setTeacher(findTeacherOrThrow(dto.getTeacherId()));
        return discipline;
    }

    private Discipline createDisciplineEntity(DisciplineCreateDto dto) {
        return buildDiscipline(dto);
    }
}