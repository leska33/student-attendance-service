package com.example.student.repository;

import com.example.student.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    @EntityGraph(attributePaths = {"disciplines"})
    @Query("select t from Teacher t")
    List<Teacher> findAllWithDisciplines();

    @Query("select t from Teacher t join t.disciplines d where d.name = :disciplineName")
    Page<Teacher> findByDisciplineNameJPQL(String disciplineName, Pageable pageable);

    @Query(value = "SELECT t.* FROM teachers t " +
            "JOIN disciplines d ON t.id = d.teacher_id " +
            "WHERE d.name = :disciplineName",
            countQuery = "SELECT COUNT(*) FROM teachers t JOIN disciplines d ON t.id = d.teacher_id WHERE d.name = :disciplineName",
            nativeQuery = true)
    Page<Teacher> findByDisciplineNameNative(String disciplineName, Pageable pageable);
}