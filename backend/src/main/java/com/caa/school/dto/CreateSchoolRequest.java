package com.caa.school.dto;

import java.util.List;

/**
 * 创建学校请求 DTO。
 */
public record CreateSchoolRequest(
        String name,
        String code,
        String domain,
        String packageId,
        List<String> permissionCodes,
        BrandInfo brand,
        List<SlotRequest> slots
) {}
