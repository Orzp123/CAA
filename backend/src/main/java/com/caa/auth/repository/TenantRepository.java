package com.caa.auth.repository;

import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.school.dto.SchoolSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {

    Optional<Tenant> findByCode(String code);

    Optional<Tenant> findByDomain(String domain);

    List<Tenant> findAllByStatus(Tenant.TenantStatus status);

    /**
     * 按类型 + 名称/编码模糊搜索，支持分页。
     * name/code 参数传 null 时跳过对应条件。
     */
    @Query("SELECT t FROM Tenant t WHERE t.type = :type " +
           "AND (:name IS NULL OR t.name LIKE %:name%) " +
           "AND (:code IS NULL OR t.code LIKE %:code%)")
    Page<Tenant> searchByType(
            @Param("type") Tenant.TenantType type,
            @Param("name") String name,
            @Param("code") String code,
            Pageable pageable);

    /**
     * H-4 fix: 一次查询返回学校列表含管理员数量，消除 N+1 问题。
     * 使用 LEFT JOIN 聚合 accounts 表中 SCHOOL_ADMIN 类型账户数量。
     */
    @Query("SELECT t.id AS id, t.code AS code, t.name AS name, " +
           "t.status AS status, " +
           "COUNT(a.id) AS adminCount, t.createdAt AS createdAt " +
           "FROM Tenant t " +
           "LEFT JOIN Account a ON a.tenantId = t.id AND a.accountType = :adminType " +
           "WHERE t.type = :type " +
           "AND (:name IS NULL OR t.name LIKE %:name%) " +
           "AND (:code IS NULL OR t.code LIKE %:code%) " +
           "GROUP BY t.id, t.code, t.name, t.status, t.createdAt")
    Page<SchoolSummaryProjection> searchByTypeWithAdminCount(
            @Param("type") Tenant.TenantType type,
            @Param("adminType") Account.AccountType adminType,
            @Param("name") String name,
            @Param("code") String code,
            Pageable pageable);

}
