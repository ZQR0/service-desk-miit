package ru.miit.servicedesk.catalog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;

import java.util.List;

public interface CatalogService {

    Page<ServiceCatalogEntity> listAll(String query, Boolean active, Pageable pageable);

    List<ServiceCatalogEntity> findAll();

    Page<ServiceCatalogEntity> findActive(String query, Pageable pageable);

    ServiceCatalogEntity getById(Long id);

    ServiceCatalogEntity create(String nameService, String description, Integer deadline);

    ServiceCatalogEntity update(Long id, String nameService, String description, Integer deadline);

    void deactivate(Long id);

    void activate(Long id);
}
