package com.example.student;

import com.example.student.dto.GradeCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Grade;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    GradeRepository gradeRepository;

    @Mock
    StudentRepository studentRepository;

    @Mock
    DisciplineRepository disciplineRepository;

    @InjectMocks
    GradeService service;

    private GradeCreateDto dto() {
        GradeCreateDto dto = new GradeCreateDto();
        dto.setValue(5);
        dto.setStudentId(1L);
        dto.setDisciplineId(1L);
        return dto;
    }

    @Test
    void create_success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(new Discipline()));
        when(gradeRepository.save(any())).thenReturn(new Grade());

        assertNotNull(service.createGrade(dto()));
    }

    @Test
    void student_notFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createGrade(dto()));
    }

    @Test
    void discipline_notFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(disciplineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createGrade(dto()));
    }

    @Test
    void get_notFound() {
        when(gradeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getGradeById(1L));
    }

    @Test
    void bulk_error() {
        GradeCreateDto dto = dto();
        dto.setValue(-1);

        assertThrows(IllegalStateException.class,
                () -> service.createGradesBulkWithoutTransaction(List.of(dto)));
    }
}