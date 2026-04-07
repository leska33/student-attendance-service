package com.example.student;

import com.example.student.dto.TeacherCreateDto;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.TeacherRepository;
import com.example.student.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private TeacherService teacherService;

    @Test
    void createTeacher_success() {
        TeacherCreateDto dto = new TeacherCreateDto();
        dto.setFirstName("Ivan");
        dto.setLastName("Ivanov");

        when(teacherRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);

        when(teacherRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = teacherService.createTeacher(dto);

        assertNotNull(result);
        verify(teacherRepository).save(any());
    }

    @Test
    void createTeacher_shouldThrow_whenExists() {
        when(teacherRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> teacherService.createTeacher(new TeacherCreateDto()));
    }

    @Test
    void getTeacherById_notFound() {
        when(teacherRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teacherService.getTeacherById(1L));
    }
}