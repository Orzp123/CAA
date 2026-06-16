package com.caa.workflow.service;

import com.caa.common.ErrorCode;
import com.caa.common.TenantContext;
import com.caa.workflow.dto.WorkflowRequest;
import com.caa.workflow.dto.WorkflowResponse;
import com.caa.workflow.model.WorkflowDefinition;
import com.caa.workflow.repository.WorkflowRepository;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    @Value("${temporal.service-url:localhost:7233}")
    private String temporalServiceUrl;

    @Value("${temporal.namespace:default}")
    private String temporalNamespace;

    private final WorkflowRepository workflowRepository;
    private WorkflowClient workflowClient;

    public WorkflowService(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    private WorkflowClient getClient() {
        if (workflowClient == null) {
            WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
                    WorkflowServiceStubsOptions.newBuilder()
                            .setTarget(temporalServiceUrl)
                            .build());
            workflowClient = WorkflowClient.newInstance(service,
                    WorkflowClientOptions.newBuilder()
                            .setNamespace(temporalNamespace)
                            .build());
        }
        return workflowClient;
    }

    public List<WorkflowResponse> findAll() {
        return workflowRepository.findByTenantId(TenantContext.get())
                .stream().map(this::toResponse).toList();
    }

    public WorkflowResponse findById(String id) {
        String tenantId = TenantContext.get();
        return workflowRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.WORKFLOW_NOT_FOUND.message() + ": " + id));
    }

    @Transactional
    public WorkflowResponse create(WorkflowRequest request) {
        String tenantId = TenantContext.get();
        WorkflowDefinition def = toEntity(request, tenantId);
        return toResponse(workflowRepository.save(def));
    }

    @Transactional
    public WorkflowResponse update(String id, WorkflowRequest request) {
        String tenantId = TenantContext.get();
        WorkflowDefinition existing = workflowRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.WORKFLOW_NOT_FOUND.message() + ": " + id));
        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setGraphJson(request.graphJson());
        existing.setTemporalWorkflowType(request.temporalWorkflowType());
        if (request.status() != null) {
            existing.setStatus(WorkflowDefinition.WorkflowStatus.valueOf(request.status()));
        }
        return toResponse(workflowRepository.save(existing));
    }

    @Transactional
    public void delete(String id) {
        String tenantId = TenantContext.get();
        WorkflowDefinition existing = workflowRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.WORKFLOW_NOT_FOUND.message() + ": " + id));
        workflowRepository.delete(existing);
    }

    public Map<String, Object> startWorkflow(String id, Map<String, Object> input) {
        String tenantId = TenantContext.get();
        WorkflowDefinition definition = workflowRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.WORKFLOW_NOT_FOUND.message() + ": " + id));
        String workflowId = "workflow-" + definition.getId() + "-" + System.currentTimeMillis();
        log.info("Starting workflow {} id={} tenant=", definition.getName(), workflowId, tenantId);
        Map<String, Object> result = new HashMap<>();
        result.put("workflowId", workflowId);
        result.put("runId", "run-" + System.currentTimeMillis());
        result.put("status", "STARTED");
        result.put("definition", definition.getName());
        return result;
    }

    public Map<String, Object> getWorkflowStatus(String workflowId) {
        Map<String, Object> status = new HashMap<>();
        status.put("workflowId", workflowId);
        status.put("status", "RUNNING");
        return status;
    }

    public void terminateWorkflow(String workflowId, String reason) {
        log.info("Terminating workflow {} reason: {}", workflowId, reason);
    }

    // --- Mappers ---

    private WorkflowResponse toResponse(WorkflowDefinition d) {
        return new WorkflowResponse(
                d.getId(), d.getTenantId(), d.getName(), d.getDescription(),
                d.getStatus().name(), d.getGraphJson(), d.getTemporalWorkflowType(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    private WorkflowDefinition toEntity(WorkflowRequest r, String tenantId) {
        WorkflowDefinition d = new WorkflowDefinition();
        d.setTenantId(tenantId);
        d.setName(r.name());
        d.setDescription(r.description());
        d.setGraphJson(r.graphJson());
        d.setTemporalWorkflowType(r.temporalWorkflowType());
        if (r.status() != null) {
            d.setStatus(WorkflowDefinition.WorkflowStatus.valueOf(r.status()));
        }
        return d;
    }
}
