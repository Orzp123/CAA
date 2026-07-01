package com.caa.school.repository;

import com.caa.school.model.PromotionalSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PromotionalSlotRepository extends JpaRepository<PromotionalSlot, String> {

    List<PromotionalSlot> findAllByTenantIdOrderBySortOrderAsc(String tenantId);

    // H-2 fix: 只返回 ACTIVE 状态的运营位，过滤 INACTIVE
    List<PromotionalSlot> findAllByTenantIdAndStatusOrderBySortOrderAsc(
            String tenantId, PromotionalSlot.SlotStatus status);

    long countByTenantId(String tenantId);

    @Modifying
    @Transactional
    void deleteAllByTenantId(String tenantId);
}
