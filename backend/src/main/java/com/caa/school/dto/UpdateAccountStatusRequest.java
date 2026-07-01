package com.caa.school.dto;

/**
 * 更新账户状态请求 DTO。
 * status 取值：ACTIVE / DISABLED / LOCKED
 */
public record UpdateAccountStatusRequest(
        String status
) {}
