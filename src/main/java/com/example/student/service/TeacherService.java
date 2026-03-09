package com.example.student.service;

import com.example.student.entity.Teacher;
import com.example.student.repository.TeacherRepository;
import com.example.student.dto.TeacherResponseDto;
import com.example.student.mapper.TeacherMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    public List<TeacherResponseDto> getAllTeachersDtoLazy() {
        return teacherRepository.findAll()
                .stream()
                .map(TeacherMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<TeacherResponseDto> getAllTeachersDtoOptimized() {
        return teacherRepository.findAllWithDisciplines()
                .stream()
                .map(TeacherMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Teacher createTeacher(Teacher teacher) {
        return teacherRepository.save(teacher);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        teacherRepository.deleteById(id);
    }
}