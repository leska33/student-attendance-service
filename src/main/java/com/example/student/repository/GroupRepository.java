package com.example.student.repository;

import com.example.student.entity.Group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Override
    List<Group> findAll();

    @EntityGraph(attributePaths = {"students"})
    @Query("select g from Group g")
    List<Group> findAllWithStudents();
}