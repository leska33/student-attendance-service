package com.example.student.service;

import com.example.student.dto.GroupCreateDto;
import com.example.student.entity.Group;
import com.example.student.entity.Student;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.GroupRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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

class GroupServiceTest {

    private final GroupRepository repository = mock(GroupRepository.class);
    private final GroupService service = new GroupService(repository);

    private GroupCreateDto dto(String number) {
        GroupCreateDto dto = new GroupCreateDto();
        dto.setNumber(number);
        return dto;
    }

    @Test
    void getAllGroupsDtoLazy_maps() {
        when(repository.findAll()).thenReturn(List.of(new Group()));

        assertEquals(1, service.getAllGroupsDtoLazy().size());
    }

    @Test
    void getAllGroupsDtoOptimized_maps() {
        when(repository.findAllWithStudents()).thenReturn(List.of(new Group()));

        assertEquals(1, service.getAllGroupsDtoOptimized().size());
    }

    @Test
    void getGroupById_success() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Group()));

        assertNotNull(service.getGroupById(1L));
    }

    @Test
    void getGroupById_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getGroupById(1L));
    }

    @Test
    void getGroupById_nullId() {
        when(repository.findById(null)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getGroupById(null));
    }

    @Test
    void create_success() {
        when(repository.save(any())).thenReturn(new Group());

        assertNotNull(service.createGroup(dto("123")));
    }

    @Test
    void update_success() {
        Group group = new Group();
        when(repository.findById(1L)).thenReturn(Optional.of(group));
        when(repository.save(any())).thenReturn(group);

        assertNotNull(service.updateGroup(1L, dto("999")));
    }

    @Test
    void update_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        GroupCreateDto update = dto("1");
        assertThrows(ResourceNotFoundException.class, () -> service.updateGroup(1L, update));
    }

    @Test
    void delete_success_emptyStudents() {
        Group group = new Group();
        group.setStudents(new ArrayList<>());
        when(repository.findById(1L)).thenReturn(Optional.of(group));

        service.deleteGroup(1L);

        verify(repository).delete(group);
    }

    @Test
    void delete_success_nullStudents() {
        Group group = new Group();
        group.setStudents(null);
        when(repository.findById(1L)).thenReturn(Optional.of(group));

        service.deleteGroup(1L);

        verify(repository).delete(group);
    }

    @Test
    void delete_success_withStudents() {
        Group group = new Group();
        List<Student> students = new ArrayList<>();
        Student s = new Student();
        students.add(s);
        group.setStudents(students);
        s.setGroup(group);

        when(repository.findById(1L)).thenReturn(Optional.of(group));

        service.deleteGroup(1L);

        verify(repository).delete(group);
    }

    @Test
    void delete_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteGroup(1L));
    }

    @Test
    void createGroupsBulk_success() {
        when(repository.save(any())).thenReturn(new Group());

        assertEquals(1, service.createGroupsBulk(List.of(dto("1"))).size());
    }

    @Test
    void createGroupsBulk_null_empty() {
        assertTrue(service.createGroupsBulk(null).isEmpty());
    }

    @Test
    void createGroupsBulk_transactional_errorRow() {
        List<GroupCreateDto> bulk = List.of(dto(BulkOperationConstants.ERROR_SENTINEL));
        assertThrows(IllegalStateException.class, () -> service.createGroupsBulk(bulk));
    }

    @Test
    void createGroupsBulkWithoutTransaction_error() {
        List<GroupCreateDto> bulk = List.of(dto(BulkOperationConstants.ERROR_SENTINEL));
        assertThrows(IllegalStateException.class, () -> service.createGroupsBulkWithoutTransaction(bulk));
    }

    @Test
    void createGroupsBulkWithoutTransaction_null_empty() {
        assertTrue(service.createGroupsBulkWithoutTransaction(null).isEmpty());
    }
}
