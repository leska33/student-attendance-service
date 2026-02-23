package com.example.student.mapper;

import com.example.student.dto.StudentResponseDto;
import com.example.student.entity.Student;

public class StudentMapper {
    public static StudentResponseDto toDto(Student student) {
        if (student == null) {
            return null;
        }
        return new StudentResponseDto(student.getStudentId(), student.getFullName(), student.getGroupNumber(), student.getAttendanceCount(), student.getAverageGrade());
    }
}