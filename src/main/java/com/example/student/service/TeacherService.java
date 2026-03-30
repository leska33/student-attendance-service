package com.example.student.service;

import com.example.student.aop.LogExecutionTime;
import com.example.student.dto.TeacherCreateDto;
import com.example.student.dto.TeacherResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Teacher;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.TeacherMapper;
import com.example.student.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherService {

    private static final String TEACHER_NOT_FOUND = "Teacher not found";

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
        Teacher teacher = new Teacher();

        teacher.setFirstName(dto.getFirstName());
        teacher.setLastName(dto.getLastName());
        teacher.setMiddleName(dto.getMiddleName());

        return TeacherMapper.toDto(teacherRepository.save(teacher));
    }

    @Transactional
    @LogExecutionTime
    public TeacherResponseDto updateTeacher(Long id, TeacherCreateDto dto) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TEACHER_NOT_FOUND));

        teacher.setFirstName(dto.getFirstName());
        teacher.setLastName(dto.getLastName());
        teacher.setMiddleName(dto.getMiddleName());

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
}