package com.example.student.service;

import com.example.student.aop.LogExecutionTime;
import com.example.student.dto.TeacherCreateDto;
import com.example.student.dto.TeacherResponseDto;
import com.example.student.entity.Teacher;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.TeacherMapper;
import com.example.student.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TeacherService {

    private static final String TEACHER_NOT_FOUND = "Teacher not found";
    private static final String TEACHER_ALREADY_EXISTS = "Teacher already exists";

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @LogExecutionTime
    public List<TeacherResponseDto> getAllTeachersDtoLazy() {
        return teacherRepository.findAll()
                .stream()
                .map(TeacherMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public List<TeacherResponseDto> getAllTeachersDtoOptimized() {
        return teacherRepository.findAllWithDisciplines()
                .stream()
                .map(TeacherMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public TeacherResponseDto getTeacherById(Long id) {
        return TeacherMapper.toDto(findTeacherOrThrow(id));
    }

    @Transactional
    @LogExecutionTime
    public TeacherResponseDto createTeacher(TeacherCreateDto dto) {
        validateTeacherUniqueness(dto);

        Teacher teacher = new Teacher();
        mapTeacher(teacher, dto);

        return TeacherMapper.toDto(teacherRepository.save(teacher));
    }

    @Transactional
    public List<TeacherResponseDto> createTeachersBulk(List<TeacherCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .map(this::validateBulkTeacherRow)
                .map(this::createTeacherEntity)
                .map(teacherRepository::save)
                .map(TeacherMapper::toDto)
                .toList();
    }

    public List<TeacherResponseDto> createTeachersBulkWithoutTransaction(List<TeacherCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .map(this::validateBulkTeacherRow)
                .map(this::createTeacherEntity)
                .map(teacherRepository::save)
                .map(TeacherMapper::toDto)
                .toList();
    }

    @Transactional
    @LogExecutionTime
    public TeacherResponseDto updateTeacher(Long id, TeacherCreateDto dto) {
        Teacher teacher = findTeacherOrThrow(id);
        mapTeacher(teacher, dto);
        return TeacherMapper.toDto(teacherRepository.save(teacher));
    }

    @Transactional
    @LogExecutionTime
    public void deleteTeacher(Long id) {
        teacherRepository.delete(findTeacherOrThrow(id));
    }

    private Teacher findTeacherOrThrow(Long id) {
        return Optional.ofNullable(id)
                .flatMap(teacherRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_NOT_FOUND));
    }

    private TeacherCreateDto validateBulkTeacherRow(TeacherCreateDto dto) {
        if (BulkOperationConstants.ERROR_SENTINEL.equals(dto.getFirstName())) {
            throw new IllegalStateException(BulkOperationConstants.MSG_BULK_TEACHER);
        }
        return dto;
    }

    private void validateTeacherUniqueness(TeacherCreateDto dto) {
        if (teacherRepository.existsByFirstNameAndLastNameAndMiddleName(
                dto.getFirstName(),
                dto.getLastName(),
                dto.getMiddleName()
        )) {
            throw new AlreadyExistsException(TEACHER_ALREADY_EXISTS);
        }
    }

    private Teacher createTeacherEntity(TeacherCreateDto dto) {
        validateTeacherUniqueness(dto);
        Teacher teacher = new Teacher();
        mapTeacher(teacher, dto);
        return teacher;
    }

    private void mapTeacher(Teacher teacher, TeacherCreateDto dto) {
        teacher.setFirstName(dto.getFirstName());
        teacher.setLastName(dto.getLastName());
        teacher.setMiddleName(dto.getMiddleName());
    }
}