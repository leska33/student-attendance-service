package com.example.student.dto;

import java.util.List;

public class StudentCreateDto {

    private String firstName;
    private String lastName;
    private String middleName;

    private int attendanceCount;

    private Long groupId;
    private List<Long> disciplineIds;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public int getAttendanceCount() {
        return attendanceCount;
    }

    public void setAttendanceCount(int attendanceCount) {
        this.attendanceCount = attendanceCount;
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