package com.example.student;

import com.example.student.dto.DisciplineCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Teacher;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.TeacherRepository;
import com.example.student.service.DisciplineService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisciplineServiceTest {

    @Mock
    DisciplineRepository disciplineRepository;

    @Mock
    TeacherRepository teacherRepository;

    @Mock
    GradeRepository gradeRepository;

    @InjectMocks
    DisciplineService service;

    @Test
    void createDiscipline_success() {
        DisciplineCreateDto dto = new DisciplineCreateDto();
        dto.setName("Math");
        dto.setTeacherId(1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(new Teacher()));
        when(disciplineRepository.save(any())).thenReturn(new Discipline());

        assertNotNull(service.createDiscipline(dto));
    }

    @Test
    void createDiscipline_teacherNotFound() {
        DisciplineCreateDto dto = new DisciplineCreateDto();
        dto.setTeacherId(1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createDiscipline(dto));
    }

    @Test
    void getDisciplineById_notFound() {
        when(disciplineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getDisciplineById(1L));
    }

    @Test
    void delete_success() {
        Discipline discipline = new Discipline();

        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(discipline));

        service.deleteDiscipline(1L);

        verify(disciplineRepository).delete(discipline);
    }

    @Test
    void bulkWithoutTransaction_error() {
        DisciplineCreateDto dto = new DisciplineCreateDto();
        dto.setName("ERROR");

        assertThrows(IllegalStateException.class,
                () -> service.createDisciplinesBulkWithoutTransaction(List.of(dto)));
    }
}