package com.example.student.repository;

import com.example.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Override
    List<Student> findAll();

    @EntityGraph(attributePaths = {"group", "disciplines"})
    @Query("select s from Student s")
    List<Student> findAllWithRelations();

    @Query("select s from Student s join s.disciplines d where d.name = :disciplineName")
    Page<Student> findByDisciplineNameJPQL(String disciplineName, Pageable pageable);

    @Query(value = "SELECT s.* FROM students s " +
            "JOIN student_discipline sd ON s.id = sd.student_id " +
            "JOIN disciplines d ON sd.discipline_id = d.id " +
            "WHERE d.name = :disciplineName",
            countQuery = "SELECT COUNT(*) FROM students s " +
                    "JOIN student_discipline sd ON s.id = sd.student_id " +
                    "JOIN disciplines d ON sd.discipline_id = d.id " +
                    "WHERE d.name = :disciplineName",
            nativeQuery = true)
    Page<Student> findByDisciplineNameNative(String disciplineName, Pageable pageable);

    @Query("select s from Student s join s.group g where g.number = :groupNumber")
    Page<Student> findByGroupNumberJPQL(String groupNumber, Pageable pageable);

    @Query(value = "SELECT s.* FROM students s " +
            "JOIN groups g ON s.group_id = g.id " +
            "WHERE g.number = :groupNumber",
            countQuery = "SELECT COUNT(*) FROM students s " +
                    "JOIN groups g ON s.group_id = g.id " +
                    "WHERE g.number = :groupNumber",
            nativeQuery = true)
    Page<Student> findByGroupNumberNative(String groupNumber, Pageable pageable);
}