package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.entity.PlatformTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provisions tenants in the Operations service.
 * Flow:
 * 1. Authenticate with Operations API (get JWT)
 * 2. Call POST /api/settings/tenants with tenant data
 * 3. Operations creates: tenant record, customer record, admin user, employee, welcome email
 */
@Service
public class OperationsProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(OperationsProvisioningService.class);

    @Value("${admin.operations-service.url:http://localhost:8102}")
    private String operationsUrl;

    @Value("${admin.operations-service.username:admin}")
    private String opsUsername;

    @Value("${admin.operations-service.password:admin123}")
    private String opsPassword;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Provision a tenant in the Operations service.
     * Returns the provisioning result including temp password for the tenant admin.
     */
    public Map<String, Object> provisionTenant(PlatformTenant tenant) {
        return provisionTenant(tenant, null);
    }

    public Map<String, Object> provisionTenant(PlatformTenant tenant, java.util.List<String> enabledFeatures) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // Step 1: Authenticate with Operations
            String opsToken = authenticateWithOperations();
            if (opsToken == null) {
                result.put("status", "failed");
                result.put("error", "Failed to authenticate with Operations service");
                return result;
            }

            // Step 2: Create tenant in Operations
            Map<String, Object> tenantPayload = buildTenantPayload(tenant);
            if (enabledFeatures != null) {
                tenantPayload.put("enabledFeatures", enabledFeatures);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(opsToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(tenantPayload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    operationsUrl + "/api/settings/tenants",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                Object data = body.get("data");
                result.put("status", "provisioned");
                result.put("operationsResult", data);
                log.info("Successfully provisioned tenant '{}' in Operations", tenant.getTenantName());
            } else {
                result.put("status", "failed");
                result.put("error", "Operations returned: " + response.getStatusCode());
                log.warn("Operations provisioning failed for '{}': {}", tenant.getTenantName(), response.getStatusCode());
            }

        } catch (Exception e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
            log.error("Failed to provision tenant '{}' in Operations: {}", tenant.getTenantName(), e.getMessage());
        }

        return result;
    }

    /**
     * Deactivate a tenant in Operations.
     */
    public Map<String, Object> deactivateTenant(String tenantSlug) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            String opsToken = authenticateWithOperations();
            if (opsToken == null) {
                result.put("status", "failed");
                result.put("error", "Failed to authenticate with Operations service");
                return result;
            }

            // Find tenant by slug, then deactivate
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(opsToken);

            // Get all tenants to find the one matching our slug
            ResponseEntity<Map> listResponse = restTemplate.exchange(
                    operationsUrl + "/api/settings/tenants",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (listResponse.getStatusCode().is2xxSuccessful() && listResponse.getBody() != null) {
                result.put("status", "deactivation_requested");
                log.info("Tenant deactivation requested in Operations for slug: {}", tenantSlug);
            }

        } catch (Exception e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
            log.error("Failed to deactivate tenant in Operations: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Check if Operations service is reachable.
     */
    public boolean isOperationsAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    operationsUrl + "/actuator/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Operations service not reachable: {}", e.getMessage());
            return false;
        }
    }

    private String authenticateWithOperations() {
        try {
            Map<String, String> loginPayload = Map.of(
                    "username", opsUsername,
                    "password", opsPassword
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(loginPayload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    operationsUrl + "/api/auth/login",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                Map data = (Map) body.get("data");
                if (data != null) {
                    return (String) data.get("token");
                }
            }

            log.warn("Operations login failed: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Failed to authenticate with Operations: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildTenantPayload(PlatformTenant tenant) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantName", tenant.getTenantName());
        payload.put("tenantSlug", tenant.getTenantSlug());
        payload.put("domain", tenant.getDomain());
        payload.put("industry", tenant.getIndustry());
        payload.put("primaryContactName", tenant.getPrimaryContactName());
        payload.put("primaryContactEmail", tenant.getPrimaryContactEmail());
        payload.put("phone", tenant.getPhone());
        payload.put("address", tenant.getAddress());
        payload.put("status", "Active");
        payload.put("licenseType", tenant.getLicenseType());
        payload.put("maxUsers", tenant.getMaxUsers());
        payload.put("notes", tenant.getNotes());
        payload.put("isActive", true);
        return payload;
    }
}
