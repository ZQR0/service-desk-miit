ALTER TABLE service_catalog
    ADD COLUMN active boolean NOT NULL DEFAULT true;

CREATE INDEX idx_service_catalog_active_name
    ON service_catalog (active, lower(name_service));
