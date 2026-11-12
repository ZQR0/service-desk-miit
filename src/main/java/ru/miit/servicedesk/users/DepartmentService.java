package ru.miit.servicedesk.users;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ru.miit.servicedesk.users.DTO.UserDto;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;

public interface DepartmentService {

    Page<UserDto> getDepartmentUsers(Long departmentId, Pageable pageable);

    UserSummaryDto getDepartmentHead(Long departmentId);

    void assignHead(Long departmentId, Long userId);

    List<DepartmentEntity> listAll();
}
