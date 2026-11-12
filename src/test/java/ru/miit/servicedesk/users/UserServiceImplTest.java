package ru.miit.servicedesk.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.miit.servicedesk.common.ConflictException;
import ru.miit.servicedesk.users.DTO.CreateUserRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, roleRepository, departmentRepository, passwordEncoder);
    }

    @Test
    void createHashesPasswordAndChecksLoginUniqueness() {
        when(userRepository.existsByLoginIgnoreCase("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        RoleEntity role = org.mockito.Mockito.mock(RoleEntity.class);
        when(role.getId()).thenReturn((short) 1);
        when(role.getNameRole()).thenReturn("TEACHER");
        when(roleRepository.findById((short) 1)).thenReturn(java.util.Optional.of(role));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        service.create(new CreateUserRequest(
                "newuser", "password123", "Иван", "Иванов", null, (short) 1, null));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$encoded");
        assertThat(captor.getValue().getLogin()).isEqualTo("newuser");
    }

    @Test
    void createRejectsDuplicateLogin() {
        when(userRepository.existsByLoginIgnoreCase("taken")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateUserRequest(
                "taken", "password123", "A", "B", null, (short) 1, null)))
                .isInstanceOf(ConflictException.class);
    }
}
