package com.example.student;

import com.example.student.dto.StudentCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Group;
import com.example.student.entity.Student;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.*;
import com.example.student.service.StudentService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentServiceTest {

    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final DisciplineRepository disciplineRepository = mock(DisciplineRepository.class);
    private final GradeRepository gradeRepository = mock(GradeRepository.class);

    private final StudentService service =
            new StudentService(studentRepository, groupRepository, disciplineRepository, gradeRepository);

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

        StudentCreateDto studentDto = dto();
        assertThrows(AlreadyExistsException.class,
                () -> service.createStudent(studentDto));
    }

    @Test
    void create_groupNotFound() {
        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        StudentCreateDto studentDto = dto();
        assertThrows(ResourceNotFoundException.class,
                () -> service.createStudent(studentDto));
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
        student.setDisciplines(List.of());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.deleteStudent(1L);

        verify(studentRepository).delete(student);
    }

    @Test
    void bulk_error() {
        StudentCreateDto studentDto = dto();
        studentDto.setFirstName("ERROR");

        assertThrows(IllegalStateException.class,
                () -> service.createStudentsBulkWithoutTransaction(List.of(studentDto)));
    }
}