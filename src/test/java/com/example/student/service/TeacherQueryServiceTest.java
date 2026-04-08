package com.example.student.service;

import com.example.student.entity.Teacher;
import com.example.student.repository.TeacherRepository;
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

class TeacherQueryServiceTest {

    private final TeacherRepository repository = mock(TeacherRepository.class);
    private final TeacherQueryService service = new TeacherQueryService(repository);

    @Test
    void getTeachersByDisciplineJPQL_cache() {
        Teacher t = new Teacher();
        when(repository.findByDisciplineNameJPQL(eq("Alg"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        service.getTeachersByDisciplineJPQL("Alg", 0, 3);
        service.getTeachersByDisciplineJPQL("Alg", 0, 3);

        verify(repository, times(1)).findByDisciplineNameJPQL(eq("Alg"), eq(PageRequest.of(0, 3)));
    }

    @Test
    void getTeachersByDisciplineNative_cache() {
        Teacher t = new Teacher();
        when(repository.findByDisciplineNameNative(eq("Geom"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        service.getTeachersByDisciplineNative("Geom", 0, 3);
        service.getTeachersByDisciplineNative("Geom", 0, 3);

        verify(repository, times(1)).findByDisciplineNameNative(eq("Geom"), eq(PageRequest.of(0, 3)));
    }

    @Test
    void invalidateCache() {
        Teacher t = new Teacher();
        when(repository.findByDisciplineNameJPQL(eq("Q"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        service.getTeachersByDisciplineJPQL("Q", 0, 1);
        service.invalidateCache();
        service.getTeachersByDisciplineJPQL("Q", 0, 1);

        verify(repository, times(2)).findByDisciplineNameJPQL(eq("Q"), eq(PageRequest.of(0, 1)));
    }

    @Test
    void invalidateCache_clearsNativePath() {
        Teacher t = new Teacher();
        when(repository.findByDisciplineNameNative(eq("Ntv"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        service.getTeachersByDisciplineNative("Ntv", 0, 1);
        service.invalidateCache();
        service.getTeachersByDisciplineNative("Ntv", 0, 1);

        verify(repository, times(2)).findByDisciplineNameNative(eq("Ntv"), eq(PageRequest.of(0, 1)));
    }

    @Test
    void saveOrUpdate() {
        Teacher in = new Teacher();
        Teacher out = new Teacher();
        when(repository.save(in)).thenReturn(out);

        assertEquals(out, service.saveOrUpdate(in));
    }

    @Test
    void delete() {
        Teacher t = new Teacher();
        service.delete(t);
        verify(repository).delete(t);
    }
}
