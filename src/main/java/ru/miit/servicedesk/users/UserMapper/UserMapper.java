package ru.miit.servicedesk.users.UserMapper;

import java.util.stream.Stream;

import org.springframework.security.core.userdetails.UserDetails;

import ru.miit.servicedesk.security.dto.DefaultUserDetails;
import ru.miit.servicedesk.users.DTO.UserDto;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;
import ru.miit.servicedesk.users.DepartmentEntity;
import ru.miit.servicedesk.users.UserEntity;

public final class UserMapper {
    private UserMapper() {
    }

    public static String formatDisplayName(UserEntity user) {
        if (user == null) {
            return null;
        }
        return Stream.of(user.getLastName(), user.getName(), user.getPatronymic())
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse(user.getLogin());
    }

    public static UserSummaryDto toShort(UserEntity u) {
        if (u == null) {
            return null;
        }
        return new UserSummaryDto(u.getId(), formatDisplayName(u), u.getRole().getNameRole());
    }

    public static UserDto toDto(UserEntity user) {
        DepartmentEntity department = user.getDepartment();
        return new UserDto(
                user.getId(),
                user.getLogin(),
                user.getName(),
                user.getLastName(),
                user.getPatronymic(),
                formatDisplayName(user),
                user.getRole().getId(),
                user.getRole().getNameRole(),
                department == null ? null : department.getId(),
                department == null ? null : department.getName()
        );
    }

    public static UserDetails toUserDetails(UserEntity user) {
        if (user == null) {
            throw new NullPointerException("User in mapper cannot be null");
        }
        return new DefaultUserDetails(
                user.getLogin(),
                user.getPasswordHash(),
                user.getRole().getNameRole()
        );
    }
}
