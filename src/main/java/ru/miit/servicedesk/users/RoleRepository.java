package ru.miit.servicedesk.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Short> {

    Optional<RoleEntity> findByNameRole(String nameRole);
}
