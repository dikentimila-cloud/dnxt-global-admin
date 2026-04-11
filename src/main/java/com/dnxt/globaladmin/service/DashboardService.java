package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private PlatformTenantRepository tenantRepository;

    @Autowired
    private TenantModuleRepository moduleRepository;

    @Autowired
    private AdminUserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OperationsProvisioningService operationsProvisioning;

    @Transactional(readOnly = true)
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // Tenant stats
        overview.put("totalTenants", tenantRepository.count());
        overview.put("activeTenants", tenantRepository.countActive());
        overview.put("inactiveTenants", tenantRepository.countByStatus("Inactive"));

        // Module stats
        overview.put("totalLicenses", moduleRepository.countAllEnabled());
        overview.put("operationsLicenses", moduleRepository.countEnabledByModule("Operations"));
        overview.put("reviewerLicenses", moduleRepository.countEnabledByModule("Reviewer"));
        overview.put("publisherLicenses", moduleRepository.countEnabledByModule("Publisher"));
        overview.put("edmsLicenses", moduleRepository.countEnabledByModule("EDMS"));
        overview.put("plannerLicenses", moduleRepository.countEnabledByModule("Planner"));

        // Admin user stats
        overview.put("totalAdminUsers", userRepository.count());
        overview.put("activeAdminUsers", userRepository.findByIsActiveTrue().size());

        // Audit stats
        overview.put("totalAuditEntries", auditLogRepository.count());

        // Service health
        overview.put("operationsServiceUp", operationsProvisioning.isOperationsAvailable());

        return overview;
    }

    public Map<String, Object> getPlatformHealth() {
        Map<String, Object> health = new LinkedHashMap<>();

        // Service status checks
        List<Map<String, Object>> services = new ArrayList<>();

        services.add(buildServiceStatus("Operations", operationsProvisioning.isOperationsAvailable(), "8102"));
        // Future: add Reviewer, Publisher, EDMS, Planner health checks here

        health.put("services", services);

        // License summary
        Map<String, Object> licenseSummary = new LinkedHashMap<>();
        for (String mod : List.of("Operations", "Reviewer", "Publisher", "EDMS", "Planner")) {
            licenseSummary.put(mod, Map.of(
                    "enabledCount", moduleRepository.countEnabledByModule(mod)
            ));
        }
        health.put("licenseSummary", licenseSummary);

        // Tenant summary
        health.put("activeTenants", tenantRepository.countActive());
        health.put("totalTenants", tenantRepository.count());

        return health;
    }

    private Map<String, Object> buildServiceStatus(String name, boolean isUp, String port) {
        Map<String, Object> svc = new LinkedHashMap<>();
        svc.put("name", name);
        svc.put("status", isUp ? "UP" : "DOWN");
        svc.put("port", port);
        return svc;
    }
}
