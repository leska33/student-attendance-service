package com.example.student.service;

import com.example.student.cache.GroupQueryKey;
import com.example.student.dto.GroupResponseDto;
import com.example.student.entity.Group;
import com.example.student.mapper.GroupMapper;
import com.example.student.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupQueryService.class);

    private final GroupRepository repository;
    private final Map<GroupQueryKey, List<GroupResponseDto>> cache = new ConcurrentHashMap<>();

    public GroupQueryService(GroupRepository repository) {
        this.repository = repository;
    }

    public List<GroupResponseDto> getGroupsByStudentLastNameJPQL(String lastName, int page, int size) {
        GroupQueryKey key = new GroupQueryKey(lastName, page, size, "JPQL");

        List<GroupResponseDto> cached = cache.get(key);
        if (cached != null) {
            LOGGER.info("GROUP_JPQL: FROM CACHE");
            return cached;
        }

        LOGGER.info("GROUP_JPQL: FROM DATABASE");

        List<GroupResponseDto> result = repository
                .findByStudentLastNameJPQL(lastName, PageRequest.of(page, size))
                .map(GroupMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public List<GroupResponseDto> getGroupsByStudentLastNameNative(String lastName, int page, int size) {
        GroupQueryKey key = new GroupQueryKey(lastName, page, size, "NATIVE");

        List<GroupResponseDto> cached = cache.get(key);
        if (cached != null) {
            LOGGER.info("GROUP_NATIVE: FROM CACHE");
            return cached;
        }

        LOGGER.info("GROUP_NATIVE: FROM DATABASE");

        List<GroupResponseDto> result = repository
                .findByStudentLastNameNative(lastName, PageRequest.of(page, size))
                .map(GroupMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public void invalidateCache() {
        LOGGER.info("GROUP CACHE CLEARED");
        cache.clear();
    }

    @Transactional
    public Group saveOrUpdate(Group group) {
        Group saved = repository.save(group);
        invalidateCache();
        return saved;
    }

    @Transactional
    public void delete(Group group) {
        repository.delete(group);
        invalidateCache();
    }
}