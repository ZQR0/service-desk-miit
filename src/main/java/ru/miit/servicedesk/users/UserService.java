package ru.miit.servicedesk.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ru.miit.servicedesk.users.DTO.ChangePasswordRequest;
import ru.miit.servicedesk.users.DTO.CreateUserRequest;
import ru.miit.servicedesk.users.DTO.UpdateUserRequest;
import ru.miit.servicedesk.users.DTO.UserDto;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;

public interface UserService {

    UserDto create(CreateUserRequest request);

    UserDto getById(Long id);

    Page<UserDto> findAll(Pageable pageable);

    Page<UserDto> findByDepartment(Long departmentId, Pageable pageable);

    UserDto update(Long id, UpdateUserRequest request);

    void changePassword(Long id, ChangePasswordRequest request);

    java.util.List<UserSummaryDto> findPossibleExecutors();
}
