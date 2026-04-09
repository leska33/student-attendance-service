package com.example.student.service;

import com.example.student.entity.Discipline;
import com.example.student.entity.Grade;
import com.example.student.entity.Student;
import com.example.student.repository.GradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GradeQueryServiceTest {

    private final GradeRepository repository = mock(GradeRepository.class);
    private final GradeQueryService service = new GradeQueryService(repository);

    private Grade gradeWithDiscipline(String disciplineName) {
        Grade g = new Grade();
        Student s = new Student();
        s.setLastName("Ivanov");
        g.setStudent(s);
        Discipline d = new Discipline();
        d.setName(disciplineName);
        g.setDiscipline(d);
        return g;
    }

    @Test
    void getGradesByStudentAndDisciplineJPQL_filtersByDiscipline_andCaches() {
        Grade match = gradeWithDiscipline("Math");
        when(repository.findByStudentLastNameJPQL(eq("Ivanov"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        var first = service.getGradesByStudentAndDisciplineJPQL("Ivanov", "Math", 0, 5);
        var second = service.getGradesByStudentAndDisciplineJPQL("Ivanov", "Math", 0, 5);

        assertEquals(1, first.size());
        assertEquals(first, second);
        verify(repository, times(1)).findByStudentLastNameJPQL("Ivanov", PageRequest.of(0, 5, Sort.by("id")));
    }

    @Test
    void getGradesByStudentAndDisciplineJPQL_excludesWrongDiscipline() {
        Grade other = gradeWithDiscipline("Physics");
        when(repository.findByStudentLastNameJPQL(eq("Ivanov"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(other)));

        assertTrue(service.getGradesByStudentAndDisciplineJPQL("Ivanov", "Math", 0, 5).isEmpty());
    }

    @Test
    void getGradesByStudentAndDisciplineJPQL_nullDiscipline_filteredOut() {
        Grade g = new Grade();
        Student s = new Student();
        s.setLastName("Ivanov");
        g.setStudent(s);
        g.setDiscipline(null);
        when(repository.findByStudentLastNameJPQL(eq("Ivanov"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(g)));

        assertTrue(service.getGradesByStudentAndDisciplineJPQL("Ivanov", "Math", 0, 5).isEmpty());
    }

    @Test
    void getGradesByStudentAndDisciplineNative_filtersAndCaches() {
        Grade match = gradeWithDiscipline("Chem");
        when(repository.findByStudentLastNameNative(eq("Petrov"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        service.getGradesByStudentAndDisciplineNative("Petrov", "Chem", 0, 4);
        service.getGradesByStudentAndDisciplineNative("Petrov", "Chem", 0, 4);

        verify(repository, times(1)).findByStudentLastNameNative("Petrov", PageRequest.of(0, 4, Sort.by("id")));
    }

    @Test
    void invalidateCache() {
        Grade match = gradeWithDiscipline("Bio");
        when(repository.findByStudentLastNameJPQL(eq("S"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        service.getGradesByStudentAndDisciplineJPQL("S", "Bio", 0, 1);
        service.invalidateCache();
        service.getGradesByStudentAndDisciplineJPQL("S", "Bio", 0, 1);

        verify(repository, times(2)).findByStudentLastNameJPQL("S", PageRequest.of(0, 1, Sort.by("id")));
    }

    @Test
    void invalidateCache_clearsNativePath() {
        Grade match = gradeWithDiscipline("Zoo");
        when(repository.findByStudentLastNameNative(eq("N2"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(match)));

        service.getGradesByStudentAndDisciplineNative("N2", "Zoo", 0, 1);
        service.invalidateCache();
        service.getGradesByStudentAndDisciplineNative("N2", "Zoo", 0, 1);

        verify(repository, times(2)).findByStudentLastNameNative("N2", PageRequest.of(0, 1, Sort.by("id")));
    }

    @Test
    void getGradesByStudentAndDisciplineNative_excludesWrongDiscipline() {
        Grade other = gradeWithDiscipline("X");
        when(repository.findByStudentLastNameNative(eq("Petrov"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(other)));

        assertTrue(service.getGradesByStudentAndDisciplineNative("Petrov", "Y", 0, 5).isEmpty());
    }

    @Test
    void saveOrUpdate() {
        Grade in = new Grade();
        Grade out = new Grade();
        when(repository.save(in)).thenReturn(out);

        assertEquals(out, service.saveOrUpdate(in));
    }

    @Test
    void delete() {
        Grade g = new Grade();
        service.delete(g);
        verify(repository).delete(g);
    }
}
