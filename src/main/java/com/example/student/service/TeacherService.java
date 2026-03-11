package com.example.student.service;

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

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    public List<TeacherResponseDto> getAllTeachersDtoLazy() {
        return teacherRepository.findAll()
                .stream()
                .map(TeacherMapper::toDto)
                .toList();
    }

    public List<TeacherResponseDto> getAllTeachersDtoOptimized() {
        return teacherRepository.findAllWithDisciplines()
                .stream()
                .map(TeacherMapper::toDto)
                .toList();
    }

    public TeacherResponseDto getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        return TeacherMapper.toDto(teacher);
    }

    @Transactional
    public TeacherResponseDto createTeacher(TeacherCreateDto dto) {
        Teacher teacher = new Teacher();
        teacher.setFullName(dto.getFullName());
        Teacher saved = teacherRepository.save(teacher);
        return TeacherMapper.toDto(saved);
    }

    @Transactional
    public TeacherResponseDto updateTeacher(Long id, TeacherCreateDto dto) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        teacher.setFullName(dto.getFullName());
        Teacher updated = teacherRepository.save(teacher);
        return TeacherMapper.toDto(updated);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (teacher.getDisciplines() != null) {
            for (Discipline d : teacher.getDisciplines()) {
                d.setTeacher(null);
            }
        }
        teacherRepository.delete(teacher);
    }
}