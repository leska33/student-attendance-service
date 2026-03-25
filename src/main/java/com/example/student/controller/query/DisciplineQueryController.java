package com.example.student.controller.query;

import com.example.student.dto.DisciplineResponseDto;
import com.example.student.service.DisciplineQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/disciplines/filter")
public class DisciplineQueryController {

    private final DisciplineQueryService service;

    public DisciplineQueryController(DisciplineQueryService service) {
        this.service = service;
    }

    @GetMapping("/jpql")
    public List<DisciplineResponseDto> getByTeacherJPQL(
            @RequestParam String firstName,
            @RequestParam String middleName,
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getDisciplinesByTeacherJPQL(firstName, middleName, lastName, page, size);
    }

    @GetMapping("/native")
    public List<DisciplineResponseDto> getByTeacherNative(
            @RequestParam String firstName,
            @RequestParam String middleName,
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.getDisciplinesByTeacherNative(firstName, middleName, lastName, page, size);
    }
}