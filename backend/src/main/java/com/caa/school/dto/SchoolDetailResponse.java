package com.caa.school.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学校详情响应 DTO。
 */
public record SchoolDetailResponse(
        String id,
        String code,
        String name,
        String domain,
        String status,
        BrandInfo brand,
        String packageId,
        List<String> permissionCodes,
        List<SlotRequest> slots,
        LocalDateTime createdAt
) {}
