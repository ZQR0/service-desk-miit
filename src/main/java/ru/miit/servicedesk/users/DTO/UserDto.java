package ru.miit.servicedesk.users.DTO;

public record UserDto(
        Long id,
        String login,
        String name,
        String lastName,
        String patronymic,
        String displayName,
        Short roleId,
        String role,
        Long departmentId,
        String department
) {
}
