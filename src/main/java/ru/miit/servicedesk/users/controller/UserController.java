package ru.miit.servicedesk.users.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.miit.servicedesk.common.PageResponse;
import ru.miit.servicedesk.users.DTO.ChangePasswordRequest;
import ru.miit.servicedesk.users.DTO.CreateUserRequest;
import ru.miit.servicedesk.users.DTO.UpdateUserRequest;
import ru.miit.servicedesk.users.DTO.UserDto;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;
import ru.miit.servicedesk.users.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    ResponseEntity<UserDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    PageResponse<UserDto> findAll(
            @RequestParam(required = false) Long departmentId,
            Pageable pageable
    ) {
        if (departmentId != null) {
            return PageResponse.from(userService.findByDepartment(departmentId, pageable), value -> value);
        }
        return PageResponse.from(userService.findAll(pageable), value -> value);
    }

    @GetMapping("/executors")
    java.util.List<UserSummaryDto> possibleExecutors() {
        return userService.findPossibleExecutors();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
