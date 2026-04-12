package com.dnxt.globaladmin.aigateway.repository;

import com.dnxt.globaladmin.aigateway.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiModelRepository extends JpaRepository<AiModel, String> {
    List<AiModel> findByProviderIdOrderByModelDisplayNameAsc(String providerId);
    List<AiModel> findAllByOrderByModelDisplayNameAsc();
    Optional<AiModel> findByProviderIdAndModelName(String providerId, String modelName);
    void deleteByProviderId(String providerId);
}
