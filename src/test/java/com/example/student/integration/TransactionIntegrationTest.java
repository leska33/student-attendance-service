package com.example.student.integration;

import com.example.student.dto.StudentCreateDto;
import com.example.student.entity.Group;
import com.example.student.repository.GroupRepository;
import com.example.student.repository.StudentRepository;
import com.example.student.service.StudentService;
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
 * Сравнение bulk-создания студентов с транзакцией ({@code createStudentsBulk}) и без неё
 * ({@code createStudentsBulkWithoutTransaction}): при ошибке на второй записи (имя {@code ERROR})
 * в транзакционном варианте счётчик студентов остаётся 0 (откат), без транзакции — первая запись
 * уже закоммичена, поэтому {@code count() > 0}.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GroupRepository groupRepository;

    private Long groupId;

    @BeforeEach
    void setup() {
        studentRepository.deleteAll();
        groupRepository.deleteAll();

        Group group = new Group();
        group.setNumber("G1");
        group = groupRepository.save(group);

        groupId = group.getId();
    }

    private StudentCreateDto normalDto(String name) {
        StudentCreateDto dto = new StudentCreateDto();
        dto.setFirstName(name);
        dto.setLastName("Test");
        dto.setMiddleName("T");
        dto.setGroupId(groupId);
        dto.setDisciplineIds(List.of());
        return dto;
    }

    private StudentCreateDto errorDto() {
        return normalDto("ERROR");
    }

    @Test
    void transactional_bulk_shouldRollbackAll() {
        List<StudentCreateDto> list = List.of(
                normalDto("A"),
                errorDto(),
                normalDto("B")
        );

        assertThrows(IllegalStateException.class,
                () -> studentService.createStudentsBulk(list));

        assertEquals(0, studentRepository.count());
    }

    @Test
    void nonTransactional_bulk_shouldSavePartial() {
        List<StudentCreateDto> list = List.of(
                normalDto("A"),
                errorDto(),
                normalDto("B")
        );

        assertThrows(IllegalStateException.class,
                () -> studentService.createStudentsBulkWithoutTransaction(list));

        assertTrue(studentRepository.count() > 0);
    }
}