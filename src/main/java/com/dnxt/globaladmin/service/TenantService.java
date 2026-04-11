package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.dto.TenantCreateRequest;
import com.dnxt.globaladmin.dto.TenantOnboardRequest;
import com.dnxt.globaladmin.entity.PlatformTenant;
import com.dnxt.globaladmin.entity.TenantModule;
import com.dnxt.globaladmin.repository.PlatformTenantRepository;
import com.dnxt.globaladmin.repository.TenantModuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private static final List<String> ALL_MODULES = List.of(
            "Operations", "Reviewer", "Publisher", "EDMS", "Planner", "Support", "Consulting"
    );

    @Autowired
    private PlatformTenantRepository tenantRepository;

    @Autowired
    private TenantModuleRepository moduleRepository;

    @Autowired
    private OperationsProvisioningService operationsProvisioning;

    @Autowired
    private FeatureService featureService;

    @Transactional(readOnly = true)
    public List<PlatformTenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PlatformTenant> getActiveTenants() {
        return tenantRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public PlatformTenant getTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    }

    @Transactional(readOnly = true)
    public List<TenantModule> getTenantModules(String tenantId) {
        return moduleRepository.findByTenantId(tenantId);
    }

    @Transactional
    public PlatformTenant createTenant(TenantCreateRequest request, String createdBy) {
        String slug = slugify(request.getTenantName());
        if (tenantRepository.existsByTenantSlug(slug)) {
            throw new IllegalArgumentException("Tenant slug already exists: " + slug);
        }

        PlatformTenant tenant = new PlatformTenant();
        tenant.setTenantId(UUID.randomUUID().toString());
        tenant.setTenantName(request.getTenantName());
        tenant.setTenantSlug(slug);
        tenant.setDomain(request.getDomain());
        tenant.setIndustry(request.getIndustry());
        tenant.setPrimaryContactName(request.getPrimaryContactName());
        tenant.setPrimaryContactEmail(request.getPrimaryContactEmail());
        tenant.setPhone(request.getPhone());
        tenant.setAddress(request.getAddress());
        tenant.setStatus("Active");
        tenant.setLicenseType(request.getLicenseType());
        tenant.setLicenseExpiry(request.getLicenseExpiry() != null ? Date.valueOf(request.getLicenseExpiry()) : null);
        tenant.setMaxUsers(request.getMaxUsers() != null ? request.getMaxUsers() : 50);
        tenant.setNotes(request.getNotes());
        tenant.setCreatedBy(createdBy);
        tenant.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        tenant.setModifiedBy(createdBy);
        tenant.setModifiedDate(new Timestamp(System.currentTimeMillis()));
        tenant.setIsActive(true);

        tenantRepository.save(tenant);

        // Create module entries (all disabled by default, with license defaults)
        for (String moduleName : ALL_MODULES) {
            TenantModule module = new TenantModule();
            module.setModuleId(UUID.randomUUID().toString());
            module.setTenantId(tenant.getTenantId());
            module.setModuleName(moduleName);
            module.setIsEnabled(false);
            module.setLicensedUsers(0);
            module.setLicenseType("Concurrent");
            module.setCreatedBy(createdBy);
            module.setCreatedDate(new Timestamp(System.currentTimeMillis()));
            module.setModifiedBy(createdBy);
            module.setModifiedDate(new Timestamp(System.currentTimeMillis()));
            moduleRepository.save(module);
        }

        log.info("Created tenant: {} (slug: {})", tenant.getTenantName(), tenant.getTenantSlug());
        return tenant;
    }

    @Transactional
    public PlatformTenant updateTenant(String tenantId, TenantCreateRequest request, String modifiedBy) {
        PlatformTenant tenant = getTenant(tenantId);

        tenant.setTenantName(request.getTenantName());
        if (request.getDomain() != null) tenant.setDomain(request.getDomain());
        if (request.getIndustry() != null) tenant.setIndustry(request.getIndustry());
        tenant.setPrimaryContactName(request.getPrimaryContactName());
        tenant.setPrimaryContactEmail(request.getPrimaryContactEmail());
        if (request.getPhone() != null) tenant.setPhone(request.getPhone());
        if (request.getAddress() != null) tenant.setAddress(request.getAddress());
        if (request.getLicenseType() != null) tenant.setLicenseType(request.getLicenseType());
        if (request.getLicenseExpiry() != null) tenant.setLicenseExpiry(Date.valueOf(request.getLicenseExpiry()));
        if (request.getMaxUsers() != null) tenant.setMaxUsers(request.getMaxUsers());
        if (request.getNotes() != null) tenant.setNotes(request.getNotes());
        tenant.setModifiedBy(modifiedBy);
        tenant.setModifiedDate(new Timestamp(System.currentTimeMillis()));

        tenantRepository.save(tenant);
        log.info("Updated tenant: {}", tenant.getTenantName());
        return tenant;
    }

    @Transactional
    public void deactivateTenant(String tenantId, String modifiedBy) {
        PlatformTenant tenant = getTenant(tenantId);
        tenant.setIsActive(false);
        tenant.setStatus("Inactive");
        tenant.setModifiedBy(modifiedBy);
        tenant.setModifiedDate(new Timestamp(System.currentTimeMillis()));
        tenantRepository.save(tenant);

        // Disable all modules
        List<TenantModule> modules = moduleRepository.findByTenantId(tenantId);
        for (TenantModule module : modules) {
            module.setIsEnabled(false);
            module.setModifiedBy(modifiedBy);
            module.setModifiedDate(new Timestamp(System.currentTimeMillis()));
            moduleRepository.save(module);
        }

        log.info("Deactivated tenant: {} and disabled all modules", tenant.getTenantName());
    }

    @Transactional
    public void reactivateTenant(String tenantId, String modifiedBy) {
        PlatformTenant tenant = getTenant(tenantId);
        tenant.setIsActive(true);
        tenant.setStatus("Active");
        tenant.setModifiedBy(modifiedBy);
        tenant.setModifiedDate(new Timestamp(System.currentTimeMillis()));
        tenantRepository.save(tenant);
        log.info("Reactivated tenant: {}", tenant.getTenantName());
    }

    @Transactional
    public TenantModule toggleModule(String tenantId, String moduleName, boolean enabled, String modifiedBy) {
        TenantModule module = moduleRepository.findByTenantIdAndModuleName(tenantId, moduleName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Module not found: " + moduleName + " for tenant: " + tenantId));

        module.setIsEnabled(enabled);
        if (enabled && module.getActivatedDate() == null) {
            module.setActivatedDate(new Date(System.currentTimeMillis()));
        }
        module.setModifiedBy(modifiedBy);
        module.setModifiedDate(new Timestamp(System.currentTimeMillis()));
        moduleRepository.save(module);

        // Provision in downstream service when a module is enabled
        if (enabled && "Operations".equals(moduleName)) {
            PlatformTenant tenant = getTenant(tenantId);
            try {
                operationsProvisioning.provisionTenant(tenant);
            } catch (Exception e) {
                log.warn("Module enabled but Operations provisioning failed: {}", e.getMessage());
            }
        }

        log.info("Module {} {} for tenant {}", moduleName, enabled ? "enabled" : "disabled", tenantId);
        return module;
    }

    @Transactional
    public Map<String, Object> onboardTenant(TenantOnboardRequest request, String createdBy) {
        // Step 1: Create the tenant
        TenantCreateRequest createReq = new TenantCreateRequest();
        createReq.setTenantName(request.getTenantName());
        createReq.setDomain(request.getDomain());
        createReq.setIndustry(request.getIndustry());
        createReq.setPrimaryContactName(request.getPrimaryContactName());
        createReq.setPrimaryContactEmail(request.getPrimaryContactEmail());
        createReq.setPhone(request.getPhone());
        createReq.setAddress(request.getAddress());
        createReq.setLicenseType(request.getLicenseType());
        createReq.setLicenseExpiry(request.getLicenseExpiry());
        createReq.setMaxUsers(request.getMaxUsers());
        createReq.setNotes(request.getNotes());

        PlatformTenant tenant = createTenant(createReq, createdBy);

        // Step 2: Enable modules with license configuration
        List<String> enabledModuleNames = new java.util.ArrayList<>();

        // Use enhanced module config if provided, otherwise fall back to enabledModules list
        if (request.getModules() != null && !request.getModules().isEmpty()) {
            for (TenantOnboardRequest.ModuleLicenseConfig modConfig : request.getModules()) {
                if (modConfig.isEnabled()) {
                    try {
                        TenantModule module = toggleModule(tenant.getTenantId(), modConfig.getModuleName(), true, createdBy);
                        // Apply license config
                        if (modConfig.getLicenseType() != null) module.setLicenseType(modConfig.getLicenseType());
                        if (modConfig.getLicensePermits() != null) module.setLicensedUsers(modConfig.getLicensePermits());
                        if (modConfig.getExpiryDate() != null) module.setExpiryDate(Date.valueOf(modConfig.getExpiryDate()));
                        if (modConfig.getPlanId() != null) module.setPlanId(modConfig.getPlanId());
                        module.setModifiedBy(createdBy);
                        module.setModifiedDate(new Timestamp(System.currentTimeMillis()));
                        moduleRepository.save(module);
                        enabledModuleNames.add(modConfig.getModuleName());
                    } catch (Exception e) {
                        log.warn("Failed to enable module {} for tenant {}: {}",
                                modConfig.getModuleName(), tenant.getTenantName(), e.getMessage());
                    }
                }
            }
        } else if (request.getEnabledModules() != null) {
            // Legacy path — simple module name list without license config
            for (String moduleName : request.getEnabledModules()) {
                try {
                    toggleModule(tenant.getTenantId(), moduleName, true, createdBy);
                    enabledModuleNames.add(moduleName);
                } catch (Exception e) {
                    log.warn("Failed to enable module {} for tenant {}: {}",
                            moduleName, tenant.getTenantName(), e.getMessage());
                }
            }
        }

        // Step 3: Provision in downstream services for enabled modules
        Map<String, Object> provisioningResults = new LinkedHashMap<>();

        if (enabledModuleNames.contains("Operations")) {
            // Resolve feature list from plan + overrides
            List<String> enabledFeatures = featureService.getEnabledFeatures(tenant.getTenantId(), "Operations");
            Map<String, Object> opsResult = operationsProvisioning.provisionTenant(tenant, enabledFeatures);
            provisioningResults.put("operations", opsResult);
            log.info("Operations provisioning result for {}: {} (features: {})",
                    tenant.getTenantName(), opsResult.get("status"), enabledFeatures);
        }

        // TODO: Future — provision Reviewer, Publisher, EDMS, Planner when those integrations are built

        log.info("Onboarded tenant: {} with modules: {}", tenant.getTenantName(), enabledModuleNames);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenant", tenant);
        result.put("modules", moduleRepository.findByTenantId(tenant.getTenantId()));
        result.put("provisioning", provisioningResults);
        result.put("status", "onboarded");
        return result;
    }

    private String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
