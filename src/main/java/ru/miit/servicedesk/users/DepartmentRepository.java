package ru.miit.servicedesk.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    @EntityGraph(attributePaths = {"head", "head.role"})
    Optional<DepartmentEntity> findWithHeadById(Long id);
}
