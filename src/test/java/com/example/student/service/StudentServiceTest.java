package com.example.student.service;

import com.example.student.dto.StudentCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Group;
import com.example.student.entity.Student;
import com.example.student.exception.AlreadyExistsException;
import com.example.student.exception.ResourceNotFoundException;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.GroupRepository;
import com.example.student.repository.StudentRepository;
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

class StudentServiceTest {

    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final DisciplineRepository disciplineRepository = mock(DisciplineRepository.class);
    private final GradeRepository gradeRepository = mock(GradeRepository.class);

    private final StudentService service =
            new StudentService(studentRepository, groupRepository, disciplineRepository, gradeRepository);

    private StudentCreateDto dto() {
        StudentCreateDto dto = new StudentCreateDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setMiddleName("C");
        dto.setGroupId(1L);
        dto.setDisciplineIds(List.of(1L));
        return dto;
    }

    @Test
    void getAllStudentsDtoLazy_mapsResults() {
        Student s = new Student();
        s.setFirstName("A");
        s.setLastName("B");
        when(studentRepository.findAll()).thenReturn(List.of(s));

        assertEquals(1, service.getAllStudentsDtoLazy().size());
    }

    @Test
    void getAllStudentsDtoOptimized_mapsResults() {
        Student s = new Student();
        s.setFirstName("A");
        s.setLastName("B");
        when(studentRepository.findAllWithRelations()).thenReturn(List.of(s));

        assertEquals(1, service.getAllStudentsDtoOptimized().size());
    }

    @Test
    void getStudentById_success() {
        Student s = new Student();
        s.setFirstName("A");
        s.setLastName("B");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        assertNotNull(service.getStudentById(1L));
    }

    @Test
    void getStudentById_notFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getStudentById(1L));
    }

    @Test
    void getStudentById_nullId_notFound() {
        when(studentRepository.findById(null)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getStudentById(null));
    }

    @Test
    void create_success() {
        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(new Group()));
        when(disciplineRepository.findAllById(any())).thenReturn(List.of(new Discipline()));
        when(studentRepository.save(any())).thenReturn(new Student());

        assertNotNull(service.createStudent(dto()));
    }

    @Test
    void create_duplicate() {
        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> service.createStudent(dto()));
    }

    @Test
    void create_groupNotFound() {
        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createStudent(dto()));
    }

    @Test
    void update_success() {
        Student student = new Student();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(new Group()));
        when(disciplineRepository.findAllById(any())).thenReturn(List.of(new Discipline()));
        when(studentRepository.save(any())).thenReturn(student);

        assertNotNull(service.updateStudent(1L, dto()));
    }

    @Test
    void update_notFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateStudent(1L, dto()));
    }

    @Test
    void delete_success_withoutRelations() {
        Student student = new Student();
        student.setDisciplines(new ArrayList<>());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.deleteStudent(1L);

        verify(gradeRepository).deleteAllByStudent(student);
        verify(studentRepository).delete(student);
    }

    @Test
    void delete_success_nullDisciplines() {
        Student student = new Student();
        student.setDisciplines(null);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.deleteStudent(1L);

        verify(studentRepository).delete(student);
    }

    @Test
    void delete_success_withGroupAndDisciplines() {
        Group group = new Group();
        group.setStudents(new ArrayList<>());

        Discipline d = new Discipline();
        d.setStudents(new ArrayList<>());

        Student student = new Student();
        student.setGroup(group);
        List<Discipline> discs = new ArrayList<>();
        discs.add(d);
        student.setDisciplines(discs);
        d.getStudents().add(student);
        group.getStudents().add(student);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.deleteStudent(1L);

        verify(studentRepository).delete(student);
    }

    @Test
    void createStudentsBulk_success() {
        when(studentRepository.existsByFirstNameAndLastNameAndMiddleName(any(), any(), any()))
                .thenReturn(false);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(new Group()));
        when(disciplineRepository.findAllById(any())).thenReturn(List.of(new Discipline()));
        when(studentRepository.save(any())).thenReturn(new Student());

        assertEquals(1, service.createStudentsBulk(List.of(dto())).size());
    }

    @Test
    void createStudentsBulk_nullList_yieldsEmpty() {
        assertTrue(service.createStudentsBulk(null).isEmpty());
    }

    @Test
    void createStudentsBulk_transactional_throwsOnErrorRow() {
        StudentCreateDto bad = dto();
        bad.setFirstName(BulkOperationConstants.ERROR_SENTINEL);

        assertThrows(IllegalStateException.class, () -> service.createStudentsBulk(List.of(bad)));
    }

    @Test
    void createStudentsBulkWithoutTransaction_throwsOnErrorRow() {
        StudentCreateDto bad = dto();
        bad.setFirstName(BulkOperationConstants.ERROR_SENTINEL);

        assertThrows(IllegalStateException.class,
                () -> service.createStudentsBulkWithoutTransaction(List.of(bad)));
    }

    @Test
    void createStudentsBulkWithoutTransaction_nullList_yieldsEmpty() {
        assertTrue(service.createStudentsBulkWithoutTransaction(null).isEmpty());
    }

    @Test
    void deleteStudent_notFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteStudent(99L));
    }

    @Test
    void updateStudent_groupNotFound() {
        Student student = new Student();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateStudent(1L, dto()));
    }

    @Test
    void deleteStudent_groupOnly_emptyDisciplines() {
        Group group = new Group();
        group.setStudents(new ArrayList<>());

        Student student = new Student();
        student.setGroup(group);
        student.setDisciplines(new ArrayList<>());
        group.getStudents().add(student);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.deleteStudent(1L);

        verify(studentRepository).delete(student);
    }
}
