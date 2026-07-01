package com.caa.school.dto;

import java.time.LocalDateTime;

/**
 * H-4 fix: JPQL 投影接口，一次查询返回学校摘要含管理员数量，消除 N+1 问题。
 * Spring Data JPA 会自动生成代理实现。
 */
public interface SchoolSummaryProjection {
    String getId();
    String getCode();
    String getName();
    String getStatus();
    long getAdminCount();
    LocalDateTime getCreatedAt();
}
