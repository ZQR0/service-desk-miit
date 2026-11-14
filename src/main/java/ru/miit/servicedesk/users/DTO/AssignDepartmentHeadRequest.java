package ru.miit.servicedesk.users.DTO;

import jakarta.validation.constraints.NotNull;

public record AssignDepartmentHeadRequest(@NotNull Long userId) {
}
