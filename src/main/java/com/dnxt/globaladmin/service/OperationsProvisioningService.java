package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.entity.PlatformTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provisions tenants in the Operations service.
 *
 * Flow (internal API):
 * 1. POST /api/internal/tenants/provision with X-Internal-Api-Key
 * 2. Operations creates a dedicated ops_&lt;slug&gt; database, runs Flyway migrations,
 *    seeds the tenant row + Super Administrator user, returns a temp password.
 * 3. Global Admin emails the temp password to the primary contact.
 *
 * No user login dance. No shared admin credentials.
 */
@Service
public class OperationsProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(OperationsProvisioningService.class);

    @Value("${admin.operations-service.url:http://localhost:8102}")
    private String operationsUrl;

    @Value("${admin.operations-service.internal-api-key:${OPS_INTERNAL_API_KEY:}}")
    private String opsInternalApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Provision a tenant in the Operations service.
     * Returns the provisioning result including temp password for the tenant admin.
     */
    public Map<String, Object> provisionTenant(PlatformTenant tenant) {
        return provisionTenant(tenant, null);
    }

    public Map<String, Object> provisionTenant(PlatformTenant tenant, List<String> enabledFeatures) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (opsInternalApiKey == null || opsInternalApiKey.isBlank()) {
            result.put("status", "failed");
            result.put("error", "OPS_INTERNAL_API_KEY is not configured on Global Admin");
            log.error("Cannot provision tenant '{}': internal API key missing", tenant.getTenantName());
            return result;
        }

        Map<String, Object> payload = buildTenantPayload(tenant);
        if (enabledFeatures != null) {
            payload.put("enabledFeatures", enabledFeatures);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Api-Key", opsInternalApiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    operationsUrl + "/api/internal/tenants/provision",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map body = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                Object data = body.get("data");
                result.put("status", "provisioned");
                result.put("operationsResult", data);
                log.info("Provisioned tenant '{}' in Operations (slug: {})",
                        tenant.getTenantName(), tenant.getTenantSlug());
            } else {
                result.put("status", "failed");
                result.put("error", "Operations returned: " + response.getStatusCode());
                log.warn("Operations provisioning failed for '{}': {}",
                        tenant.getTenantName(), response.getStatusCode());
            }

        } catch (HttpStatusCodeException e) {
            result.put("status", "failed");
            result.put("error", extractErrorMessage(e));
            log.error("Operations provisioning returned {} for '{}': {}",
                    e.getStatusCode(), tenant.getTenantName(), e.getResponseBodyAsString());
        } catch (Exception e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
            log.error("Failed to provision tenant '{}' in Operations", tenant.getTenantName(), e);
        }

        return result;
    }

    private String extractErrorMessage(HttpStatusCodeException e) {
        try {
            Map<?, ?> body = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(e.getResponseBodyAsString(), Map.class);
            Object msg = body.get("error");
            if (msg == null) msg = body.get("message");
            return msg != null ? String.valueOf(msg) : ("HTTP " + e.getStatusCode());
        } catch (Exception ignored) {
            return "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        }
    }

    /**
     * Deactivate a tenant in Operations. Currently a no-op placeholder —
     * deactivation is tracked in Global Admin's platform_tenant table.
     * A future enhancement will add an /api/internal/tenants/deactivate
     * endpoint that marks ops_tenant.is_active=false in the tenant's DB.
     */
    public Map<String, Object> deactivateTenant(String tenantSlug) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "deactivation_recorded");
        log.info("Tenant deactivation recorded for slug: {} (Operations DB left intact)", tenantSlug);
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
