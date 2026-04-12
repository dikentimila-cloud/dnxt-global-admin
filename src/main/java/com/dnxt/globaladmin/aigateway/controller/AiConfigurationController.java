package com.dnxt.globaladmin.aigateway.controller;

import com.dnxt.globaladmin.aigateway.dto.*;
import com.dnxt.globaladmin.aigateway.service.AiClientService;
import com.dnxt.globaladmin.aigateway.service.AiConfigService;
import com.dnxt.globaladmin.aigateway.service.AiUsageService;
import com.dnxt.globaladmin.aigateway.service.ResolvedAiConfig;
import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.security.PermissionCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/aigateway")
public class AiConfigurationController {

    private static final Logger log = LoggerFactory.getLogger(AiConfigurationController.class);

    private final AiConfigService configService;
    private final AiClientService clientService;
    private final AiUsageService usageService;

    @Autowired
    public AiConfigurationController(AiConfigService configService, AiClientService clientService, AiUsageService usageService) {
        this.configService = configService;
        this.clientService = clientService;
        this.usageService = usageService;
    }

    private static String actor(Authentication auth) { return auth != null ? auth.getName() : "system"; }
    private ResponseEntity<ApiResponse> ok(Object data) { return ResponseEntity.ok(ApiResponse.ok(data)); }
    private ResponseEntity<ApiResponse> bad(String msg) { return ResponseEntity.badRequest().body(ApiResponse.error(msg)); }
    private ResponseEntity<ApiResponse> serverError(String prefix, Exception e) {
        log.error("{}: {}", prefix, e.getMessage(), e);
        return ResponseEntity.status(500).body(ApiResponse.error(prefix + ": " + e.getMessage()));
    }

