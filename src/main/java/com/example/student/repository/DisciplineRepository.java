package com.example.student.repository;

import com.example.student.entity.Discipline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DisciplineRepository extends JpaRepository<Discipline, Long> {

    @EntityGraph(attributePaths = {"teacher", "students"})
    @Query("select d from Discipline d")
    List<Discipline> findAllWithRelations();

    @Query(value = """
            select distinct d from Discipline d
            join fetch d.teacher t
            left join fetch d.students s
            where t.firstName = :firstName
            and t.middleName = :middleName
            and t.lastName = :lastName
            """)
    Page<Discipline> findByTeacherFullNameJPQL(
            @Param("firstName") String firstName,
            @Param("middleName") String middleName,
            @Param("lastName") String lastName,
            Pageable pageable
    );

    @SuppressWarnings("checkstyle:Indentation")
    @Query(value = "SELECT d.* FROM disciplines d\n" +
                   "JOIN teachers t ON d.teacher_id = t.id\n" +
                   "LEFT JOIN student_discipline sd ON d.id = sd.discipline_id\n" +
                   "LEFT JOIN students s ON sd.student_id = s.id\n" +
                   "WHERE t.first_name = :firstName\n" +
                   "AND t.middle_name = :middleName\n" +
                   "AND t.last_name = :lastName\n",
            countQuery = "SELECT COUNT(*) FROM disciplines d\n" +
                         "JOIN teachers t ON d.teacher_id = t.id\n" +
                         "WHERE t.first_name = :firstName\n" +
                         "AND t.middle_name = :middleName\n" +
                         "AND t.last_name = :lastName\n",
            nativeQuery = true)
    Page<Discipline> findByTeacherFullNameNative(
            @Param("firstName") String firstName,
            @Param("middleName") String middleName,
            @Param("lastName") String lastName,
            Pageable pageable
    );
}