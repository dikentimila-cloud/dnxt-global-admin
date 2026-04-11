package com.dnxt.globaladmin.repository;

import com.dnxt.globaladmin.entity.PlatformTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformTenantRepository extends JpaRepository<PlatformTenant, String> {

    Optional<PlatformTenant> findByTenantSlug(String tenantSlug);

    List<PlatformTenant> findByIsActiveTrue();

    List<PlatformTenant> findByStatus(String status);

    boolean existsByTenantSlug(String tenantSlug);

    @Query("SELECT COUNT(t) FROM PlatformTenant t WHERE t.isActive = true")
    long countActive();

    @Query("SELECT COUNT(t) FROM PlatformTenant t WHERE t.status = :status")
    long countByStatus(String status);
}
