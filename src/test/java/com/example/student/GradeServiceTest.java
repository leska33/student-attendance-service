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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradeServiceTest {

    private final GradeRepository gradeRepository = mock(GradeRepository.class);
    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final DisciplineRepository disciplineRepository = mock(DisciplineRepository.class);

    private final GradeService service =
            new GradeService(gradeRepository, studentRepository, disciplineRepository);

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

        GradeCreateDto gradeDto = dto();
        assertThrows(ResourceNotFoundException.class,
                () -> service.createGrade(gradeDto));
    }

    @Test
    void discipline_notFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(disciplineRepository.findById(1L)).thenReturn(Optional.empty());

        GradeCreateDto gradeDto = dto();
        assertThrows(ResourceNotFoundException.class,
                () -> service.createGrade(gradeDto));
    }

    @Test
    void get_notFound() {
        when(gradeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getGradeById(1L));
    }

    @Test
    void bulk_error() {
        GradeCreateDto gradeDto = dto();
        gradeDto.setValue(-1);

        assertThrows(IllegalStateException.class,
                () -> service.createGradesBulkWithoutTransaction(java.util.List.of(gradeDto)));
    }
}