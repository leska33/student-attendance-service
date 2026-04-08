package com.example.student.service;

import com.example.student.entity.Student;
import com.example.student.repository.StudentRepository;
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

class StudentQueryServiceTest {

    private final StudentRepository repository = mock(StudentRepository.class);
    private final StudentQueryService service = new StudentQueryService(repository);

    @Test
    void getStudentsByDisciplineJPQL_usesCacheOnSecondCall() {
        Student student = new Student();
        student.setFirstName("A");
        student.setLastName("B");
        when(repository.findByDisciplineNameJPQL(eq("Math"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student)));

        service.getStudentsByDisciplineJPQL("Math", 0, 5);
        service.getStudentsByDisciplineJPQL("Math", 0, 5);

        verify(repository, times(1)).findByDisciplineNameJPQL(eq("Math"), eq(PageRequest.of(0, 5)));
    }

    @Test
    void getStudentsByDisciplineNative_usesCacheOnSecondCall() {
        Student student = new Student();
        student.setFirstName("A");
        student.setLastName("B");
        when(repository.findByDisciplineNameNative(eq("Physics"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student)));

        service.getStudentsByDisciplineNative("Physics", 1, 3);
        service.getStudentsByDisciplineNative("Physics", 1, 3);

        verify(repository, times(1)).findByDisciplineNameNative(eq("Physics"), eq(PageRequest.of(1, 3)));
    }

    @Test
    void invalidateCache_clearsJpqlPath() {
        Student student = new Student();
        student.setFirstName("A");
        student.setLastName("B");
        when(repository.findByDisciplineNameJPQL(eq("X"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student)));

        service.getStudentsByDisciplineJPQL("X", 0, 2);
        service.invalidateCache();
        service.getStudentsByDisciplineJPQL("X", 0, 2);

        verify(repository, times(2)).findByDisciplineNameJPQL(eq("X"), eq(PageRequest.of(0, 2)));
    }

    @Test
    void invalidateCache_clearsNativePath() {
        Student student = new Student();
        student.setFirstName("A");
        student.setLastName("B");
        when(repository.findByDisciplineNameNative(eq("Nat"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student)));

        service.getStudentsByDisciplineNative("Nat", 0, 2);
        service.invalidateCache();
        service.getStudentsByDisciplineNative("Nat", 0, 2);

        verify(repository, times(2)).findByDisciplineNameNative(eq("Nat"), eq(PageRequest.of(0, 2)));
    }

    @Test
    void saveOrUpdate_invalidatesAndSaves() {
        Student in = new Student();
        Student out = new Student();
        when(repository.save(in)).thenReturn(out);

        assertEquals(out, service.saveOrUpdate(in));
    }

    @Test
    void delete_delegates() {
        Student student = new Student();
        service.delete(student);
        verify(repository).delete(student);
    }
}
