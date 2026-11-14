package ru.miit.servicedesk.users;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.miit.servicedesk.common.BusinessRuleException;
import ru.miit.servicedesk.common.NotFoundException;
import ru.miit.servicedesk.users.DTO.UserDto;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;
import ru.miit.servicedesk.users.UserMapper.UserMapper;

@Service
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<UserDto> getDepartmentUsers(Long departmentId, Pageable pageable) {
        ensureDepartmentExists(departmentId);
        return userRepository.findByDepartment_Id(departmentId, pageable).map(UserMapper::toDto);
    }

    @Override
    public UserSummaryDto getDepartmentHead(Long departmentId) {
        DepartmentEntity department = departmentRepository.findWithHeadById(departmentId)
                .orElseThrow(() -> new NotFoundException("Department not found: " + departmentId));
        return UserMapper.toShort(department.getHead());
    }

    @Override
    @Transactional
    public void assignHead(Long departmentId, Long userId) {
        DepartmentEntity department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NotFoundException("Department not found: " + departmentId));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (user.getDepartment() == null
                || !departmentId.equals(user.getDepartment().getId())) {
            throw new BusinessRuleException(
                    "HEAD_NOT_IN_DEPARTMENT",
                    "Department head must belong to the same department"
            );
        }
        if (!RoleName.DEPARTMENT_HEAD.name().equals(user.getRole().getNameRole())) {
            throw new BusinessRuleException(
                    "INVALID_HEAD_ROLE",
                    "Department head must have role DEPARTMENT_HEAD"
            );
        }
        department.setHead(user);
    }

    @Override
    public List<DepartmentEntity> listAll() {
        return departmentRepository.findAll();
    }

    private void ensureDepartmentExists(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new NotFoundException("Department not found: " + departmentId);
        }
    }
}
