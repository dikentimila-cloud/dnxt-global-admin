package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.dto.TenantCreateRequest;
import com.dnxt.globaladmin.dto.TenantOnboardRequest;
import com.dnxt.globaladmin.entity.PlatformTenant;
import com.dnxt.globaladmin.entity.TenantModule;
import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.security.PermissionCheck;
import com.dnxt.globaladmin.service.AuditService;
import com.dnxt.globaladmin.service.SupportAccessService;
import com.dnxt.globaladmin.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private SupportAccessService supportAccessService;

    @GetMapping
    @PermissionCheck("TENANT_VIEW")
    public ResponseEntity<ApiResponse> getAllTenants() {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getAllTenants()));
    }

    @GetMapping("/active")
    @PermissionCheck("TENANT_VIEW")
    public ResponseEntity<ApiResponse> getActiveTenants() {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getActiveTenants()));
    }

    @GetMapping("/{tenantId}")
    @PermissionCheck("TENANT_VIEW")
    public ResponseEntity<ApiResponse> getTenant(@PathVariable String tenantId) {
        try {
            PlatformTenant tenant = tenantService.getTenant(tenantId);
            List<TenantModule> modules = tenantService.getTenantModules(tenantId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("tenant", tenant, "modules", modules)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    @PermissionCheck("TENANT_CREATE")
    public ResponseEntity<ApiResponse> createTenant(@Valid @RequestBody TenantCreateRequest request,
                                                     Authentication auth,
                                                     HttpServletRequest httpRequest) {
        try {
            PlatformTenant tenant = tenantService.createTenant(request, auth.getName());
            auditService.log(auth.getName(), null, "TENANT_CREATED",
                    "TENANT", tenant.getTenantId(), tenant.getTenantName(),
                    "Slug: " + tenant.getTenantSlug(), httpRequest);
            return ResponseEntity.ok(ApiResponse.ok(tenant));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{tenantId}")
    @PermissionCheck("TENANT_EDIT")
    public ResponseEntity<ApiResponse> updateTenant(@PathVariable String tenantId,
                                                     @Valid @RequestBody TenantCreateRequest request,
                                                     Authentication auth,
                                                     HttpServletRequest httpRequest) {
        try {
            PlatformTenant tenant = tenantService.updateTenant(tenantId, request, auth.getName());
            auditService.log(auth.getName(), null, "TENANT_UPDATED",
                    "TENANT", tenantId, tenant.getTenantName(), null, httpRequest);
            return ResponseEntity.ok(ApiResponse.ok(tenant));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{tenantId}/deactivate")
    @PermissionCheck("TENANT_DEACTIVATE")
    public ResponseEntity<ApiResponse> deactivateTenant(@PathVariable String tenantId,
                                                         Authentication auth,
                                                         HttpServletRequest httpRequest) {
        try {
            PlatformTenant tenant = tenantService.getTenant(tenantId);
            tenantService.deactivateTenant(tenantId, auth.getName());
            auditService.log(auth.getName(), null, "TENANT_DEACTIVATED",
                    "TENANT", tenantId, tenant.getTenantName(),
                    "All modules disabled", httpRequest);
            return ResponseEntity.ok(ApiResponse.ok("Tenant deactivated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{tenantId}/reactivate")
    @PermissionCheck("TENANT_EDIT")
    public ResponseEntity<ApiResponse> reactivateTenant(@PathVariable String tenantId,
                                                         Authentication auth,
                                                         HttpServletRequest httpRequest) {
        try {
            PlatformTenant tenant = tenantService.getTenant(tenantId);
            tenantService.reactivateTenant(tenantId, auth.getName());
            auditService.log(auth.getName(), null, "TENANT_REACTIVATED",
                    "TENANT", tenantId, tenant.getTenantName(), null, httpRequest);
            return ResponseEntity.ok(ApiResponse.ok("Tenant reactivated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{tenantId}/modules/{moduleName}/toggle")
    @PermissionCheck("MODULE_MANAGE")
    public ResponseEntity<ApiResponse> toggleModule(@PathVariable String tenantId,
                                                     @PathVariable String moduleName,
                                                     @RequestBody Map<String, Boolean> body,
                                                     Authentication auth,
                                                     HttpServletRequest httpRequest) {
        try {
            boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
            TenantModule module = tenantService.toggleModule(tenantId, moduleName, enabled, auth.getName());
            auditService.log(auth.getName(), null, enabled ? "MODULE_ENABLED" : "MODULE_DISABLED",
                    "MODULE", module.getModuleId(), moduleName,
                    "Tenant: " + tenantId, httpRequest);
            return ResponseEntity.ok(ApiResponse.ok(module));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/onboard")
    @PermissionCheck("TENANT_ONBOARD")
    public ResponseEntity<ApiResponse> onboardTenant(@Valid @RequestBody TenantOnboardRequest request,
                                                      Authentication auth,
                                                      HttpServletRequest httpRequest) {
        try {
            Map<String, Object> result = tenantService.onboardTenant(request, auth.getName());
            PlatformTenant tenant = (PlatformTenant) result.get("tenant");
            auditService.log(auth.getName(), null, "TENANT_ONBOARDED",
                    "TENANT", tenant.getTenantId(), tenant.getTenantName(),
                    "Modules: " + request.getEnabledModules(), httpRequest);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Support Access (Login-As)
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/{tenantId}/support-access")
    @PermissionCheck("SUPPORT_ACCESS")
    public ResponseEntity<ApiResponse> initiateSupportAccess(
            @PathVariable String tenantId,
            Authentication auth,
            HttpServletRequest httpRequest) {
        try {
            // Validate tenant exists and is active
            PlatformTenant tenant = tenantService.getTenant(tenantId);
            if (!Boolean.TRUE.equals(tenant.getIsActive())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Tenant is not active"));
            }

            // Check Operations module is enabled
            List<TenantModule> modules = tenantService.getTenantModules(tenantId);
            boolean opsEnabled = modules.stream()
                    .anyMatch(m -> "Operations".equals(m.getModuleName()) && Boolean.TRUE.equals(m.getIsEnabled()));
            if (!opsEnabled) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Operations module is not enabled for this tenant"));
            }

            // Check if tenant has support access enabled
            if (!supportAccessService.isSupportAccessEnabled()) {
                auditService.log(auth.getName(), null, "SUPPORT_ACCESS_BLOCKED",
                        "TENANT", tenantId, tenant.getTenantName(),
                        "Tenant has disabled support access", httpRequest);
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("This tenant has disabled support access. Contact the tenant administrator to enable it."));
            }

            // Get the requesting admin's email
            String requestedBy = auth.getName(); // userId
            // TODO: resolve to email from AdminUser table

            // Generate one-time code from Operations
            String code = supportAccessService.generateSupportCode(requestedBy, "Support access via Global Admin");

            // Build redirect URL
            String redirectUrl = supportAccessService.buildRedirectUrlDirect(code);

            // Audit log
            auditService.log(auth.getName(), null, "SUPPORT_ACCESS_INITIATED",
                    "TENANT", tenantId, tenant.getTenantName(),
                    "Redirect: " + tenant.getTenantSlug() + "-operations", httpRequest);

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "redirectUrl", redirectUrl,
                    "tenantName", tenant.getTenantName(),
                    "expiresInSeconds", 300
            )));

        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }
}
