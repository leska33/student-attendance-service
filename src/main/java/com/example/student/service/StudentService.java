package com.example.student.service;

import com.example.student.aop.LogExecutionTime;
import com.example.student.dto.StudentCreateDto;
import com.example.student.dto.StudentResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Group;
import com.example.student.entity.Student;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.StudentMapper;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
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
    private final GradeRepository gradeRepository;

    public StudentService(StudentRepository studentRepository,
                          GroupRepository groupRepository,
                          DisciplineRepository disciplineRepository,
                          GradeRepository gradeRepository) {
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.disciplineRepository = disciplineRepository;
        this.gradeRepository = gradeRepository;
    }

    @LogExecutionTime
    public List<StudentResponseDto> getAllStudentsDtoLazy() {
        return studentRepository.findAll()
                .stream()
                .map(StudentMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public List<StudentResponseDto> getAllStudentsDtoOptimized() {
        return studentRepository.findAllWithRelations()
                .stream()
                .map(StudentMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public StudentResponseDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        return StudentMapper.toDto(student);
    }

    @Transactional
    @LogExecutionTime
    public StudentResponseDto createStudent(StudentCreateDto dto) {
        Student student = new Student();

        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setMiddleName(dto.getMiddleName());

        Group group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException(GROUP_NOT_FOUND));
        student.setGroup(group);

        List<Discipline> disciplines = disciplineRepository.findAllById(dto.getDisciplineIds());
        student.setDisciplines(disciplines);

        return StudentMapper.toDto(studentRepository.save(student));
    }

    @Transactional
    @LogExecutionTime
    public StudentResponseDto updateStudent(Long id, StudentCreateDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setMiddleName(dto.getMiddleName());

        Group group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException(GROUP_NOT_FOUND));
        student.setGroup(group);

        List<Discipline> disciplines = disciplineRepository.findAllById(dto.getDisciplineIds());
        student.setDisciplines(disciplines);

        return StudentMapper.toDto(studentRepository.save(student));
    }

    @Transactional
    @LogExecutionTime
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        gradeRepository.deleteAllByStudent(student);

        if (student.getDisciplines() != null) {
            for (Discipline discipline : student.getDisciplines()) {
                discipline.getStudents().remove(student);
            }
            student.getDisciplines().clear();
        }

        if (student.getGroup() != null) {
            Group group = student.getGroup();
            group.getStudents().remove(student);
            student.setGroup(null);
        }

        studentRepository.delete(student);
    }

    @LogExecutionTime
    public void saveWithoutTransaction(StudentCreateDto dto) {
        Student student = new Student();
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setMiddleName(dto.getMiddleName());

        studentRepository.save(student);

        throw new ResourceNotFoundException("Ошибка после сохранения — данные частично сохранены");
    }

    @Transactional
    @LogExecutionTime
    public void saveWithTransaction(StudentCreateDto dto) {
        Student student = new Student();
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setMiddleName(dto.getMiddleName());

        studentRepository.save(student);

        throw new ResourceNotFoundException("Ошибка — транзакция откатится");
    }
}