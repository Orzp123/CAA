package com.caa.school.dto;

import java.util.List;

/**
 * 批量删除账户请求 DTO。
 */
public record BatchDeleteRequest(
        List<String> accountIds
) {}
