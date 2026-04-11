package com.dnxt.globaladmin.repository;

import com.dnxt.globaladmin.entity.TenantModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantModuleRepository extends JpaRepository<TenantModule, String> {

    List<TenantModule> findByTenantId(String tenantId);

    Optional<TenantModule> findByTenantIdAndModuleName(String tenantId, String moduleName);

    @Query("SELECT COUNT(tm) FROM TenantModule tm WHERE tm.isEnabled = true AND tm.moduleName = :moduleName")
    long countEnabledByModule(String moduleName);

    @Query("SELECT COUNT(tm) FROM TenantModule tm WHERE tm.isEnabled = true")
    long countAllEnabled();
}
