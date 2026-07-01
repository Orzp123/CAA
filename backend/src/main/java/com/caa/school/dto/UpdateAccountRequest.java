package com.caa.school.dto;

/**
 * 更新账户基本信息请求 DTO。
 * accountType / secondaryRole 不可通过此接口修改。
 */
public record UpdateAccountRequest(
        String name,
        String nickname,
        String email,
        String phone
) {}
