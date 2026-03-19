package com.example.student.cache;

import java.util.Objects;

public class GradeQueryKey {

    private final String studentLastName;
    private final String disciplineName;
    private final int page;
    private final int size;

    public GradeQueryKey(String studentLastName, String disciplineName, int page, int size) {
        this.studentLastName = studentLastName;
        this.disciplineName = disciplineName;
        this.page = page;
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GradeQueryKey)) return false;
        GradeQueryKey that = (GradeQueryKey) o;
        return page == that.page &&
                size == that.size &&
                Objects.equals(studentLastName, that.studentLastName) &&
                Objects.equals(disciplineName, that.disciplineName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentLastName, disciplineName, page, size);
    }
}