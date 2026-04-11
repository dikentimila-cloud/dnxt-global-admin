package com.dnxt.globaladmin.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TenantCreateRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(max = 200)
    private String tenantName;

    private String domain;

    @Size(max = 100)
    private String industry;

    @NotBlank(message = "Primary contact name is required")
    private String primaryContactName;

    @NotBlank(message = "Primary contact email is required")
    @Email(message = "Invalid email format")
    private String primaryContactEmail;

    private String phone;
    private String address;

    @Size(max = 50)
    private String licenseType;

    private String licenseExpiry;

    @Min(1)
    private Integer maxUsers;

    private String notes;
}
