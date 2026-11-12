package ru.miit.servicedesk.users.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String patronymic,
        @NotNull Short roleId,
        Long departmentId
) {
}
