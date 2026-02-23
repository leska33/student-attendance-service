package com.example.student.repository;

import com.example.student.entity.Student;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StudentRepository {
    private final List<Student> students = new ArrayList<>();
    public StudentRepository() {
        students.add(new Student("3580011",
                "Басюк Алеся Владимировна",
                "450502",
                12,
                7.5));
        students.add(new Student("3580024",
                "Усевич Кристина Дмитриевна",
                "450502",
                10,
                7.9));
        students.add(new Student("358120",
                "Троц Роман Николаевич",
                "450503",
                15,
                9.2));
    }
    public List<Student> findAll() {
        return students;
    }
}