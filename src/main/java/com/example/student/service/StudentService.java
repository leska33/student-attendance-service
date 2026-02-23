package com.example.student.service;

import com.example.student.dto.StudentResponseDto;
import com.example.student.entity.Student;
import com.example.student.mapper.StudentMapper;
import com.example.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<StudentResponseDto> getAllStudents() {
        return studentRepository.findAll().stream().map(StudentMapper::toDto).toList();
    }

    public StudentResponseDto findStudentById(String studentId) {
        Student student = studentRepository.findAll().stream().filter(s -> s.getStudentId().equals(studentId)).findFirst().orElse(null);
        return StudentMapper.toDto(student);
    }

    public List<StudentResponseDto> findStudentsByGroup(String groupNumber) {
        if (groupNumber == null || groupNumber.isEmpty()) {
            return getAllStudents();
        }
        return studentRepository.findAll().stream().filter(s -> s.getGroupNumber().equals(groupNumber)).map(StudentMapper::toDto).toList();
    }
}