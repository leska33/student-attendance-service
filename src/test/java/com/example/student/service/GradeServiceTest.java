package com.example.student.service;

import com.example.student.dto.GradeCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Grade;
import com.example.student.entity.Student;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void getAllGradesDtoLazy_maps() {
        when(gradeRepository.findAll()).thenReturn(List.of(new Grade()));

        assertEquals(1, service.getAllGradesDtoLazy().size());
    }

    @Test
    void getAllGradesDtoOptimized_maps() {
        when(gradeRepository.findAllWithRelations()).thenReturn(List.of(new Grade()));

        assertEquals(1, service.getAllGradesDtoOptimized().size());
    }

    @Test
    void getGradeById_success() {
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(new Grade()));

        assertNotNull(service.getGradeById(1L));
    }

    @Test
    void getGradeById_notFound() {
        when(gradeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getGradeById(1L));
    }

    @Test
    void getGradeById_nullId_notFound() {
        when(gradeRepository.findById(null)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getGradeById(null));
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

        assertThrows(ResourceNotFoundException.class, () -> service.createGrade(dto()));
    }

    @Test
    void discipline_notFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(disciplineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createGrade(dto()));
    }

    @Test
    void update_success() {
        Grade grade = new Grade();
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(grade));
        when(gradeRepository.save(any())).thenReturn(grade);

        assertNotNull(service.updateGrade(1L, dto()));
    }

    @Test
    void update_notFound() {
        when(gradeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateGrade(1L, dto()));
    }

    @Test
    void delete_success() {
        Grade grade = new Grade();
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(grade));

        service.deleteGrade(1L);

        verify(gradeRepository).delete(grade);
    }

    @Test
    void createGradesBulk_success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(new Discipline()));
        when(gradeRepository.save(any())).thenReturn(new Grade());

        assertEquals(1, service.createGradesBulk(List.of(dto())).size());
    }

    @Test
    void createGradesBulk_nullList_empty() {
        assertTrue(service.createGradesBulk(null).isEmpty());
    }

    @Test
    void createGradesBulk_transactional_throwsOnNegative() {
        GradeCreateDto bad = dto();
        bad.setValue(-1);

        assertThrows(IllegalStateException.class, () -> service.createGradesBulk(List.of(bad)));
    }

    @Test
    void createGradesBulkWithoutTransaction_throwsOnNegative() {
        GradeCreateDto bad = dto();
        bad.setValue(-1);

        assertThrows(IllegalStateException.class,
                () -> service.createGradesBulkWithoutTransaction(List.of(bad)));
    }

    @Test
    void assertBulkGradeRowValid_allowsNullValueForBranch() {
        GradeCreateDto d = dto();
        d.setValue(null);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(new Student()));
        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(new Discipline()));
        when(gradeRepository.save(any())).thenReturn(new Grade());

        assertEquals(1, service.createGradesBulk(List.of(d)).size());
    }

    @Test
    void deleteGrade_notFound() {
        when(gradeRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteGrade(2L));
    }

    @Test
    void createGradesBulkWithoutTransaction_nullList_empty() {
        assertTrue(service.createGradesBulkWithoutTransaction(null).isEmpty());
    }
}
