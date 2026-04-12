package com.dnxt.globaladmin.aigateway.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "aig_model",
       uniqueConstraints = @UniqueConstraint(name = "uk_aig_model_per_provider",
                                             columnNames = {"provider_id", "model_name"}))
public class AiModel {

    @Id @Column(name = "model_id", length = 40) private String modelId;
    @Column(name = "provider_id", length = 40, nullable = false) private String providerId;
    @Column(name = "model_name", length = 150, nullable = false) private String modelName;
    @Column(name = "model_display_name", length = 200) private String modelDisplayName;
    @Column(name = "model_type", length = 30, nullable = false) private String modelType = "CHAT";
    @Column(name = "deployment_name", length = 200) private String deploymentName;
    @Column(name = "max_input_tokens") private Long maxInputTokens = 128000L;
    @Column(name = "max_output_tokens") private Long maxOutputTokens = 4096L;
    @Column(name = "cost_per_input_1k", precision = 12, scale = 6) private BigDecimal costPerInput1k = BigDecimal.ZERO;
    @Column(name = "cost_per_output_1k", precision = 12, scale = 6) private BigDecimal costPerOutput1k = BigDecimal.ZERO;
    @Column(name = "is_active", length = 1, nullable = false) private String isActive = "Y";
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "created_date") private Timestamp createdDate;
    @Column(name = "modified_by", length = 100) private String modifiedBy;
    @Column(name = "modified_date") private Timestamp modifiedDate;

    @PrePersist public void prePersist() {
        if (this.modelId == null) this.modelId = UUID.randomUUID().toString();
        if (this.createdDate == null) this.createdDate = new Timestamp(System.currentTimeMillis());
    }
    @PreUpdate public void preUpdate() { this.modifiedDate = new Timestamp(System.currentTimeMillis()); }

    public AiModel() {}

    public String getModelId() { return modelId; }
    public void setModelId(String v) { this.modelId = v; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String v) { this.providerId = v; }
    public String getModelName() { return modelName; }
    public void setModelName(String v) { this.modelName = v; }
    public String getModelDisplayName() { return modelDisplayName; }
    public void setModelDisplayName(String v) { this.modelDisplayName = v; }
    public String getModelType() { return modelType; }
    public void setModelType(String v) { this.modelType = v; }
    public String getDeploymentName() { return deploymentName; }
    public void setDeploymentName(String v) { this.deploymentName = v; }
    public Long getMaxInputTokens() { return maxInputTokens; }
    public void setMaxInputTokens(Long v) { this.maxInputTokens = v; }
    public Long getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(Long v) { this.maxOutputTokens = v; }
    public BigDecimal getCostPerInput1k() { return costPerInput1k; }
    public void setCostPerInput1k(BigDecimal v) { this.costPerInput1k = v; }
    public BigDecimal getCostPerOutput1k() { return costPerOutput1k; }
    public void setCostPerOutput1k(BigDecimal v) { this.costPerOutput1k = v; }
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
