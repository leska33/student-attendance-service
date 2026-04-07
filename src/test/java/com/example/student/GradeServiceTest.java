package com.example.student;

import com.example.student.dto.GradeCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Student;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.StudentRepository;
import com.example.student.service.GradeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DisciplineRepository disciplineRepository;

    @InjectMocks
    private GradeService gradeService;

    @Test
    void createGrade_success() {
        when(studentRepository.findById(any()))
                .thenReturn(Optional.of(new Student()));

        when(disciplineRepository.findById(any()))
                .thenReturn(Optional.of(new Discipline()));

        when(gradeRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = gradeService.createGrade(new GradeCreateDto());

        assertNotNull(result);
    }

    @Test
    void createGrade_studentNotFound() {
        when(studentRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gradeService.createGrade(new GradeCreateDto()));
    }

    @Test
    void createGrade_disciplineNotFound() {
        when(studentRepository.findById(any()))
                .thenReturn(Optional.of(new Student()));

        when(disciplineRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gradeService.createGrade(new GradeCreateDto()));
    }
}