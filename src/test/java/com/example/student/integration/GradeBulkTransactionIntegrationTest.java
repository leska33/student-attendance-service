package com.example.student.integration;

import com.example.student.dto.GradeCreateDto;
import com.example.student.entity.Discipline;
import com.example.student.entity.Group;
import com.example.student.entity.Student;
import com.example.student.entity.Teacher;
import com.example.student.repository.DisciplineRepository;
import com.example.student.repository.GradeRepository;
import com.example.student.repository.GroupRepository;
import com.example.student.repository.StudentRepository;
import com.example.student.repository.TeacherRepository;
import com.example.student.service.GradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Демонстрация транзакционности bulk-импорта оценок: с {@code @Transactional} при сбое на одной
 * записи откатывается вся пачка; без транзакции часть строк успевает сохраниться в БД.
 */
@SpringBootTest
@ActiveProfiles("test")
class GradeBulkTransactionIntegrationTest {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private Long studentId;
    private Long disciplineId;

    @BeforeEach
    void setup() {
        gradeRepository.deleteAll();
        studentRepository.deleteAll();
        disciplineRepository.deleteAll();
        groupRepository.deleteAll();
        teacherRepository.deleteAll();

        Group group = new Group();
        group.setNumber("G-BULK");
        group = groupRepository.save(group);

        Teacher teacher = new Teacher();
        teacher.setFirstName("T");
        teacher.setLastName("T");
        teacher.setMiddleName("T");
        teacher = teacherRepository.save(teacher);

        Discipline discipline = new Discipline();
        discipline.setName("Math");
        discipline.setTeacher(teacher);
        discipline = disciplineRepository.save(discipline);

        Student student = new Student();
        student.setFirstName("S");
        student.setLastName("S");
        student.setMiddleName("S");
        student.setGroup(group);
        student.setDisciplines(List.of());
        student = studentRepository.save(student);

        studentId = student.getId();
        disciplineId = discipline.getId();
    }

    private GradeCreateDto okGrade(int value) {
        GradeCreateDto dto = new GradeCreateDto();
        dto.setValue(value);
        dto.setStudentId(studentId);
        dto.setDisciplineId(disciplineId);
        return dto;
    }

    private GradeCreateDto badGrade() {
        GradeCreateDto dto = okGrade(5);
        dto.setValue(-1);
        return dto;
    }

    @Test
    void transactional_bulk_shouldRollbackAll() {
        List<GradeCreateDto> list = List.of(okGrade(5), badGrade(), okGrade(7));

        assertThrows(IllegalStateException.class, () -> gradeService.createGradesBulk(list));

        assertEquals(0, gradeRepository.count());
    }

    @Test
    void nonTransactional_bulk_shouldSavePartial() {
        List<GradeCreateDto> list = List.of(okGrade(5), badGrade(), okGrade(7));

        assertThrows(IllegalStateException.class,
                () -> gradeService.createGradesBulkWithoutTransaction(list));

        assertTrue(gradeRepository.count() > 0);
    }
}
