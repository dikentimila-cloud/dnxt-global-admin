package com.dnxt.globaladmin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * Thin proxy from dnxt-global-admin frontend to globalservices's Phase 1
 * release-framework endpoints. Frontend hits /api/releases/* which we forward
 * to globalservices /platform/release/*.
 *
 * <p>See {@code docs/system-metadata-release-design.md} for the design.
 *
 * <p>Marker: release-proxy-phase1-v1
 */
@RestController
@RequestMapping("/api/releases")
public class ReleaseController {

    private static final Logger log = LoggerFactory.getLogger(ReleaseController.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private String gsBase() {
        String env = System.getenv("GLOBALSERVICES_URL");
        if (env != null && !env.isEmpty()) return env;
        // Self-healing fallback for ACA: construct via the env DNS suffix.
        String dns = System.getenv("CONTAINER_APP_ENV_DNS_SUFFIX");
        if (dns != null && !dns.isEmpty()) {
            return "https://globalservices.internal." + dns + "/globalservices";
        }
        return "http://localhost:8080/globalservices";
    }

    @GetMapping("/list")
    public ResponseEntity<Object> list() {
        return forward("/platform/release/list");
    }

    @GetMapping("/tenants")
    public ResponseEntity<Object> tenants() {
        return forward("/platform/release/tenants");
    }

    @GetMapping("/drift")
    public ResponseEntity<Object> drift(@RequestParam("tenantId") String tenantId,
                                         @RequestParam(value = "details", defaultValue = "false") boolean details) {
        return forward("/platform/release/drift?tenantId=" + tenantId + "&details=" + details);
    }

    @GetMapping("/version/{version:.+}")
    public ResponseEntity<Object> getRelease(@PathVariable("version") String version) {
        return forward("/platform/release/" + version);
    }

    private ResponseEntity<Object> forward(String suffix) {
        String url = gsBase() + suffix;
        try {
            ResponseEntity<Object> r = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.status(r.getStatusCode()).body(r.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("[releases proxy] {} -> {} {}", suffix, e.getRawStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getRawStatusCode()).body(
                Collections.singletonMap("error", e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("[releases proxy] {} failed: {}", suffix, e.getMessage());
            return ResponseEntity.status(500).body(
                Collections.singletonMap("error", e.getMessage()));
        }
    }
}
