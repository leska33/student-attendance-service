package com.example.student.dto;

import java.util.List;

public class StudentCreateDto {

    private String fullName;
    private int attendanceCount;
    private double averageGrade;
    private Long groupId;
    private List<Long> disciplineIds;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAttendanceCount() {
        return attendanceCount;
    }

    public void setAttendanceCount(int attendanceCount) {
        this.attendanceCount = attendanceCount;
    }

    public double getAverageGrade() {
        return averageGrade;
    }

    public void setAverageGrade(double averageGrade) {
        this.averageGrade = averageGrade;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public List<Long> getDisciplineIds() {
        return disciplineIds;
    }

    public void setDisciplineIds(List<Long> disciplineIds) {
        this.disciplineIds = disciplineIds;
    }
}