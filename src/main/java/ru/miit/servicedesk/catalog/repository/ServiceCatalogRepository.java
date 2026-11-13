package ru.miit.servicedesk.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;

import java.util.Optional;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalogEntity, Long> {
    boolean existsByNameServiceIgnoreCase(String nameService);

    boolean existsByNameServiceIgnoreCaseAndIdNot(String name, Long id);

    Optional<ServiceCatalogEntity> findByIdAndActiveTrue(Long id);

    Page<ServiceCatalogEntity> findByNameServiceContainingIgnoreCaseAndActive(
        String query, boolean active, Pageable pageable);

    Page<ServiceCatalogEntity> findByNameServiceContainingIgnoreCaseAndActiveTrue(
        String query, Pageable pageable);

    Page<ServiceCatalogEntity> findByNameServiceContainingIgnoreCase(
        String query, Pageable pageable);
}
