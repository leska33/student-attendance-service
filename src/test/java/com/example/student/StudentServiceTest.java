package com.example.student;

import com.example.student.dto.StudentCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Group;
import com.example.student.entity.Student;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.GroupRepository;
import com.example.student.repository.StudentRepository;
import com.example.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    StudentRepository studentRepository;

    @Mock
    GroupRepository groupRepository;

    @Mock
    DisciplineRepository disciplineRepository;

    @Mock
    GradeRepository gradeRepository;

    @InjectMocks
    StudentService service;

    private StudentCreateDto dto() {
        StudentCreateDto dto = new StudentCreateDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setMiddleName("C");
        dto.setGroupId(1L);
        dto.setDisciplineIds(List.of(1L));
        return dto;
    }

    @Test
    void create_success() {
        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(new Group()));
        when(disciplineRepository.findAllById(any())).thenReturn(List.of(new Discipline()));
        when(studentRepository.save(any())).thenReturn(new Student());

        assertNotNull(service.createStudent(dto()));
    }

    @Test
    void create_duplicate() {
        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> service.createStudent(dto()));
    }

    @Test
    void create_groupNotFound() {
        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);

        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createStudent(dto()));
    }

    @Test
    void getById_notFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getStudentById(1L));
    }

    @Test
    void delete_success() {
        Student student = new Student();
        student.setDisciplines(new ArrayList<>());

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.deleteStudent(1L);

        verify(studentRepository).delete(student);
    }

    @Test
    void bulk_error() {
        StudentCreateDto dto = dto();
        dto.setFirstName("ERROR");

        assertThrows(IllegalStateException.class,
                () -> service.createStudentsBulkWithoutTransaction(List.of(dto)));
    }
}