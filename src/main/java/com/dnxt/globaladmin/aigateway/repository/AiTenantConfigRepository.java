package com.dnxt.globaladmin.aigateway.repository;

import com.dnxt.globaladmin.aigateway.entity.AiTenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiTenantConfigRepository extends JpaRepository<AiTenantConfig, String> {
}
