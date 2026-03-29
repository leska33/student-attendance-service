package com.example.student.repository;

import com.example.student.entity.Discipline;
import com.example.student.entity.Grade;
import com.example.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    @EntityGraph(attributePaths = {"student", "discipline"})
    @Query("select g from Grade g")
    List<Grade> findAllWithRelations();

    void deleteAllByDiscipline(Discipline discipline);
    void deleteAllByStudent(Student student);

    @Query(value = """
                select g from Grade g
                join fetch g.student s
                join fetch g.discipline d
                where d.name = :disciplineName
            """)
    Page<Grade> findByDisciplineNameJPQL(String disciplineName, Pageable pageable);

    @Query(value = """
        SELECT DISTINCT g.*
        FROM grades g
        JOIN disciplines d ON g.discipline_id = d.id
        JOIN students s ON g.student_id = s.id
        WHERE d.name = :disciplineName
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM grades g
        JOIN disciplines d ON g.discipline_id = d.id
        WHERE d.name = :disciplineName
        """,
            nativeQuery = true)
    Page<Grade> findByDisciplineNameNative(String disciplineName, Pageable pageable);

    @Query(value = """
                select g from Grade g
                join fetch g.student s
                join fetch g.discipline d
                where s.lastName = :studentLastName
            """)
    Page<Grade> findByStudentLastNameJPQL(String studentLastName, Pageable pageable);

    @Query(value = """
        SELECT DISTINCT g.*
        FROM grades g
        JOIN students s ON g.student_id = s.id
        JOIN disciplines d ON g.discipline_id = d.id
        WHERE s.last_name = :studentLastName
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM grades g
        JOIN students s ON g.student_id = s.id
        WHERE s.last_name = :studentLastName
        """,
            nativeQuery = true)
    Page<Grade> findByStudentLastNameNative(String studentLastName, Pageable pageable);
}