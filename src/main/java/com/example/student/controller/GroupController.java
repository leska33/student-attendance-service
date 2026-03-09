package com.example.student.controller;

import com.example.student.dto.GroupCreateDto;
import com.example.student.dto.GroupResponseDto;
import com.example.student.service.GroupService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public void createGroup(@RequestBody GroupCreateDto dto) {
        groupService.createGroup(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
    }
}