package com.example.student.controller;

import com.example.student.dto.GroupResponseDto;
import com.example.student.service.GroupService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/lazy")
    public List<GroupResponseDto> getGroupsLazy() {
        return groupService.getAllGroupsDtoLazy();
    }

    @GetMapping("/optimized")
    public List<GroupResponseDto> getGroupsOptimized() {
        return groupService.getAllGroupsDtoOptimized();
    }
}