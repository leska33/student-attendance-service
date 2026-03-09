package com.example.student.service;

import com.example.student.dto.GroupCreateDto;
import com.example.student.dto.GroupResponseDto;
import com.example.student.entity.Group;
import com.example.student.mapper.GroupMapper;
import com.example.student.repository.GroupRepository;

import org.springframework.stereotype.Service;

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

    public Group createGroup(GroupCreateDto dto) {

        Group group = new Group();
        group.setGroupNumber(dto.getGroupNumber());

        return groupRepository.save(group);
    }

    public void deleteGroup(Long id) {
        groupRepository.deleteById(id);
    }
}