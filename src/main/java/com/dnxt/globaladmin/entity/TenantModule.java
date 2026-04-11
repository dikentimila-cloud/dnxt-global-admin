package com.dnxt.globaladmin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "tenant_module")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TenantModule {

    @Id
    @Column(name = "module_id")
    private String moduleId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private PlatformTenant tenant;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "licensed_users")
    private Integer licensedUsers;

    @Column(name = "activated_date")
    private Date activatedDate;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @Column(name = "license_type")
    private String licenseType;

    @Column(name = "plan_id")
    private String planId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private ModulePlan plan;

    @Column(name = "feature_overrides")
    private String featureOverrides;

    @Column(name = "config_json")
    private String configJson;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_date")
    private Timestamp modifiedDate;
}