    @GetMapping("/admin/state")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> getState() {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("providers", configService.listProviders());
            body.put("models", configService.listModels(null));
            body.put("credentials", configService.listCredentials());
            body.put("tenantConfig", configService.getTenantConfig());
            return ok(body);
        } catch (Exception e) { return serverError("Failed to load AI configuration", e); }
    }

    // ── Providers ───────────────────────────────────────────────────────────

    @GetMapping("/admin/providers")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> listProviders() { return ok(configService.listProviders()); }

    @PostMapping("/admin/providers")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> createProvider(@RequestBody AiProviderDTO body, Authentication auth) {
        try { return ok(configService.saveProvider(body, actor(auth))); }
        catch (IllegalArgumentException e) { return bad(e.getMessage()); }
        catch (Exception e) { return serverError("Failed to save provider", e); }
    }

    @PutMapping("/admin/providers/{id}")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> updateProvider(@PathVariable("id") String id, @RequestBody AiProviderDTO body, Authentication auth) {
        try { body.providerId = id; return ok(configService.saveProvider(body, actor(auth))); }
        catch (IllegalArgumentException e) { return bad(e.getMessage()); }
        catch (Exception e) { return serverError("Failed to update provider", e); }
    }

    @DeleteMapping("/admin/providers/{id}")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> deleteProvider(@PathVariable("id") String id) {
        try { configService.deleteProvider(id); return ok(Map.of("deleted", true)); }
        catch (Exception e) { return serverError("Failed to delete provider", e); }
    }

    // ── Models ──────────────────────────────────────────────────────────────

    @GetMapping("/admin/models")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> listModels(@RequestParam(value = "providerId", required = false) String providerId) {
        return ok(configService.listModels(providerId));
    }

    @PostMapping("/admin/models")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> createModel(@RequestBody AiModelDTO body, Authentication auth) {
        try { return ok(configService.saveModel(body, actor(auth))); }
        catch (IllegalArgumentException e) { return bad(e.getMessage()); }
        catch (Exception e) { return serverError("Failed to save model", e); }
    }

    @PutMapping("/admin/models/{id}")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> updateModel(@PathVariable("id") String id, @RequestBody AiModelDTO body, Authentication auth) {
        try { body.modelId = id; return ok(configService.saveModel(body, actor(auth))); }
        catch (IllegalArgumentException e) { return bad(e.getMessage()); }
        catch (Exception e) { return serverError("Failed to update model", e); }
    }

    @DeleteMapping("/admin/models/{id}")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> deleteModel(@PathVariable("id") String id) {
        try { configService.deleteModel(id); return ok(Map.of("deleted", true)); }
        catch (Exception e) { return serverError("Failed to delete model", e); }
    }

    // ── Credentials ─────────────────────────────────────────────────────────

    @GetMapping("/admin/credentials")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> listCredentials() { return ok(configService.listCredentials()); }

    @PostMapping("/admin/credentials")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> saveCredential(@RequestBody AiCredentialDTO body, Authentication auth) {
        try { return ok(configService.saveCredential(body, actor(auth))); }
        catch (IllegalArgumentException e) { return bad(e.getMessage()); }
        catch (Exception e) { return serverError("Failed to save credential", e); }
    }

    @DeleteMapping("/admin/credentials/{providerId}")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> deleteCredential(@PathVariable("providerId") String providerId) {
        try { configService.deleteCredential(providerId); return ok(Map.of("deleted", true)); }
        catch (Exception e) { return serverError("Failed to delete credential", e); }
    }

    // ── Tenant config ───────────────────────────────────────────────────────

    @GetMapping("/admin/tenant")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> getTenant() { return ok(configService.getTenantConfig()); }

    @PutMapping("/admin/tenant")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> saveTenant(@RequestBody AiTenantConfigDTO body, Authentication auth) {
        try { return ok(configService.saveTenantConfig(body, actor(auth))); }
        catch (Exception e) { return serverError("Failed to save tenant config", e); }
    }

    // ── Quick setup / active / test ─────────────────────────────────────────

    @PostMapping("/admin/quick-setup")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> quickSetup(Authentication auth) {
        try { return ok(configService.quickSetup(actor(auth))); }
        catch (Exception e) { return serverError("Quick setup failed", e); }
    }

    @GetMapping("/admin/active")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> getActive() {
        ResolvedAiConfig cfg = configService.resolveActiveConfig();
        if (cfg == null) return ok(null);
        Map<String, Object> body = new HashMap<>();
        body.put("provider", AiProviderDTO.fromEntity(cfg.provider));
        body.put("model", AiModelDTO.fromEntity(cfg.model));
        body.put("endpoint", cfg.endpoint);
        body.put("hasApiKey", cfg.apiKey != null);
        return ok(body);
    }

    @PostMapping("/admin/test")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> test(@RequestBody(required = false) Map<String, String> body, Authentication auth) {
        try {
            String providerId = body != null ? body.get("providerId") : null;
            ResolvedAiConfig cfg = (providerId != null && !providerId.isBlank())
                    ? configService.resolveProviderConfig(providerId) : configService.resolveActiveConfig();
            if (cfg == null) return bad("No active AI configuration. Enable AI and pick a default provider.");
            String reply = clientService.chatCompletionWith(cfg, "Reply with the single word: ok",
                    AiClientService.Options.defaults().maxTokens(20),
                    "global-admin", "Global Admin", "TEST", actor(auth));
            Map<String, Object> result = new HashMap<>();
            result.put("ok", true);
            result.put("provider", cfg.provider.getDisplayName());
            result.put("model", cfg.model.getModelDisplayName());
            result.put("reply", reply);
            return ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) { return bad(e.getMessage()); }
        catch (Exception e) { return serverError("Test failed", e); }
    }

    // ── Usage tracking ──────────────────────────────────────────────────────

    @GetMapping("/admin/usage/summary")
    @PermissionCheck("CONFIG_VIEW")
    public ResponseEntity<ApiResponse> usageSummary(@RequestParam(value = "days", defaultValue = "30") int days) {
        try { return ok(usageService.getSummary(days)); }
        catch (Exception e) { return serverError("Failed to load usage", e); }
    }

    @GetMapping("/admin/usage/recent")
    @PermissionCheck("CONFIG_VIEW")
    public ResponseEntity<ApiResponse> usageRecent(@RequestParam(value = "page", defaultValue = "0") int page,
                                                    @RequestParam(value = "size", defaultValue = "50") int size) {
        try { return ok(usageService.getRecentUsage(page, size)); }
        catch (Exception e) { return serverError("Failed to load usage", e); }
    }

    @GetMapping("/admin/usage/tenant/{tenantId}")
    @PermissionCheck("CONFIG_VIEW")
    public ResponseEntity<ApiResponse> usageTenant(@PathVariable("tenantId") String tenantId,
                                                    @RequestParam(value = "days", defaultValue = "30") int days) {
        try { return ok(usageService.getTenantUsage(tenantId, days)); }
        catch (Exception e) { return serverError("Failed to load tenant usage", e); }
    }
}
