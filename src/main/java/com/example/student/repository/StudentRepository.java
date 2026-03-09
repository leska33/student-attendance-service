package com.example.student.repository;

import com.example.student.entity.Student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Override
    List<Student> findAll();

    @EntityGraph(attributePaths = {"group", "disciplines"})
    @Query("select s from Student s")
    List<Student> findAllWithRelations();
}