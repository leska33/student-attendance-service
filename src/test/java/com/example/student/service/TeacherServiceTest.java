package com.example.student.service;

import com.example.student.dto.TeacherCreateDto;
import com.example.student.entity.Teacher;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.TeacherRepository;
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
    void getAllTeachersDtoLazy_maps() {
        when(repository.findAll()).thenReturn(List.of(new Teacher()));

        assertEquals(1, service.getAllTeachersDtoLazy().size());
    }

    @Test
    void getAllTeachersDtoOptimized_maps() {
        when(repository.findAllWithDisciplines()).thenReturn(List.of(new Teacher()));

        assertEquals(1, service.getAllTeachersDtoOptimized().size());
    }

    @Test
    void getTeacherById_success() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Teacher()));

        assertNotNull(service.getTeacherById(1L));
    }

    @Test
    void getTeacherById_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getTeacherById(1L));
    }

    @Test
    void getTeacherById_nullId() {
        when(repository.findById(null)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getTeacherById(null));
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

        TeacherCreateDto create = dto();
        assertThrows(AlreadyExistsException.class, () -> service.createTeacher(create));
    }

    @Test
    void update_success() {
        Teacher teacher = new Teacher();
        when(repository.findById(1L)).thenReturn(Optional.of(teacher));
        when(repository.save(any())).thenReturn(teacher);

        assertNotNull(service.updateTeacher(1L, dto()));
    }

    @Test
    void update_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        TeacherCreateDto update = dto();
        assertThrows(ResourceNotFoundException.class, () -> service.updateTeacher(1L, update));
    }

    @Test
    void delete_success() {
        Teacher teacher = new Teacher();
        when(repository.findById(1L)).thenReturn(Optional.of(teacher));

        service.deleteTeacher(1L);

        verify(repository).delete(teacher);
    }

    @Test
    void delete_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteTeacher(1L));
    }

    @Test
    void createTeachersBulk_success() {
        when(repository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);
        when(repository.save(any())).thenReturn(new Teacher());

        assertEquals(1, service.createTeachersBulk(List.of(dto())).size());
    }

    @Test
    void createTeachersBulk_null_empty() {
        assertTrue(service.createTeachersBulk(null).isEmpty());
    }

    @Test
    void createTeachersBulk_transactional_error() {
        TeacherCreateDto bad = dto();
        bad.setFirstName(BulkOperationConstants.ERROR_SENTINEL);

        List<TeacherCreateDto> bulk = List.of(bad);
        assertThrows(IllegalStateException.class, () -> service.createTeachersBulk(bulk));
    }

    @Test
    void bulkWithoutTransaction_error() {
        TeacherCreateDto bad = dto();
        bad.setFirstName(BulkOperationConstants.ERROR_SENTINEL);

        List<TeacherCreateDto> bulk = List.of(bad);
        assertThrows(IllegalStateException.class,
                () -> service.createTeachersBulkWithoutTransaction(bulk));
    }

    @Test
    void createTeachersBulkWithoutTransaction_null_empty() {
        assertTrue(service.createTeachersBulkWithoutTransaction(null).isEmpty());
    }
}
