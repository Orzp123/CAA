package com.caa.page.service;

import com.caa.common.ErrorCode;
import com.caa.common.TenantContext;
import com.caa.page.dto.PageRequest;
import com.caa.page.dto.PageResponse;
import com.caa.page.model.PageSchema;
import com.caa.page.repository.PageSchemaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PageService {

    private final PageSchemaRepository pageSchemaRepository;

    public PageService(PageSchemaRepository pageSchemaRepository) {
        this.pageSchemaRepository = pageSchemaRepository;
    }

    public List<PageResponse> findAll() {
        return pageSchemaRepository.findByTenantId(TenantContext.get())
                .stream().map(this::toResponse).toList();
    }

    @Cacheable(value = "pages", key = "#tenantId + ':' + #id")
    public PageResponse findById(String id) {
        String tenantId = TenantContext.get();
        return pageSchemaRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.PAGE_NOT_FOUND.message() + ": " + id));
    }

    public PageResponse findByPath(String path) {
        String tenantId = TenantContext.get();
        return pageSchemaRepository.findByTenantIdAndPath(tenantId, path)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.PAGE_NOT_FOUND.message() + " at path: " + path));
    }

    @Transactional
    public PageResponse create(PageRequest request) {
        String tenantId = TenantContext.get();
        PageSchema page = toEntity(request, tenantId);
        return toResponse(pageSchemaRepository.save(page));
    }

    @Transactional
    @CacheEvict(value = "pages", key = "#tenantId + ':' + #id")
    public PageResponse update(String id, PageRequest request) {
        String tenantId = TenantContext.get();
        PageSchema existing = pageSchemaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.PAGE_NOT_FOUND.message() + ": " + id));
        existing.setName(request.name());
        existing.setPath(request.path());
        existing.setDescription(request.description());
        existing.setSchemaJson(request.schemaJson());
        if (request.status() != null) {
            existing.setStatus(PageSchema.PageStatus.valueOf(request.status()));
        }
        return toResponse(pageSchemaRepository.save(existing));
    }

    @Transactional
    @CacheEvict(value = "pages", key = "#tenantId + ':' + #id")
    public void delete(String id) {
        String tenantId = TenantContext.get();
        PageSchema existing = pageSchemaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.PAGE_NOT_FOUND.message() + ": " + id));
        pageSchemaRepository.delete(existing);
    }

    // --- Mappers ---

    private PageResponse toResponse(PageSchema p) {
        return new PageResponse(
                p.getId(), p.getTenantId(), p.getName(), p.getPath(),
                p.getDescription(), p.getSchemaJson(),
                p.getStatus().name(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private PageSchema toEntity(PageRequest r, String tenantId) {
        PageSchema p = new PageSchema();
        p.setTenantId(tenantId);
        p.setName(r.name());
        p.setPath(r.path());
        p.setDescription(r.description());
        p.setSchemaJson(r.schemaJson());
        if (r.status() != null) {
            p.setStatus(PageSchema.PageStatus.valueOf(r.status()));
        }
        return p;
    }
}
