package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.entity.ModulePlan;
import com.dnxt.globaladmin.entity.TenantModule;
import com.dnxt.globaladmin.repository.ModulePlanRepository;
import com.dnxt.globaladmin.repository.TenantModuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Resolves the final feature list for a tenant's module by combining:
 * 1. Plan features (from module_plan.features JSON array)
 * 2. Custom overrides (from tenant_module.feature_overrides JSON: {"add":[], "remove":[]})
 */
@Service
public class FeatureService {

    private static final Logger log = LoggerFactory.getLogger(FeatureService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TenantModuleRepository moduleRepository;

    @Autowired
    private ModulePlanRepository planRepository;

    /**
     * Get the final enabled features for a tenant's module.
     * Returns ALL features if no plan is assigned (backward compat).
     */
    public List<String> getEnabledFeatures(String tenantId, String moduleName) {
        TenantModule tenantModule = moduleRepository.findByTenantIdAndModuleName(tenantId, moduleName)
                .orElse(null);

        if (tenantModule == null || !Boolean.TRUE.equals(tenantModule.getIsEnabled())) {
            return List.of();
        }

        // If no plan assigned, return all features (backward compat / Enterprise default)
        if (tenantModule.getPlanId() == null) {
            return getAllFeaturesForModule(moduleName);
        }

        // Load plan features
        ModulePlan plan = planRepository.findById(tenantModule.getPlanId()).orElse(null);
        if (plan == null) {
            log.warn("Plan not found: {} for tenant module {}:{}", tenantModule.getPlanId(), tenantId, moduleName);
            return getAllFeaturesForModule(moduleName);
        }

        Set<String> features = new LinkedHashSet<>(parseJsonArray(plan.getFeatures()));

        // Apply overrides
        String overridesJson = tenantModule.getFeatureOverrides();
        if (overridesJson != null && !overridesJson.isBlank() && !overridesJson.equals("{}")) {
            try {
                Map<String, List<String>> overrides = mapper.readValue(overridesJson, new TypeReference<>() {});
                List<String> add = overrides.getOrDefault("add", List.of());
                List<String> remove = overrides.getOrDefault("remove", List.of());
                features.addAll(add);
                features.removeAll(remove);
            } catch (Exception e) {
                log.warn("Failed to parse feature overrides for tenant {}:{}: {}", tenantId, moduleName, e.getMessage());
            }
        }

        return new ArrayList<>(features);
    }

    /**
     * Get ALL features for a module (Enterprise / no-plan default).
     */
    public List<String> getAllFeaturesForModule(String moduleName) {
        // Find the plan with the most features (Enterprise)
        List<ModulePlan> plans = planRepository.findByModuleNameAndIsActiveTrueOrderBySortOrderAsc(moduleName);
        if (!plans.isEmpty()) {
            ModulePlan enterprise = plans.get(plans.size() - 1); // Last = highest tier
            return parseJsonArray(enterprise.getFeatures());
        }

        // Hardcoded fallback for Operations
        if ("Operations".equals(moduleName)) {
            return List.of("dashboard", "timesheets", "employees", "customers", "revenue", "pnl",
                    "banking", "payroll", "consulting", "invoices", "reports",
                    "object-manager", "workflows", "user-management", "settings");
        }

        return List.of();
    }

    private List<String> parseJsonArray(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse feature JSON: {}", e.getMessage());
            return List.of();
        }
    }
}
