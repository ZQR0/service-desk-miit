package ru.miit.servicedesk.users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = {"role", "department"})
    Optional<UserEntity> findByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCaseAndIdNot(String login, Long id);

    @EntityGraph(attributePaths = {"role", "department"})
    Page<UserEntity> findByDepartment_Id(Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"role", "department"})
    Page<UserEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"role", "department"})
    List<UserEntity> findByDepartment_IdAndRole_NameRole(Long departmentId, String roleName);

    @EntityGraph(attributePaths = {"role", "department"})
    List<UserEntity> findByRole_NameRole(String roleName);
}
