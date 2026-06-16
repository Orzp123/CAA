package com.caa.page.repository;

import com.caa.page.model.PageSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageSchemaRepository extends JpaRepository<PageSchema, String> {

    List<PageSchema> findByTenantId(String tenantId);

    Optional<PageSchema> findByTenantIdAndPath(String tenantId, String path);

    Optional<PageSchema> findByIdAndTenantId(String id, String tenantId);

    List<PageSchema> findByTenantIdAndStatus(String tenantId, PageSchema.PageStatus status);
}
