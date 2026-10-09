package rut.miit.servicedesk.auth;

public record UserDto(
    long id,
    String firstName,
    String lastName,
    String middleName,
    Long departmentId
) {}

