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
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        return GroupMapper.toDto(group);
    }

    @Transactional
    public GroupResponseDto createGroup(GroupCreateDto dto) {
        Group group = new Group();
        group.setGroupNumber(dto.getGroupNumber());
        Group saved = groupRepository.save(group);
        return GroupMapper.toDto(saved);
    }

    @Transactional
    public GroupResponseDto updateGroup(Long id, GroupCreateDto dto) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        group.setGroupNumber(dto.getGroupNumber());
        Group updated = groupRepository.save(group);
        return GroupMapper.toDto(updated);
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (group.getStudents() != null) {
            for (Student s : group.getStudents()) {
                s.setGroup(null);
            }
        }
        groupRepository.delete(group);
    }
}