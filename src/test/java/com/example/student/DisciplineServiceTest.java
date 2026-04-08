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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisciplineServiceTest {

    private final DisciplineRepository disciplineRepository = mock(DisciplineRepository.class);
    private final TeacherRepository teacherRepository = mock(TeacherRepository.class);
    private final GradeRepository gradeRepository = mock(GradeRepository.class);

    private final DisciplineService service =
            new DisciplineService(disciplineRepository, teacherRepository, gradeRepository);

    @Test
    void createDiscipline_success() {
        DisciplineCreateDto dto = new DisciplineCreateDto();
        dto.setName("Math");
        dto.setTeacherId(1L);

        Teacher teacher = new Teacher();
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        Discipline saved = new Discipline();
        saved.setName("Math");
        when(disciplineRepository.save(any())).thenReturn(saved);

        assertNotNull(service.createDiscipline(dto));
    }

    @Test
    void createDiscipline_teacherNotFound() {
        DisciplineCreateDto dto = new DisciplineCreateDto();
        dto.setName("Math");
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
    void deleteDiscipline_success() {
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
                () -> service.createDisciplinesBulkWithoutTransaction(java.util.List.of(dto)));
    }
}