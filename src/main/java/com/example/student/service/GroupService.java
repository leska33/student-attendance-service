package com.example.student.service;

import com.example.student.entity.Group;
import com.example.student.repository.GroupRepository;
import com.example.student.dto.GroupResponseDto;
import com.example.student.mapper.GroupMapper;

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

    public Group createGroup(Group group) {
        return groupRepository.save(group);
    }

    public void deleteGroup(Long id) {
        groupRepository.deleteById(id);
    }
}