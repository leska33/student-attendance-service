package com.example.student.service;

import com.example.student.entity.Discipline;
import com.example.student.repository.DisciplineRepository;
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

class DisciplineQueryServiceTest {

    private final DisciplineRepository repository = mock(DisciplineRepository.class);
    private final DisciplineQueryService service = new DisciplineQueryService(repository);

    @Test
    void getDisciplinesByTeacherJPQL_cache() {
        Discipline d = new Discipline();
        when(repository.findByTeacherFullNameJPQL(eq("A"), eq("B"), eq("C"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(d)));

        service.getDisciplinesByTeacherJPQL("A", "B", "C", 0, 2);
        service.getDisciplinesByTeacherJPQL("A", "B", "C", 0, 2);

        verify(repository, times(1)).findByTeacherFullNameJPQL(
                eq("A"), eq("B"), eq("C"), eq(PageRequest.of(0, 2)));
    }

    @Test
    void getDisciplinesByTeacherNative_cache() {
        Discipline d = new Discipline();
        when(repository.findByTeacherFullNameNative(eq("A"), eq("B"), eq("C"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(d)));

        service.getDisciplinesByTeacherNative("A", "B", "C", 0, 2);
        service.getDisciplinesByTeacherNative("A", "B", "C", 0, 2);

        verify(repository, times(1)).findByTeacherFullNameNative(
                eq("A"), eq("B"), eq("C"), eq(PageRequest.of(0, 2)));
    }

    @Test
    void invalidateCache() {
        Discipline d = new Discipline();
        when(repository.findByTeacherFullNameJPQL(eq("X"), eq("Y"), eq("Z"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(d)));

        service.getDisciplinesByTeacherJPQL("X", "Y", "Z", 0, 1);
        service.invalidateCache();
        service.getDisciplinesByTeacherJPQL("X", "Y", "Z", 0, 1);

        verify(repository, times(2)).findByTeacherFullNameJPQL(
                eq("X"), eq("Y"), eq("Z"), eq(PageRequest.of(0, 1)));
    }

    @Test
    void invalidateCache_clearsNativePath() {
        Discipline d = new Discipline();
        when(repository.findByTeacherFullNameNative(eq("N"), eq("A"), eq("T"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(d)));

        service.getDisciplinesByTeacherNative("N", "A", "T", 0, 1);
        service.invalidateCache();
        service.getDisciplinesByTeacherNative("N", "A", "T", 0, 1);

        verify(repository, times(2)).findByTeacherFullNameNative(
                eq("N"), eq("A"), eq("T"), eq(PageRequest.of(0, 1)));
    }

    @Test
    void saveOrUpdate() {
        Discipline in = new Discipline();
        Discipline out = new Discipline();
        when(repository.save(in)).thenReturn(out);

        assertEquals(out, service.saveOrUpdate(in));
    }

    @Test
    void delete() {
        Discipline d = new Discipline();
        service.delete(d);
        verify(repository).delete(d);
    }
}
