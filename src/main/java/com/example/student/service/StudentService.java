package com.example.student.service;

import com.example.student.aop.LogExecutionTime;
import com.example.student.dto.StudentCreateDto;
import com.example.student.dto.StudentResponseDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Group;
import com.example.student.entity.Student;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.StudentMapper;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.GroupRepository;
import com.example.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private static final String STUDENT_NOT_FOUND = "Student not found";
    private static final String GROUP_NOT_FOUND = "Group not found";
    private static final String STUDENT_ALREADY_EXISTS = "Student already exists";

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
                .sorted(Comparator.comparing(StudentResponseDto::getId))
                .toList();
    }

    @LogExecutionTime
    public List<StudentResponseDto> getAllStudentsDtoOptimized() {
        return studentRepository.findAllWithRelations()
                .stream()
                .map(StudentMapper::toDto)
                .sorted(Comparator.comparing(StudentResponseDto::getId))
                .toList();
    }

    @LogExecutionTime
    public StudentResponseDto getStudentById(Long id) {
        return StudentMapper.toDto(findStudentOrThrow(id));
    }

    @Transactional
    @LogExecutionTime
    public StudentResponseDto createStudent(StudentCreateDto dto) {
        validateStudentUniqueness(dto);

        Student student = buildStudent(dto);
        return StudentMapper.toDto(studentRepository.save(student));
    }

    @Transactional
    public List<StudentResponseDto> createStudentsBulk(List<StudentCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .map(this::validateBulkStudentRow)
                .map(this::createStudentEntity)
                .map(studentRepository::save)
                .map(StudentMapper::toDto)
                .toList();
    }

    public List<StudentResponseDto> createStudentsBulkWithoutTransaction(List<StudentCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .map(this::validateBulkStudentRow)
                .map(this::createStudentEntity)
                .map(studentRepository::save)
                .map(StudentMapper::toDto)
                .toList();
    }

    @Transactional
    @LogExecutionTime
    public StudentResponseDto updateStudent(Long id, StudentCreateDto dto) {
        Student student = findStudentOrThrow(id);
        mapStudent(student, dto);
        return StudentMapper.toDto(studentRepository.save(student));
    }

    @Transactional
    @LogExecutionTime
    public void deleteStudent(Long id) {
        Student student = findStudentOrThrow(id);

        gradeRepository.deleteAllByStudent(student);

        if (student.getDisciplines() != null) {
            for (Discipline d : student.getDisciplines()) {
                d.getStudents().remove(student);
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

    private Student findStudentOrThrow(Long id) {
        return Optional.ofNullable(id)
                .flatMap(studentRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
    }

    private Group findGroupOrThrow(Long id) {
        return Optional.ofNullable(id)
                .flatMap(groupRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException(GROUP_NOT_FOUND));
    }

    private StudentCreateDto validateBulkStudentRow(StudentCreateDto dto) {
        if (BulkOperationConstants.ERROR_SENTINEL.equals(dto.getFirstName())) {
            throw new IllegalStateException(BulkOperationConstants.MSG_BULK_GENERIC);
        }
        return dto;
    }

    private void validateStudentUniqueness(StudentCreateDto dto) {
        if (studentRepository.existsByFirstNameAndLastNameAndMiddleName(
                dto.getFirstName(),
                dto.getLastName(),
                dto.getMiddleName()
        )) {
            throw new AlreadyExistsException(STUDENT_ALREADY_EXISTS);
        }
    }

    private Student buildStudent(StudentCreateDto dto) {
        Student student = new Student();
        mapStudent(student, dto);
        return student;
    }

    private Student createStudentEntity(StudentCreateDto dto) {
        validateStudentUniqueness(dto);
        return buildStudent(dto);
    }

    private void mapStudent(Student student, StudentCreateDto dto) {
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setMiddleName(dto.getMiddleName());
        student.setGroup(findGroupOrThrow(dto.getGroupId()));
        student.setDisciplines(disciplineRepository.findAllById(dto.getDisciplineIds()));
    }
}