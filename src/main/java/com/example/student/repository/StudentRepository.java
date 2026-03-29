package com.example.student.repository;

import com.example.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = {"group", "disciplines"})
    @Query("select s from Student s")
    List<Student> findAllWithRelations();

    @Query(value = """
                select distinct s from Student s
                join fetch s.group g
                left join fetch s.disciplines d
                where d.name = :disciplineName
            """)
    Page<Student> findByDisciplineNameJPQL(String disciplineName, Pageable pageable);

    @Query(value = """
        SELECT DISTINCT s.*
        FROM students s
        JOIN groups g ON s.group_id = g.id
        JOIN student_discipline sd ON s.id = sd.student_id
        JOIN disciplines d ON sd.discipline_id = d.id
        WHERE d.name = :disciplineName
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM students s
        JOIN student_discipline sd ON s.id = sd.student_id
        JOIN disciplines d ON sd.discipline_id = d.id
        WHERE d.name = :disciplineName
        """,
            nativeQuery = true)
    Page<Student> findByDisciplineNameNative(String disciplineName, Pageable pageable);

    @Query(value = """
                select s from Student s
                join fetch s.group g
                left join fetch s.disciplines d
                where g.number = :groupNumber
            """)
    Page<Student> findByGroupNumberJPQL(String groupNumber, Pageable pageable);

    @Query(value = """
        SELECT DISTINCT s.*
        FROM students s
        JOIN groups g ON s.group_id = g.id
        LEFT JOIN student_discipline sd ON s.id = sd.student_id
        LEFT JOIN disciplines d ON sd.discipline_id = d.id
        WHERE g.number = :groupNumber
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM students s
        JOIN groups g ON s.group_id = g.id
        WHERE g.number = :groupNumber
        """,
            nativeQuery = true)
    Page<Student> findByGroupNumberNative(String groupNumber, Pageable pageable);
}