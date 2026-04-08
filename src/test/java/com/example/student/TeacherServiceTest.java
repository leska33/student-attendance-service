package com.example.student;

import com.example.student.dto.TeacherCreateDto;
import com.example.student.entity.Teacher;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.TeacherRepository;
import com.example.student.service.TeacherService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeacherServiceTest {

    private final TeacherRepository repository = mock(TeacherRepository.class);
    private final TeacherService service = new TeacherService(repository);

    private TeacherCreateDto dto() {
        TeacherCreateDto dto = new TeacherCreateDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setMiddleName("C");
        return dto;
    }

    @Test
    void create_success() {
        when(repository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);
        when(repository.save(any())).thenReturn(new Teacher());

        assertNotNull(service.createTeacher(dto()));
    }

    @Test
    void create_duplicate() {
        when(repository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> service.createTeacher(dto()));
    }

    @Test
    void get_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getTeacherById(1L));
    }

    @Test
    void bulk_error() {
        TeacherCreateDto dto = dto();
        dto.setFirstName("ERROR");

        assertThrows(IllegalStateException.class,
                () -> service.createTeachersBulkWithoutTransaction(
                        java.util.List.of(dto)
                ));
    }
}