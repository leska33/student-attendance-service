package com.example.student.service;

import com.example.student.entity.Group;
import com.example.student.repository.GroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupQueryServiceTest {

    private final GroupRepository repository = mock(GroupRepository.class);
    private final GroupQueryService service = new GroupQueryService(repository);

    @Test
    void getGroupsByStudentLastNameJPQL_cache() {
        Group g = new Group();
        when(repository.findByStudentLastNameJPQL(eq("Ivanov"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(g)));

        service.getGroupsByStudentLastNameJPQL("Ivanov", 0, 4);
        service.getGroupsByStudentLastNameJPQL("Ivanov", 0, 4);

        verify(repository, times(1)).findByStudentLastNameJPQL("Ivanov", PageRequest.of(0, 4));
    }

    @Test
    void getGroupsByStudentLastNameNative_cache() {
        Group g = new Group();
        when(repository.findByStudentLastNameNative(eq("Petrov"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(g)));

        service.getGroupsByStudentLastNameNative("Petrov", 0, 4);
        service.getGroupsByStudentLastNameNative("Petrov", 0, 4);

        verify(repository, times(1)).findByStudentLastNameNative("Petrov", PageRequest.of(0, 4));
    }

    @Test
    void invalidateCache() {
        Group g = new Group();
        when(repository.findByStudentLastNameJPQL(eq("Z"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(g)));

        service.getGroupsByStudentLastNameJPQL("Z", 0, 1);
        service.invalidateCache();
        service.getGroupsByStudentLastNameJPQL("Z", 0, 1);

        verify(repository, times(2)).findByStudentLastNameJPQL("Z", PageRequest.of(0, 1));
    }

    @Test
    void invalidateCache_clearsNativePath() {
        Group g = new Group();
        when(repository.findByStudentLastNameNative(eq("N1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(g)));

        service.getGroupsByStudentLastNameNative("N1", 0, 1);
        service.invalidateCache();
        service.getGroupsByStudentLastNameNative("N1", 0, 1);

        verify(repository, times(2)).findByStudentLastNameNative("N1", PageRequest.of(0, 1));
    }

    @Test
    void saveOrUpdate() {
        Group in = new Group();
        Group out = new Group();
        when(repository.save(in)).thenReturn(out);

        assertEquals(out, service.saveOrUpdate(in));
    }

    @Test
    void delete() {
        Group g = new Group();
        service.delete(g);
        verify(repository).delete(g);
    }
}
