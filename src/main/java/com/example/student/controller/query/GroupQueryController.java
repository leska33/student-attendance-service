package com.example.student.controller.query;

import com.example.student.dto.GroupResponseDto;
import com.example.student.service.GroupQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/groups/filter")
public class GroupQueryController {

    private final GroupQueryService service;

    public GroupQueryController(GroupQueryService service) {
        this.service = service;
    }

    @GetMapping("/jpql")
    public List<GroupResponseDto> getByStudentJPQL(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.getGroupsByStudentLastNameJPQL(lastName, page, size);
    }

    @GetMapping("/native")
    public List<GroupResponseDto> getByStudentNative(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getGroupsByStudentLastNameNative(lastName, page, size);
    }
}