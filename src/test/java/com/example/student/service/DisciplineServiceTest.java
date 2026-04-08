package com.example.student.service;

import com.example.student.dto.DisciplineCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Student;
import com.example.student.entity.Teacher;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.TeacherRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisciplineServiceTest {

    private final DisciplineRepository disciplineRepository = mock(DisciplineRepository.class);
    private final TeacherRepository teacherRepository = mock(TeacherRepository.class);
    private final GradeRepository gradeRepository = mock(GradeRepository.class);

    private final DisciplineService service =
            new DisciplineService(disciplineRepository, teacherRepository, gradeRepository);

    private DisciplineCreateDto dto() {
        DisciplineCreateDto dto = new DisciplineCreateDto();
        dto.setName("Math");
        dto.setTeacherId(1L);
        return dto;
    }

    @Test
    void getAllDisciplinesDtoLazy_maps() {
        when(disciplineRepository.findAll()).thenReturn(List.of(new Discipline()));

        assertEquals(1, service.getAllDisciplinesDtoLazy().size());
    }

    @Test
    void getAllDisciplinesDtoOptimized_maps() {
        when(disciplineRepository.findAllWithRelations()).thenReturn(List.of(new Discipline()));

        assertEquals(1, service.getAllDisciplinesDtoOptimized().size());
    }

    @Test
    void getDisciplineById_success() {
        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(new Discipline()));

        assertNotNull(service.getDisciplineById(1L));
    }

    @Test
    void getDisciplineById_notFound() {
        when(disciplineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDisciplineById(1L));
    }

    @Test
    void getDisciplineById_nullId() {
        when(disciplineRepository.findById(null)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDisciplineById(null));
    }

    @Test
    void createDiscipline_success() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(new Teacher()));
        Discipline saved = new Discipline();
        saved.setName("Math");
        when(disciplineRepository.save(any())).thenReturn(saved);

        assertNotNull(service.createDiscipline(dto()));
    }

    @Test
    void createDiscipline_teacherNotFound() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        DisciplineCreateDto create = dto();
        assertThrows(ResourceNotFoundException.class, () -> service.createDiscipline(create));
    }

    @Test
    void updateDiscipline_success() {
        Discipline discipline = new Discipline();
        Teacher teacher = new Teacher();
        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(discipline));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(disciplineRepository.save(any())).thenReturn(discipline);

        assertNotNull(service.updateDiscipline(1L, dto()));
    }

    @Test
    void updateDiscipline_disciplineNotFound() {
        when(disciplineRepository.findById(1L)).thenReturn(Optional.empty());

        DisciplineCreateDto update = dto();
        assertThrows(ResourceNotFoundException.class, () -> service.updateDiscipline(1L, update));
    }

    @Test
    void updateDiscipline_teacherNotFound() {
        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(new Discipline()));
        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        DisciplineCreateDto update = dto();
        assertThrows(ResourceNotFoundException.class, () -> service.updateDiscipline(1L, update));
    }

    @Test
    void deleteDiscipline_success_minimal() {
        Discipline discipline = new Discipline();
        discipline.setStudents(new ArrayList<>());
        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(discipline));

        service.deleteDiscipline(1L);

        verify(gradeRepository).deleteAllByDiscipline(discipline);
        verify(disciplineRepository).delete(discipline);
    }

    @Test
    void deleteDiscipline_nullStudents() {
        Discipline discipline = new Discipline();
        discipline.setStudents(null);
        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(discipline));

        service.deleteDiscipline(1L);

        verify(disciplineRepository).delete(discipline);
    }

    @Test
    void deleteDiscipline_withStudentsAndTeacher() {
        Teacher teacher = new Teacher();
        teacher.setDisciplines(new ArrayList<>());

        Discipline discipline = new Discipline();
        discipline.setTeacher(teacher);
        List<Student> st = new ArrayList<>();
        Student student = new Student();
        List<Discipline> sd = new ArrayList<>();
        sd.add(discipline);
        student.setDisciplines(sd);
        st.add(student);
        discipline.setStudents(st);
        teacher.getDisciplines().add(discipline);

        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(discipline));

        service.deleteDiscipline(1L);

        verify(disciplineRepository).delete(discipline);
    }

    @Test
    void deleteDiscipline_notFound() {
        when(disciplineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteDiscipline(1L));
    }

    @Test
    void createDisciplinesBulk_success() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(new Teacher()));
        Discipline saved = new Discipline();
        when(disciplineRepository.save(any())).thenReturn(saved);

        assertEquals(1, service.createDisciplinesBulk(List.of(dto())).size());
    }

    @Test
    void createDisciplinesBulk_null_empty() {
        assertTrue(service.createDisciplinesBulk(null).isEmpty());
    }

    @Test
    void createDisciplinesBulk_transactional_error() {
        DisciplineCreateDto bad = dto();
        bad.setName(BulkOperationConstants.ERROR_SENTINEL);

        List<DisciplineCreateDto> bulk = List.of(bad);
        assertThrows(IllegalStateException.class, () -> service.createDisciplinesBulk(bulk));
    }

    @Test
    void createDisciplinesBulkWithoutTransaction_error() {
        DisciplineCreateDto bad = dto();
        bad.setName(BulkOperationConstants.ERROR_SENTINEL);

        List<DisciplineCreateDto> bulk = List.of(bad);
        assertThrows(IllegalStateException.class,
                () -> service.createDisciplinesBulkWithoutTransaction(bulk));
    }

    @Test
    void createDisciplinesBulkWithoutTransaction_null_empty() {
        assertTrue(service.createDisciplinesBulkWithoutTransaction(null).isEmpty());
    }

    @Test
    void deleteDiscipline_studentsNonNull_teacherNull() {
        Discipline discipline = new Discipline();
        discipline.setTeacher(null);
        List<Student> st = new ArrayList<>();
        Student student = new Student();
        List<Discipline> sd = new ArrayList<>();
        sd.add(discipline);
        student.setDisciplines(sd);
        st.add(student);
        discipline.setStudents(st);

        when(disciplineRepository.findById(1L)).thenReturn(Optional.of(discipline));

        service.deleteDiscipline(1L);

        verify(disciplineRepository).delete(discipline);
    }
}
