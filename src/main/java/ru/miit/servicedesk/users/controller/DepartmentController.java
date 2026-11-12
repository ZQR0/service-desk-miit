package ru.miit.servicedesk.users.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.miit.servicedesk.common.PageResponse;
import ru.miit.servicedesk.users.DTO.AssignDepartmentHeadRequest;
import ru.miit.servicedesk.users.DTO.UserDto;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;
import ru.miit.servicedesk.users.DepartmentService;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/{id}/users")
    PageResponse<UserDto> users(@PathVariable Long id, Pageable pageable) {
        return PageResponse.from(departmentService.getDepartmentUsers(id, pageable), value -> value);
    }

    @GetMapping("/{id}/head")
    UserSummaryDto head(@PathVariable Long id) {
        return departmentService.getDepartmentHead(id);
    }

    @PutMapping("/{id}/head")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> assignHead(@PathVariable Long id, @Valid @RequestBody AssignDepartmentHeadRequest request) {
        departmentService.assignHead(id, request.userId());
        return ResponseEntity.noContent().build();
    }
}
