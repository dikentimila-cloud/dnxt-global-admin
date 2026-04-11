package com.dnxt.globaladmin.repository;

import com.dnxt.globaladmin.entity.PlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, String> {

    List<PlatformConfig> findByCategory(String category);

    List<PlatformConfig> findByCategoryOrderByConfigKeyAsc(String category);
}
