package com.example.student.cache;

import java.util.Objects;

public class DisciplineQueryKey {
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final int page;
    private final int size;
    private final String queryType;

    public DisciplineQueryKey(String firstName, String middleName, String lastName,
                              int page, int size, String queryType) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.page = page;
        this.size = size;
        this.queryType = queryType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DisciplineQueryKey)) {
            return false;
        }
        DisciplineQueryKey that = (DisciplineQueryKey) o;
        return page == that.page &&
                size == that.size &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(middleName, that.middleName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(queryType, that.queryType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, middleName, lastName, page, size, queryType);
    }
}