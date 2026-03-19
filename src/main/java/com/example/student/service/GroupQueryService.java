package com.example.student.service;

import com.example.student.cache.GroupQueryKey;
import com.example.student.dto.GroupResponseDto;
import com.example.student.mapper.GroupMapper;
import com.example.student.repository.GroupRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupQueryService {

    private final GroupRepository repository;
    private final Map<GroupQueryKey, List<GroupResponseDto>> cache = new HashMap<>();

    public GroupQueryService(GroupRepository repository) {
        this.repository = repository;
    }

    public List<GroupResponseDto> getGroupsByStudentLastNameJPQL(
            String lastName, int page, int size) {

        GroupQueryKey key = new GroupQueryKey(
                lastName, page, size, "JPQL"
        );

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        List<GroupResponseDto> result = repository
                .findByStudentLastNameJPQL(lastName, PageRequest.of(page, size))
                .map(GroupMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public List<GroupResponseDto> getGroupsByStudentLastNameNative(
            String lastName, int page, int size) {

        GroupQueryKey key = new GroupQueryKey(
                lastName, page, size, "NATIVE"
        );

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        List<GroupResponseDto> result = repository
                .findByStudentLastNameNative(lastName, PageRequest.of(page, size))
                .map(GroupMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    @Transactional
    public void invalidateCache() {
        cache.clear();
    }
}