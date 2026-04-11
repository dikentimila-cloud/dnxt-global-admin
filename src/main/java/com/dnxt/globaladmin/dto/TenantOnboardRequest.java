package com.dnxt.globaladmin.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TenantOnboardRequest {

    @NotBlank(message = "Tenant name is required")
    private String tenantName;

    private String domain;
    private String industry;
    private String customerType;

    @NotBlank(message = "Primary contact name is required")
    private String primaryContactName;

    @NotBlank(message = "Primary contact email is required")
    @Email
    private String primaryContactEmail;

    private String phone;
    private String address;
    private String licenseType;
    private String licenseExpiry;

    @Min(1)
    private Integer maxUsers;

    private String notes;

    // Legacy field — kept for backward compatibility
    private List<String> enabledModules;

    // Enhanced per-module license configuration
    private List<ModuleLicenseConfig> modules;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ModuleLicenseConfig {
        private String moduleName;
        private boolean enabled;
        private String planId;           // FK to module_plan (e.g., "plan-ops-starter")
        private String licenseType;      // "Concurrent" or "Named"
        private String expiryDate;       // ISO date (yyyy-MM-dd)
        private Integer expiryDaysLeft;
        private Integer licensePermits;  // max concurrent users
    }
}
