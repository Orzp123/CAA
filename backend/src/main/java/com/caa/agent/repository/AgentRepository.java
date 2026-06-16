package com.caa.agent.repository;

import com.caa.agent.model.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<AgentEntity, String> {

    List<AgentEntity> findByTenantId(String tenantId);

    List<AgentEntity> findByTenantIdAndStatus(String tenantId, AgentEntity.AgentStatus status);

    List<AgentEntity> findByTenantIdAndProvider(String tenantId, String provider);

    boolean existsByTenantIdAndName(String tenantId, String name);

    Optional<AgentEntity> findByIdAndTenantId(String id, String tenantId);

    // Legacy single-tenant queries (kept for backward compat, now tenant-scoped)
    @Query("SELECT a FROM AgentEntity a WHERE a.status = :status AND a.tenantId = :tenantId")
    List<AgentEntity> findByStatus(@Param("status") AgentEntity.AgentStatus status,
                                   @Param("tenantId") String tenantId);
}
