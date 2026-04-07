package com.example.student;

import com.example.student.dto.DisciplineCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Teacher;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.TeacherRepository;
import com.example.student.service.DisciplineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisciplineServiceTest {

    @Mock
    private DisciplineRepository disciplineRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private DisciplineService disciplineService;

    @Test
    void createDiscipline_success() {
        when(teacherRepository.findById(any()))
                .thenReturn(Optional.of(new Teacher()));

        when(disciplineRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = disciplineService.createDiscipline(new DisciplineCreateDto());

        assertNotNull(result);
    }

    @Test
    void createDiscipline_teacherNotFound() {
        when(teacherRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> disciplineService.createDiscipline(new DisciplineCreateDto()));
    }

    @Test
    void deleteDiscipline_success() {
        Discipline discipline = new Discipline();

        when(disciplineRepository.findById(1L))
                .thenReturn(Optional.of(discipline));

        disciplineService.deleteDiscipline(1L);

        verify(disciplineRepository).delete(discipline);
    }
}