package com.caa.school.dto;

import jakarta.validation.constraints.Pattern;

/**
 * 运营位请求 DTO。
 * position 使用字符串，对应 PromotionalSlot.SlotPosition 枚举名。
 */
public record SlotRequest(
        String title,
        @Pattern(regexp = "^https?://.*", message = "URL 必须以 http:// 或 https:// 开头")
        String imageUrl,
        @Pattern(regexp = "^https?://.*", message = "URL 必须以 http:// 或 https:// 开头")
        String linkUrl,
        String position,
        int sortOrder
) {}
