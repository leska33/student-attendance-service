package com.example.student;

import com.example.student.dto.GroupCreateDto;
import com.example.student.entity.Group;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.GroupRepository;
import com.example.student.service.GroupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private GroupService groupService;

    @Test
    void createGroup_success() {
        when(groupRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = groupService.createGroup(new GroupCreateDto());

        assertNotNull(result);
        verify(groupRepository).save(any());
    }

    @Test
    void getGroupById_notFound() {
        when(groupRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> groupService.getGroupById(1L));
    }

    @Test
    void deleteGroup_success() {
        Group group = new Group();

        when(groupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        groupService.deleteGroup(1L);

        verify(groupRepository).delete(group);
    }
}