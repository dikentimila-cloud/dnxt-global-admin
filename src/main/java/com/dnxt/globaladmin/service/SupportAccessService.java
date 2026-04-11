package com.dnxt.globaladmin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Handles support access (Login-As) flow between Global Admin and Operations.
 * Uses internal networking URL + API key for service-to-service calls.
 */
@Service
public class SupportAccessService {

    private static final Logger log = LoggerFactory.getLogger(SupportAccessService.class);

    @Value("${admin.operations-service.internal-url:${OPERATIONS_INTERNAL_URL:http://localhost:8102}}")
    private String opsInternalUrl;

    @Value("${admin.operations-service.internal-api-key:${OPS_INTERNAL_API_KEY:}}")
    private String opsInternalApiKey;

    @Value("${admin.base-domain:${BASE_DOMAIN:dnxtcloud.com}}")
    private String baseDomain;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Check if the target tenant has support access enabled.
     */
    public boolean isSupportAccessEnabled() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", opsInternalApiKey);

            ResponseEntity<Map> response = restTemplate.exchange(
                    opsInternalUrl + "/api/internal/support-access-status",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map data = (Map) response.getBody().get("data");
                return data != null && Boolean.TRUE.equals(data.get("enabled"));
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to check support access status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate a one-time support access code from Operations.
     * Returns the code (NOT a JWT).
     */
    public String generateSupportCode(String requestedBy, String reason) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Api-Key", opsInternalApiKey);

        Map<String, String> body = Map.of(
                "requestedBy", requestedBy,
                "reason", reason != null ? reason : "Support access via Global Admin"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                opsInternalUrl + "/api/internal/support-token",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map data = (Map) response.getBody().get("data");
            if (data != null) {
                return (String) data.get("code");
            }
        }

        String errorMsg = "Unknown error";
        if (response.getBody() != null && response.getBody().get("message") != null) {
            errorMsg = (String) response.getBody().get("message");
        }
        throw new RuntimeException("Failed to generate support code: " + errorMsg);
    }

    /**
     * Build the redirect URL for the tenant's Operations frontend.
     */
    public String buildRedirectUrl(String tenantSlug, String code) {
        return "https://" + tenantSlug + "-operations." + baseDomain + "/support-login?code=" + code;
    }

    /**
     * Build redirect URL using ops.dnxtcloud.com (for the current single-instance setup).
     */
    public String buildRedirectUrlDirect(String code) {
        return "https://ops." + baseDomain + "/support-login?code=" + code;
    }
}
