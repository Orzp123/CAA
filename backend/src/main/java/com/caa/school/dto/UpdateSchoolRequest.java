package com.caa.school.dto;

import java.util.List;

/**
 * 更新学校请求 DTO。
 */
public record UpdateSchoolRequest(
        String name,
        String domain,
        String packageId,
        List<String> permissionCodes,
        BrandInfo brand,
        List<SlotRequest> slots
) {}
