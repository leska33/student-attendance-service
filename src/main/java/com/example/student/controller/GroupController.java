package com.example.student.controller;

import com.example.student.dto.GroupCreateDto;
import com.example.student.dto.GroupResponseDto;
import com.example.student.openapi.OpenApiDescriptions;
import com.example.student.service.GroupQueryService;
import com.example.student.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Группы", description = "Операции с группами")
@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupQueryService queryService;

    public GroupController(GroupService groupService, GroupQueryService queryService) {
        this.groupService = groupService;
        this.queryService = queryService;
    }

    @Operation(summary = "Создать группу")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = OpenApiDescriptions.CREATED_201),
                    @ApiResponse(responseCode = "400", description = OpenApiDescriptions.VALIDATION_ERROR_400)
    })
    @PostMapping
    public ResponseEntity<GroupResponseDto> createGroup(@Valid @RequestBody GroupCreateDto dto) {
        GroupResponseDto response = groupService.createGroup(dto);
        queryService.invalidateCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Получить все группы")
    @GetMapping
    public ResponseEntity<List<GroupResponseDto>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroupsDtoOptimized());
    }

    @Operation(summary = "Получить группу по ID")
    @ApiResponse(responseCode = "404", description = "Группа не найдена")
    @GetMapping("/{id}")
    public ResponseEntity<GroupResponseDto> getGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getGroupById(id));
    }

    @Operation(summary = "Обновить группу")
    @ApiResponse(responseCode = "404", description = "Группа не найдена")
    @PutMapping("/{id}")
    public ResponseEntity<GroupResponseDto> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody GroupCreateDto dto) {

        GroupResponseDto response = groupService.updateGroup(id, dto);
        queryService.invalidateCache();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Удалить группу")
    @ApiResponse(responseCode = "404", description = "Группа не найдена")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        queryService.invalidateCache();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить группы (lazy)")
    @GetMapping("/lazy")
    public ResponseEntity<List<GroupResponseDto>> getGroupsLazy() {
        return ResponseEntity.ok(groupService.getAllGroupsDtoLazy());
    }
    @PostMapping("/bulk")
    public List<GroupResponseDto> bulk(@RequestBody List<GroupCreateDto> dtos) {
        return groupService.createGroupsBulk(dtos);
    }

    @PostMapping("/bulk/no-transaction")
    public List<GroupResponseDto> bulkNoTx(@RequestBody List<GroupCreateDto> dtos) {
        return groupService.createGroupsBulkWithoutTransaction(dtos);
    }
}