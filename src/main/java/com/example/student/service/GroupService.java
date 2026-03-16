package com.example.student.service;

import com.example.student.dto.GroupCreateDto;
import com.example.student.dto.GroupResponseDto;
import com.example.student.entity.Group;
import com.example.student.entity.Student;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.mapper.GroupMapper;
import com.example.student.repository.GroupRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private static final String GROUP_NOT_FOUND = "Group not found";
    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public List<GroupResponseDto> getAllGroupsDtoLazy() {
        return groupRepository.findAll()
                .stream()
                .map(GroupMapper::toDto)
                .toList();
    }

    public List<GroupResponseDto> getAllGroupsDtoOptimized() {
        return groupRepository.findAllWithStudents()
                .stream()
                .map(GroupMapper::toDto)
                .toList();
    }

    public GroupResponseDto getGroupById(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(GROUP_NOT_FOUND));
        return GroupMapper.toDto(group);
    }

    @Transactional
    public GroupResponseDto createGroup(GroupCreateDto dto) {
        Group group = new Group();
        // теперь просто используем поле number из GroupCreateDto
        group.setNumber(dto.getNumber());
        Group saved = groupRepository.save(group);
        return GroupMapper.toDto(saved);
    }

    @Transactional
    public GroupResponseDto updateGroup(Long id, GroupCreateDto dto) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(GROUP_NOT_FOUND));
        group.setNumber(dto.getNumber());
        Group updated = groupRepository.save(group);
        return GroupMapper.toDto(updated);
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(GROUP_NOT_FOUND));
        if (group.getStudents() != null) {
            for (Student student : group.getStudents()) {
                student.setGroup(null);
            }
            group.getStudents().clear();
        }
        groupRepository.delete(group);
    }
}