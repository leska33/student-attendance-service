package com.example.student.service;

import com.example.student.entity.Student;
import com.example.student.entity.Group;
import com.example.student.entity.Discipline;

import com.example.student.repository.StudentRepository;
import com.example.student.repository.GroupRepository;
import com.example.student.repository.DisciplineRepository;

import com.example.student.dto.StudentResponseDto;
import com.example.student.mapper.StudentMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;
    private final DisciplineRepository disciplineRepository;

    public StudentService(StudentRepository studentRepository,
                          GroupRepository groupRepository,
                          DisciplineRepository disciplineRepository) {
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.disciplineRepository = disciplineRepository;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    public Student createStudent(Student student) {
        return studentRepository.save(student);
    }

    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }

    public List<StudentResponseDto> getAllStudentsDtoLazy() {
        return studentRepository.findAll()
                .stream()
                .map(StudentMapper::toDto)
                .toList();
    }

    public List<StudentResponseDto> getAllStudentsDtoOptimized() {
        return studentRepository.findAllWithRelations()
                .stream()
                .map(StudentMapper::toDto)
                .toList();
    }

    @Transactional
    public Student createStudentWithRelations(
            Student student,
            Long groupId,
            List<Long> disciplineIds) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        List<Discipline> disciplines = disciplineRepository.findAllById(disciplineIds);

        student.setGroup(group);
        student.setDisciplines(disciplines);

        return studentRepository.save(student);
    }

    public void saveWithoutTransaction(Student student) {
        studentRepository.save(student);
        throw new RuntimeException("Ошибка после сохранения — данные частично сохранены");
    }

    @Transactional
    public void saveWithTransaction(Student student) {
        studentRepository.save(student);
        throw new RuntimeException("Ошибка — транзакция откатится, данные не сохранятся");
    }
}