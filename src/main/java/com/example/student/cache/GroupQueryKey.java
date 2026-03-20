package com.example.student.cache;

import java.util.Objects;

public class GroupQueryKey {

    private final String studentLastName;
    private final int page;
    private final int size;
    private final String queryType;

    public GroupQueryKey(String studentLastName, int page, int size, String queryType) {
        this.studentLastName = studentLastName;
        this.page = page;
        this.size = size;
        this.queryType = queryType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupQueryKey)) {
            return false;
        }
        GroupQueryKey that = (GroupQueryKey) o;
        return page == that.page &&
                size == that.size &&
                Objects.equals(studentLastName, that.studentLastName) &&
                Objects.equals(queryType, that.queryType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentLastName, page, size, queryType);
    }
}