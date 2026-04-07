package com.example.student;

import com.example.student.dto.StudentCreateDto;
import com.example.student.entity.*;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.GroupRepository;
import com.example.student.repository.StudentRepository;
import com.example.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private DisciplineRepository disciplineRepository;
    @Mock private GradeRepository gradeRepository;

    @InjectMocks private StudentService studentService;

    @Test
    void createStudent_success() {
        StudentCreateDto dto = new StudentCreateDto();
        dto.setFirstName("Ivan");
        dto.setLastName("Ivanov");
        dto.setGroupId(1L);
        dto.setDisciplineIds(List.of(1L));

        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);

        when(groupRepository.findById(1L))
                .thenReturn(Optional.of(new Group()));

        when(disciplineRepository.findAllById(any()))
                .thenReturn(List.of(new Discipline()));

        when(studentRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = studentService.createStudent(dto);

        assertNotNull(result);
        verify(studentRepository).save(any());
    }

    @Test
    void createStudent_shouldThrow_whenExists() {
        StudentCreateDto dto = new StudentCreateDto();

        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> studentService.createStudent(dto));
    }

    @Test
    void createStudentsBulk_success() {
        StudentCreateDto dto = new StudentCreateDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setGroupId(1L);
        dto.setDisciplineIds(List.of(1L));

        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);

        when(groupRepository.findById(any()))
                .thenReturn(Optional.of(new Group()));

        when(disciplineRepository.findAllById(any()))
                .thenReturn(List.of(new Discipline()));

        when(studentRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = studentService.createStudentsBulk(List.of(dto));

        assertEquals(1, result.size());
    }

    @Test
    void createStudentsBulk_shouldThrow() {
        StudentCreateDto dto = new StudentCreateDto();

        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> studentService.createStudentsBulk(List.of(dto)));
    }

    @Test
    void getStudentById_success() {
        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(new Student()));

        assertNotNull(studentService.getStudentById(1L));
    }

    @Test
    void getStudentById_notFound() {
        when(studentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.getStudentById(1L));
    }

    @Test
    void deleteStudent_success() {
        Student student = new Student();
        student.setDisciplines(List.of());
        student.setGroup(null);

        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(student));

        studentService.deleteStudent(1L);

        verify(studentRepository).delete(student);
    }
}