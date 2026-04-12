package com.dnxt.globaladmin.aigateway.repository;

import com.dnxt.globaladmin.aigateway.entity.AiCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AiCredentialRepository extends JpaRepository<AiCredential, String> {
    Optional<AiCredential> findByProviderId(String providerId);
    void deleteByProviderId(String providerId);
}
