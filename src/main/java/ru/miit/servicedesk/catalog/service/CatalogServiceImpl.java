package ru.miit.servicedesk.catalog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;
import ru.miit.servicedesk.catalog.repository.ServiceCatalogRepository;

import java.util.List;

@Service
public class CatalogServiceImpl implements CatalogService {
    ServiceCatalogRepository serviceCatalogRepository;

    public CatalogServiceImpl(ServiceCatalogRepository serviceCatalogRepository) {
        this.serviceCatalogRepository = serviceCatalogRepository;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceCatalogEntity create(String nameService, String description, Integer deadline) {
        String name = nameService == null ? "" : nameService.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("nameService must not be blank");
        }
        if (deadline == null || deadline <= 0) {
            throw new IllegalArgumentException("deadline must be > 0");
        }
        if (serviceCatalogRepository.existsByNameServiceIgnoreCase(name)) {
            throw new IllegalArgumentException("Service name already exists: " + name);
        }

        ServiceCatalogEntity entity = new ServiceCatalogEntity();
        entity.setNameService(name);
        entity.setDescription(description);
        entity.setDeadline(deadline);
        return serviceCatalogRepository.save(entity);
    }

    @Override
    public Page<ServiceCatalogEntity> listAll(String query, Boolean active, Pageable pageable) {
        String normalized = normalizeQuery(query);
        if (active != null) {
            return serviceCatalogRepository.findByNameServiceContainingIgnoreCaseAndActive(
                normalized, active, pageable);
        }
        return serviceCatalogRepository.findByNameServiceContainingIgnoreCase(normalized, pageable);
    }

    @Override
    public List<ServiceCatalogEntity> findAll() {
        return serviceCatalogRepository.findAll();
    }

    @Override
    public Page<ServiceCatalogEntity> findActive(String query, Pageable pageable) {
        String normalized = normalizeQuery(query);
        return serviceCatalogRepository.findByNameServiceContainingIgnoreCaseAndActiveTrue(
            normalized, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceCatalogEntity getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return serviceCatalogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceCatalogEntity update(Long id, String nameService, String description, Integer deadline) {
        ServiceCatalogEntity entity = getById(id);
        String name = normalizeName(nameService);
        validateDeadline(deadline);
        if (serviceCatalogRepository.existsByNameServiceIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Service name already exists: " + name);
        }
        apply(entity, name, description, deadline);
        return entity;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(Long id) {
        getById(id).setActive(false);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void activate(Long id) {
        getById(id).setActive(true);
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private void validateDeadline(Integer deadline) {
        if (deadline == null || deadline <= 0) {
            throw new IllegalArgumentException("deadline must be > 0");
        }
    }

    private String normalizeName(String nameService) {
        String name = nameService == null ? "" : nameService.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("nameService must not be blank");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("nameService must be at most 255 characters");
        }
        return name;
    }

    private void apply(ServiceCatalogEntity entity, String name, String description, Integer deadline) {
        entity.setNameService(name);
        entity.setDescription(description == null || description.isBlank() ? null : description.trim());
        entity.setDeadline(deadline);
    }
}
