package com.example.student.controller.query;

import com.example.student.dto.GroupResponseDto;
import com.example.student.service.GroupQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/groups/filter")
public class GroupQueryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupQueryController.class);

    private final GroupQueryService groupQueryService;

    public GroupQueryController(GroupQueryService groupQueryService) {
        this.groupQueryService = groupQueryService;
    }

    @GetMapping("/jpql")
    public List<GroupResponseDto> getByStudentJPQL(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeHash = lastName.hashCode();
        LOGGER.info("GROUP_JPQL request - key={}, page={}, size={}", safeHash, page, size);

        return groupQueryService.getGroupsByStudentLastNameJPQL(lastName, page, size);
    }

    @GetMapping("/native")
    public List<GroupResponseDto> getByStudentNative(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safeHash = lastName.hashCode();
        LOGGER.info("GROUP_NATIVE request - key={}, page={}, size={}", safeHash, page, size);

        return groupQueryService.getGroupsByStudentLastNameNative(lastName, page, size);
    }
}