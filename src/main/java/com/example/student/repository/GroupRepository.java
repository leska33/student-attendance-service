package com.example.student.repository;

import com.example.student.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Override
    List<Group> findAll();

    @EntityGraph(attributePaths = {"students"})
    @Query("select g from Group g")
    List<Group> findAllWithStudents();

    @Query("select g from Group g join g.students s where s.lastName = :studentLastName")
    Page<Group> findByStudentLastNameJPQL(String studentLastName, Pageable pageable);

    @Query(value = "SELECT g.* FROM groups g " +
            "JOIN students s ON g.id = s.group_id " +
            "WHERE s.last_name = :studentLastName",
            countQuery = "SELECT COUNT(*) FROM groups g JOIN students s ON g.id = s.group_id WHERE s.last_name = :studentLastName",
            nativeQuery = true)
    Page<Group> findByStudentLastNameNative(String studentLastName, Pageable pageable);
}