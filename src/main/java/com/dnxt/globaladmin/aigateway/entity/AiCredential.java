package com.dnxt.globaladmin.aigateway.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "aig_credential")
public class AiCredential {

    @Id @Column(name = "credential_id", length = 40) private String credentialId;
    @Column(name = "provider_id", length = 40, nullable = false, unique = true) private String providerId;
    @Column(name = "api_key_encrypted", length = 2000) private String apiKeyEncrypted;
    @Column(name = "endpoint_override", length = 500) private String endpointOverride;
    @Column(name = "resource_name", length = 200) private String resourceName;
    @Column(name = "is_active", length = 1, nullable = false) private String isActive = "Y";
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "created_date") private Timestamp createdDate;
    @Column(name = "modified_by", length = 100) private String modifiedBy;
    @Column(name = "modified_date") private Timestamp modifiedDate;

    @PrePersist public void prePersist() {
        if (this.credentialId == null) this.credentialId = UUID.randomUUID().toString();
        if (this.createdDate == null) this.createdDate = new Timestamp(System.currentTimeMillis());
    }
    @PreUpdate public void preUpdate() { this.modifiedDate = new Timestamp(System.currentTimeMillis()); }

    public AiCredential() {}

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String v) { this.credentialId = v; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String v) { this.providerId = v; }
    public String getApiKeyEncrypted() { return apiKeyEncrypted; }
    public void setApiKeyEncrypted(String v) { this.apiKeyEncrypted = v; }
    public String getEndpointOverride() { return endpointOverride; }
    public void setEndpointOverride(String v) { this.endpointOverride = v; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String v) { this.resourceName = v; }
    public String getIsActive() { return isActive; }
    public void setIsActive(String v) { this.isActive = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public Timestamp getCreatedDate() { return createdDate; }
    public void setCreatedDate(Timestamp v) { this.createdDate = v; }
    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String v) { this.modifiedBy = v; }
    public Timestamp getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Timestamp v) { this.modifiedDate = v; }
}
