package com.caa.workflow.repository;

import com.caa.workflow.model.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowDefinition, String> {

    List<WorkflowDefinition> findByTenantId(String tenantId);

    Optional<WorkflowDefinition> findByIdAndTenantId(String id, String tenantId);

    List<WorkflowDefinition> findByTenantIdAndStatus(
            String tenantId, WorkflowDefinition.WorkflowStatus status);
}
