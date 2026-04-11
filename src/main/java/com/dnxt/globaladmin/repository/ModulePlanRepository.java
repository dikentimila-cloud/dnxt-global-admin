package com.dnxt.globaladmin.repository;

import com.dnxt.globaladmin.entity.ModulePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModulePlanRepository extends JpaRepository<ModulePlan, String> {

    List<ModulePlan> findByModuleNameAndIsActiveTrueOrderBySortOrderAsc(String moduleName);

    Optional<ModulePlan> findByModuleNameAndPlanName(String moduleName, String planName);
}
