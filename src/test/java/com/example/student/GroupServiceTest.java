package com.example.student;

import com.example.student.dto.GroupCreateDto;
import com.example.student.entity.Group;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.GroupRepository;
import com.example.student.service.GroupService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupServiceTest {

    private final GroupRepository repository = mock(GroupRepository.class);
    private final GroupService service = new GroupService(repository);

    @Test
    void create_success() {
        when(repository.save(any())).thenReturn(new Group());

        GroupCreateDto dto = new GroupCreateDto();
        dto.setNumber("123");

        assertNotNull(service.createGroup(dto));
    }

    @Test
    void get_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getGroupById(1L));
    }

    @Test
    void delete_success() {
        Group group = new Group();
        when(repository.findById(1L)).thenReturn(Optional.of(group));

        service.deleteGroup(1L);

        verify(repository).delete(group);
    }

    @Test
    void bulk_error() {
        GroupCreateDto dto = new GroupCreateDto();
        dto.setNumber("ERROR");

        List<GroupCreateDto> list = java.util.List.of(dto);

        assertThrows(IllegalStateException.class,
                () -> service.createGroupsBulkWithoutTransaction(list));
    }
}