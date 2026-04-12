package com.dnxt.globaladmin.aigateway.repository;

import com.dnxt.globaladmin.aigateway.entity.AiProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiProviderRepository extends JpaRepository<AiProvider, String> {
    Optional<AiProvider> findByProviderName(String providerName);
    List<AiProvider> findAllByOrderByDisplayNameAsc();
}
