package ru.miit.servicedesk.users;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.miit.servicedesk.common.ConflictException;
import ru.miit.servicedesk.common.NotFoundException;
import ru.miit.servicedesk.users.DTO.ChangePasswordRequest;
import ru.miit.servicedesk.users.DTO.CreateUserRequest;
import ru.miit.servicedesk.users.DTO.UpdateUserRequest;
import ru.miit.servicedesk.users.DTO.UserDto;
import ru.miit.servicedesk.users.DTO.UserSummaryDto;
import ru.miit.servicedesk.users.UserMapper.UserMapper;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserDto create(CreateUserRequest request) {
        String login = request.login().trim();
        if (userRepository.existsByLoginIgnoreCase(login)) {
            throw new ConflictException("LOGIN_ALREADY_EXISTS", "Login is already occupied");
        }
        UserEntity user = new UserEntity();
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        apply(user, request.name(), request.lastName(), request.patronymic(),
                request.roleId(), request.departmentId());
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserDto getById(Long id) {
        return UserMapper.toDto(getRequired(id));
    }

    @Override
    public Page<UserDto> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::toDto);
    }

    @Override
    public Page<UserDto> findByDepartment(Long departmentId, Pageable pageable) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new NotFoundException("Department not found: " + departmentId);
        }
        return userRepository.findByDepartment_Id(departmentId, pageable).map(UserMapper::toDto);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UpdateUserRequest request) {
        UserEntity user = getRequired(id);
        apply(user, request.name(), request.lastName(), request.patronymic(),
                request.roleId(), request.departmentId());
        return UserMapper.toDto(user);
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        getRequired(id).setPasswordHash(passwordEncoder.encode(request.password()));
    }

    @Override
    public List<UserSummaryDto> findPossibleExecutors() {
        return userRepository.findByRole_NameRole(RoleName.DUTY_OFFICER.name()).stream()
                .map(UserMapper::toShort)
                .toList();
    }

    private void apply(
            UserEntity user,
            String name,
            String lastName,
            String patronymic,
            Short roleId,
            Long departmentId
    ) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
        DepartmentEntity department = departmentId == null ? null
                : departmentRepository.findById(departmentId)
                        .orElseThrow(() -> new NotFoundException("Department not found: " + departmentId));
        user.setName(name.trim());
        user.setLastName(lastName.trim());
        user.setPatronymic(patronymic == null ? null : patronymic.trim());
        user.setRole(role);
        user.setDepartment(department);
    }

    private UserEntity getRequired(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
