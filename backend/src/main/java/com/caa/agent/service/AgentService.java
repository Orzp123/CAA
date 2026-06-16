package com.caa.agent.service;

import com.caa.agent.dto.AgentRequest;
import com.caa.agent.dto.AgentResponse;
import com.caa.agent.model.AgentEntity;
import com.caa.agent.repository.AgentRepository;
import com.caa.common.ErrorCode;
import com.caa.common.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AgentService {

    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public List<AgentResponse> findAll() {
        return agentRepository.findByTenantId(TenantContext.get())
                .stream().map(this::toResponse).toList();
    }

    public List<AgentResponse> findActive() {
        return agentRepository.findByTenantIdAndStatus(
                        TenantContext.get(), AgentEntity.AgentStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Cacheable(value = "agents", key = "#tenantId + ':' + #id")
    public AgentResponse findById(String id) {
        String tenantId = TenantContext.get();
        return agentRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.AGENT_NOT_FOUND.message() + ": " + id));
    }

    @Transactional
    public AgentResponse create(AgentRequest request) {
        String tenantId = TenantContext.get();
        if (agentRepository.existsByTenantIdAndName(tenantId, request.name())) {
            throw new IllegalArgumentException(
                    ErrorCode.AGENT_NAME_CONFLICT.message() + ": " + request.name());
        }
        AgentEntity entity = toEntity(request, tenantId);
        return toResponse(agentRepository.save(entity));
    }

    @Transactional
    @CacheEvict(value = "agents", key = "#tenantId + ':' + #id")
    public AgentResponse update(String id, AgentRequest request) {
        String tenantId = TenantContext.get();
        AgentEntity existing = agentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.AGENT_NOT_FOUND.message() + ": " + id));
        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setProvider(request.provider());
        existing.setModel(request.model());
        existing.setSystemPrompt(request.systemPrompt());
        existing.setConfig(request.config());
        if (request.status() != null) {
            existing.setStatus(AgentEntity.AgentStatus.valueOf(request.status()));
        }
        return toResponse(agentRepository.save(existing));
    }

    @Transactional
    @CacheEvict(value = "agents", key = "#tenantId + ':' + #id")
    public void delete(String id) {
        String tenantId = TenantContext.get();
        AgentEntity existing = agentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.AGENT_NOT_FOUND.message() + ": " + id));
        agentRepository.delete(existing);
    }

    /** Used by ChatService — bypasses tenant filter for internal calls. */
    public AgentEntity findEntityById(String id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.AGENT_NOT_FOUND.message() + ": " + id));
    }

    // --- Mappers ---

    private AgentResponse toResponse(AgentEntity e) {
        return new AgentResponse(
                e.getId(), e.getTenantId(), e.getName(), e.getDescription(),
                e.getProvider(), e.getModel(), e.getSystemPrompt(),
                e.getStatus().name(), e.getConfig(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private AgentEntity toEntity(AgentRequest r, String tenantId) {
        AgentEntity e = new AgentEntity();
        e.setTenantId(tenantId);
        e.setName(r.name());
        e.setDescription(r.description());
        e.setProvider(r.provider());
        e.setModel(r.model());
        e.setSystemPrompt(r.systemPrompt());
        e.setConfig(r.config());
        if (r.status() != null) {
            e.setStatus(AgentEntity.AgentStatus.valueOf(r.status()));
        }
        return e;
    }
}
