package com.example.student.mapper;

import com.example.student.dto.GroupResponseDto;
import com.example.student.entity.Group;

import java.util.List;
import java.util.stream.Collectors;

public final class GroupMapper {

    private GroupMapper() {
    }

    public static GroupResponseDto toDto(Group group) {

        if (group == null) {
            return null;
        }

        List<String> studentNames = null;

        if (group.getStudents() != null) {
            studentNames = group.getStudents()
                    .stream()
                    .map(student ->
                            student.getLastName() + " " +
                                    student.getFirstName() + " " +
                                    student.getMiddleName()
                    )
                    .collect(Collectors.toList());
        }

        return new GroupResponseDto(
                group.getId(),
                group.getNumber(),
                studentNames
        );
    }
}