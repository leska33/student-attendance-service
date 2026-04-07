package com.example.student.service;

import com.example.student.aop.LogExecutionTime;
import com.example.student.dto.TeacherCreateDto;
import com.example.student.dto.TeacherResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Teacher;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.TeacherMapper;
import com.example.student.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_NOT_FOUND));
        return TeacherMapper.toDto(teacher);
    }

    @Transactional
    @LogExecutionTime
    public TeacherResponseDto createTeacher(TeacherCreateDto dto) {
        if (teacherRepository.existsByFirstNameAndLastNameAndMiddleName(
                dto.getFirstName(),
                dto.getLastName(),
                dto.getMiddleName()
        )) {
            throw new AlreadyExistsException(TEACHER_ALREADY_EXISTS);
        }

        Teacher teacher = new Teacher();
        mapTeacher(teacher, dto);

        return TeacherMapper.toDto(teacherRepository.save(teacher));
    }

    @Transactional
    public List<TeacherResponseDto> createTeachersBulk(List<TeacherCreateDto> dtos) {

        return dtos.stream()
                .map(dto -> {
                    if (teacherRepository.existsByFirstNameAndLastNameAndMiddleName(
                            dto.getFirstName(),
                            dto.getLastName(),
                            dto.getMiddleName()
                    )) {
                        throw new AlreadyExistsException("Teacher already exists");
                    }

                    Teacher teacher = new Teacher();
                    teacher.setFirstName(dto.getFirstName());
                    teacher.setLastName(dto.getLastName());
                    teacher.setMiddleName(dto.getMiddleName());

                    return teacherRepository.save(teacher);
                })
                .map(TeacherMapper::toDto)
                .toList();
    }
    public List<TeacherResponseDto> createTeachersBulkWithoutTransaction(List<TeacherCreateDto> dtos) {

        return dtos.stream()
                .map(dto -> {
                    Teacher teacher = new Teacher();
                    teacher.setFirstName(dto.getFirstName());

                    if ("ERROR".equals(dto.getFirstName())) {
                        throw new RuntimeException("Ошибка в bulk");
                    }

                    return teacherRepository.save(teacher);
                })
                .map(TeacherMapper::toDto)
                .toList();
    }

    @Transactional
    @LogExecutionTime
    public TeacherResponseDto updateTeacher(Long id, TeacherCreateDto dto) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_NOT_FOUND));
        mapTeacher(teacher, dto);

        return TeacherMapper.toDto(teacherRepository.save(teacher));
    }

    @Transactional
    @LogExecutionTime
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_NOT_FOUND));

        if (teacher.getDisciplines() != null) {
            for (Discipline discipline : teacher.getDisciplines()) {
                discipline.setTeacher(null);
            }
            teacher.getDisciplines().clear();
        }

        teacherRepository.delete(teacher);
    }

    private void mapTeacher(Teacher teacher, TeacherCreateDto dto) {
        teacher.setFirstName(dto.getFirstName());
        teacher.setLastName(dto.getLastName());
        teacher.setMiddleName(dto.getMiddleName());
    }
}