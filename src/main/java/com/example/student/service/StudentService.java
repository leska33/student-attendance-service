package com.example.student.service;

import com.example.student.dto.StudentCreateDto;
import com.example.student.dto.StudentResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Group;
import com.example.student.entity.Student;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.StudentMapper;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GroupRepository;
import com.example.student.repository.StudentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private static final String STUDENT_NOT_FOUND = "Student not found";
    private static final String GROUP_NOT_FOUND = "Group not found";
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

    public StudentResponseDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        return StudentMapper.toDto(student);
    }

    @Transactional
    public StudentResponseDto createStudent(StudentCreateDto dto) {
        Student student = new Student();
        student.setFullName(dto.getFullName());
        student.setAttendanceCount(dto.getAttendanceCount());
        student.setAverageGrade(dto.getAverageGrade());
        if (dto.getGroupId() != null) {
            Group group = groupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException(GROUP_NOT_FOUND));
            student.setGroup(group);
        }
        if (dto.getDisciplineIds() != null) {
            List<Discipline> disciplines = disciplineRepository.findAllById(dto.getDisciplineIds());
            student.setDisciplines(disciplines);
        }
        Student saved = studentRepository.save(student);
        return StudentMapper.toDto(saved);
    }

    @Transactional
    public StudentResponseDto updateStudent(Long id, StudentCreateDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        student.setFullName(dto.getFullName());
        student.setAttendanceCount(dto.getAttendanceCount());
        student.setAverageGrade(dto.getAverageGrade());
        if (dto.getGroupId() != null) {
            Group group = groupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException(GROUP_NOT_FOUND));
            student.setGroup(group);
        }
        if (dto.getDisciplineIds() != null) {
            List<Discipline> disciplines = disciplineRepository.findAllById(dto.getDisciplineIds());
            student.setDisciplines(disciplines);
        }
        Student updated = studentRepository.save(student);
        return StudentMapper.toDto(updated);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        if (student.getDisciplines() != null) {
            student.getDisciplines().clear();
        }
        studentRepository.delete(student);
    }

    public void saveWithoutTransaction(StudentCreateDto dto) {
        Student student = new Student();
        student.setFullName(dto.getFullName());
        studentRepository.save(student);
        throw new ResourceNotFoundException(
                "Ошибка после сохранения — данные частично сохранены");
    }

    @Transactional
    public void saveWithTransaction(StudentCreateDto dto) {
        Student student = new Student();
        student.setFullName(dto.getFullName());
        studentRepository.save(student);
        throw new ResourceNotFoundException(
                "Ошибка — транзакция откатится");
    }
}