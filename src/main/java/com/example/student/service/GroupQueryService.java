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
            System.out.println("GROUP_JPQL: FROM CACHE - lastName=" + lastName + ", page=" + page);
            return cache.get(key);
        }

        System.out.println("GROUP_JPQL: FROM DATABASE - lastName=" + lastName + ", page=" + page);
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
            System.out.println("GROUP_NATIVE: FROM CACHE - lastName=" + lastName + ", page=" + page);
            return cache.get(key);
        }

        System.out.println("GROUP_NATIVE: FROM DATABASE - lastName=" + lastName + ", page=" + page);
        List<GroupResponseDto> result = repository
                .findByStudentLastNameNative(lastName, PageRequest.of(page, size))
                .map(GroupMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    @Transactional
    public void invalidateCache() {
        System.out.println("GROUP: CACHE INVALIDATED - size before=" + cache.size());
        cache.clear();
        System.out.println("GROUP: CACHE INVALIDATED - size after=" + cache.size());
    }
}