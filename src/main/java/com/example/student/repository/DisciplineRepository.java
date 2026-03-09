package com.example.student.repository;

import com.example.student.entity.Discipline;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DisciplineRepository extends JpaRepository<Discipline, Long> {

    @EntityGraph(attributePaths = {"teacher", "students"})
    @Query("select d from Discipline d")
    List<Discipline> findAllWithRelations();
}