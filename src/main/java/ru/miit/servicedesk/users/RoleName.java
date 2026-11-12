package ru.miit.servicedesk.users;

public enum RoleName {
    TEACHER,
    DEPARTMENT_HEAD,
    DEPARTMENT_STAFF,
    DUTY_OFFICER,
    ADMIN;

    public static RoleName fromDatabase(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role name cannot be null");
        }
        return RoleName.valueOf(value);
    }
}
