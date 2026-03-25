package com.example.student.service;

import com.example.student.cache.GroupQueryKey;
import com.example.student.dto.GroupResponseDto;
import com.example.student.mapper.GroupMapper;
import com.example.student.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupQueryService.class);
    private static final String PAGE_LOG = ", page=";

    private final GroupRepository repository;
    private final Map<GroupQueryKey, List<GroupResponseDto>> cache = new HashMap<>();

    public GroupQueryService(GroupRepository repository) {
        this.repository = repository;
    }

    public List<GroupResponseDto> getGroupsByStudentLastNameJPQL(String lastName, int page, int size) {
        GroupQueryKey key = new GroupQueryKey(lastName, page, size, "JPQL");

        if (cache.containsKey(key)) {
            LOGGER.info(String.format("GROUP_JPQL: FROM CACHE - lastName=%s, page=%d", lastName, page));
            return cache.get(key);
        }

        LOGGER.info(String.format("GROUP_JPQL: FROM DATABASE - lastName=%s, page=%d", lastName, page));

        List<GroupResponseDto> result = repository
                .findByStudentLastNameJPQL(lastName, PageRequest.of(page, size))
                .map(GroupMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    public List<GroupResponseDto> getGroupsByStudentLastNameNative(String lastName, int page, int size) {
        GroupQueryKey key = new GroupQueryKey(lastName, page, size, "NATIVE");

        if (cache.containsKey(key)) {
            LOGGER.info(String.format("GROUP_NATIVE: FROM CACHE - lastName=%s, page=%d", lastName, page));
            return cache.get(key);
        }

        LOGGER.info(String.format("GROUP_NATIVE: FROM DATABASE - lastName=%s, page=%d", lastName, page));

        List<GroupResponseDto> result = repository
                .findByStudentLastNameNative(lastName, PageRequest.of(page, size))
                .map(GroupMapper::toDto)
                .toList();

        cache.put(key, result);
        return result;
    }

    @Transactional
    public void invalidateCache() {
        LOGGER.info("GROUP CACHE CLEARED");
        cache.clear();
    }
}