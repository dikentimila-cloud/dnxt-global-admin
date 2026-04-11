package com.dnxt.globaladmin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "platform_tenant")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PlatformTenant {

    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "tenant_name", nullable = false)
    private String tenantName;

    @Column(name = "tenant_slug", unique = true, nullable = false)
    private String tenantSlug;

    @Column(name = "domain")
    private String domain;

    @Column(name = "industry")
    private String industry;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_contact_name")
    private String primaryContactName;

    @Column(name = "primary_contact_email")
    private String primaryContactEmail;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "status")
    private String status;

    @Column(name = "license_type")
    private String licenseType;

    @Column(name = "license_expiry")
    private Date licenseExpiry;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_date")
    private Timestamp modifiedDate;

    @Column(name = "is_active")
    private Boolean isActive;
}
