package com.example.student.repository;

import com.example.student.entity.Discipline;
import com.example.student.entity.Grade;
import com.example.student.entity.Student;

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
}