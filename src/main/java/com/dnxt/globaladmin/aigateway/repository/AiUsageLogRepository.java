package com.dnxt.globaladmin.aigateway.repository;

import com.dnxt.globaladmin.aigateway.entity.AiUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, String> {

    Page<AiUsageLog> findByOrderByCreatedDateDesc(Pageable pageable);

    Page<AiUsageLog> findByTenantIdOrderByCreatedDateDesc(String tenantId, Pageable pageable);

    List<AiUsageLog> findByCreatedDateAfterOrderByCreatedDateDesc(Timestamp after);

    @Query("SELECT u.tenantId, u.tenantName, COUNT(u), SUM(u.totalTokens), SUM(u.costTotal) " +
           "FROM AiUsageLog u WHERE u.createdDate >= :since GROUP BY u.tenantId, u.tenantName ORDER BY SUM(u.costTotal) DESC")
    List<Object[]> summarizeByTenantSince(Timestamp since);

    @Query("SELECT u.providerName, u.modelName, COUNT(u), SUM(u.totalTokens), SUM(u.costTotal) " +
           "FROM AiUsageLog u WHERE u.createdDate >= :since GROUP BY u.providerName, u.modelName ORDER BY SUM(u.costTotal) DESC")
    List<Object[]> summarizeByModelSince(Timestamp since);

    @Query("SELECT COUNT(u), COALESCE(SUM(u.totalTokens), 0), COALESCE(SUM(u.costTotal), 0) " +
           "FROM AiUsageLog u WHERE u.createdDate >= :since")
    Object[] totalsSince(Timestamp since);

    @Query("SELECT COUNT(u), COALESCE(SUM(u.totalTokens), 0), COALESCE(SUM(u.costTotal), 0) " +
           "FROM AiUsageLog u WHERE u.tenantId = :tenantId AND u.createdDate >= :since")
    Object[] totalsByTenantSince(String tenantId, Timestamp since);
}
