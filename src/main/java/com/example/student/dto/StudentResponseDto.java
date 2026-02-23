package com.example.student.dto;

public class StudentResponseDto {

    private String studentId;
    private String fullName;
    private String groupNumber;
    private int attendanceCount;
    private double averageGrade;

    public StudentResponseDto() {
    }

    public StudentResponseDto(String studentId, String fullName, String groupNumber, int attendanceCount, double averageGrade)
    {
        this.studentId = studentId;
        this.fullName = fullName;
        this.groupNumber = groupNumber;
        this.attendanceCount = attendanceCount;
        this.averageGrade = averageGrade;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGroupNumber() {
        return groupNumber;
    }

    public int getAttendanceCount() {
        return attendanceCount;
    }

    public double getAverageGrade() {
        return averageGrade;
    }
}