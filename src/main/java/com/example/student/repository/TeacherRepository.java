package com.example.student.repository;

import com.example.student.entity.Teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    @EntityGraph(attributePaths = {"disciplines"})
    @Query("select t from Teacher t")
    List<Teacher> findAllWithDisciplines();
}