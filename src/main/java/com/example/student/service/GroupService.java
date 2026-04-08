package com.example.student.service;

import com.example.student.aop.LogExecutionTime;
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
import java.util.Optional;

@Service
public class GroupService {

    private static final String GROUP_NOT_FOUND = "Group not found";

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @LogExecutionTime
    public List<GroupResponseDto> getAllGroupsDtoLazy() {
        return groupRepository.findAll().stream()
                .map(GroupMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public List<GroupResponseDto> getAllGroupsDtoOptimized() {
        return groupRepository.findAllWithStudents().stream()
                .map(GroupMapper::toDto)
                .toList();
    }

    @LogExecutionTime
    public GroupResponseDto getGroupById(Long id) {
        return GroupMapper.toDto(findGroupOrThrow(id));
    }

    @Transactional
    @LogExecutionTime
    public GroupResponseDto createGroup(GroupCreateDto dto) {
        Group group = new Group();
        group.setNumber(dto.getNumber());
        return GroupMapper.toDto(groupRepository.save(group));
    }

    @Transactional
    public List<GroupResponseDto> createGroupsBulk(List<GroupCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .peek(this::assertBulkGroupRowValid)
                .map(this::createGroupEntity)
                .map(groupRepository::save)
                .map(GroupMapper::toDto)
                .toList();
    }

    public List<GroupResponseDto> createGroupsBulkWithoutTransaction(List<GroupCreateDto> dtos) {
        return Optional.ofNullable(dtos).orElseGet(List::of).stream()
                .peek(this::assertBulkGroupRowValid)
                .map(this::createGroupEntity)
                .map(groupRepository::save)
                .map(GroupMapper::toDto)
                .toList();
    }

    @Transactional
    @LogExecutionTime
    public GroupResponseDto updateGroup(Long id, GroupCreateDto dto) {
        Group group = findGroupOrThrow(id);
        group.setNumber(dto.getNumber());
        return GroupMapper.toDto(groupRepository.save(group));
    }

    @Transactional
    @LogExecutionTime
    public void deleteGroup(Long id) {
        Group group = findGroupOrThrow(id);

        if (group.getStudents() != null) {
            for (Student s : group.getStudents()) {
                s.setGroup(null);
            }
            group.getStudents().clear();
        }

        groupRepository.delete(group);
    }

    private Group findGroupOrThrow(Long id) {
        return Optional.ofNullable(id)
                .flatMap(groupRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException(GROUP_NOT_FOUND));
    }

    private void assertBulkGroupRowValid(GroupCreateDto dto) {
        if ("ERROR".equals(dto.getNumber())) {
            throw new IllegalStateException("Ошибка группы");
        }
    }

    private Group createGroupEntity(GroupCreateDto dto) {
        Group group = new Group();
        group.setNumber(dto.getNumber());
        return group;
    }
}