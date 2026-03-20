package com.example.student.cache;

import java.util.Objects;

public class TeacherQueryKey {

    private final String disciplineName;
    private final int page;
    private final int size;
    private final String queryType;

    public TeacherQueryKey(String disciplineName, int page, int size, String queryType) {
        this.disciplineName = disciplineName;
        this.page = page;
        this.size = size;
        this.queryType = queryType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TeacherQueryKey)) {
            return false;
        }
        TeacherQueryKey that = (TeacherQueryKey) o;
        return page == that.page &&
                size == that.size &&
                Objects.equals(disciplineName, that.disciplineName) &&
                Objects.equals(queryType, that.queryType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disciplineName, page, size, queryType);
    }
}