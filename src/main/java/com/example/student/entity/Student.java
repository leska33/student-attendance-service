package com.example.student.entity;

public class Student {

    private String studentId;      // 7 цифр
    private String fullName;       // ФИО
    private String groupNumber;    // 6 цифр
    private int attendanceCount;
    private double averageGrade;

    public Student(String studentId, String fullName, String groupNumber, int attendanceCount, double averageGrade)
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