package rut.miit.servicedesk.auth;

import java.util.List;
import java.util.Optional;

/**
 Справочник пользователей и кафедр.
 TODO: согласовать с командой
 */
public interface UserDirectory {
    Optional<UserDto> findById(long id);
    Optional<UserDto> headOfDepartment(long departmentId);
    List<UserDto> findByRoleAndDepartment(Role role, Long departmentId);
}
