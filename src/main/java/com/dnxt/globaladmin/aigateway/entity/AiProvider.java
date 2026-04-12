package com.dnxt.globaladmin.aigateway.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "aig_provider")
public class AiProvider {

    @Id
    @Column(name = "provider_id", length = 40)
    private String providerId;

    @Column(name = "provider_name", length = 100, nullable = false, unique = true)
    private String providerName;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "provider_type", length = 40, nullable = false)
    private String providerType;

    @Column(name = "base_endpoint", length = 500)
    private String baseEndpoint;

    @Column(name = "api_version", length = 40)
    private String apiVersion;

    @Column(name = "is_active", length = 1, nullable = false)
    private String isActive = "Y";

    @Column(name = "supports_streaming", length = 1, nullable = false)
    private String supportsStreaming = "N";

    @Column(name = "supports_functions", length = 1, nullable = false)
    private String supportsFunctions = "N";

    @Column(name = "supports_vision", length = 1, nullable = false)
    private String supportsVision = "N";

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "modified_date")
    private Timestamp modifiedDate;

    @PrePersist
    public void prePersist() {
        if (this.providerId == null) this.providerId = UUID.randomUUID().toString();
        if (this.createdDate == null) this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() { this.modifiedDate = new Timestamp(System.currentTimeMillis()); }

    public AiProvider() {}

    public String getProviderId() { return providerId; }
    public void setProviderId(String v) { this.providerId = v; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String v) { this.providerName = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String v) { this.providerType = v; }
    public String getBaseEndpoint() { return baseEndpoint; }
    public void setBaseEndpoint(String v) { this.baseEndpoint = v; }
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String v) { this.apiVersion = v; }
    public String getIsActive() { return isActive; }
    public void setIsActive(String v) { this.isActive = v; }
    public String getSupportsStreaming() { return supportsStreaming; }
    public void setSupportsStreaming(String v) { this.supportsStreaming = v; }
    public String getSupportsFunctions() { return supportsFunctions; }
    public void setSupportsFunctions(String v) { this.supportsFunctions = v; }
    public String getSupportsVision() { return supportsVision; }
    public void setSupportsVision(String v) { this.supportsVision = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public Timestamp getCreatedDate() { return createdDate; }
    public void setCreatedDate(Timestamp v) { this.createdDate = v; }
    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String v) { this.modifiedBy = v; }
    public Timestamp getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Timestamp v) { this.modifiedDate = v; }
}
