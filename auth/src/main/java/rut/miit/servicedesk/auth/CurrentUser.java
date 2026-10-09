package rut.miit.servicedesk.auth;

/**
 Доступ к текущему аутентифицированному пользователю.
 TODO: согласовать с командой
 */
public interface CurrentUser {
    long id();
    String firstName();
    String lastName();
    String middleName();
    Long departmentId();
    boolean hasRole(Role role);
}

