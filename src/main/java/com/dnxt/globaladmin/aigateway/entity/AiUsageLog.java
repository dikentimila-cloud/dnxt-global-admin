package com.dnxt.globaladmin.aigateway.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "aig_usage_log")
public class AiUsageLog {

    @Id @Column(name = "usage_id", length = 40) private String usageId;
    @Column(name = "tenant_id", length = 36) private String tenantId;
    @Column(name = "tenant_name", length = 200) private String tenantName;
    @Column(name = "provider_id", length = 40) private String providerId;
    @Column(name = "provider_name", length = 150) private String providerName;
    @Column(name = "model_id", length = 40) private String modelId;
    @Column(name = "model_name", length = 150) private String modelName;
    @Column(name = "input_tokens") private Integer inputTokens = 0;
    @Column(name = "output_tokens") private Integer outputTokens = 0;
    @Column(name = "total_tokens") private Integer totalTokens = 0;
    @Column(name = "cost_input", precision = 12, scale = 6) private BigDecimal costInput = BigDecimal.ZERO;
    @Column(name = "cost_output", precision = 12, scale = 6) private BigDecimal costOutput = BigDecimal.ZERO;
    @Column(name = "cost_total", precision = 12, scale = 6) private BigDecimal costTotal = BigDecimal.ZERO;
    @Column(name = "request_type", length = 50) private String requestType;
    @Column(name = "status", length = 20) private String status = "SUCCESS";
    @Column(name = "error_message", length = 1000) private String errorMessage;
    @Column(name = "duration_ms") private Integer durationMs;
    @Column(name = "requested_by", length = 100) private String requestedBy;
    @Column(name = "created_date") private Timestamp createdDate;

    @PrePersist public void prePersist() {
        if (usageId == null) usageId = UUID.randomUUID().toString();
        if (createdDate == null) createdDate = new Timestamp(System.currentTimeMillis());
    }

    public AiUsageLog() {}

    public String getUsageId() { return usageId; }
    public void setUsageId(String v) { this.usageId = v; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { this.tenantId = v; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String v) { this.tenantName = v; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String v) { this.providerId = v; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String v) { this.providerName = v; }
    public String getModelId() { return modelId; }
    public void setModelId(String v) { this.modelId = v; }
    public String getModelName() { return modelName; }
    public void setModelName(String v) { this.modelName = v; }
    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer v) { this.inputTokens = v; }
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer v) { this.outputTokens = v; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer v) { this.totalTokens = v; }
    public BigDecimal getCostInput() { return costInput; }
    public void setCostInput(BigDecimal v) { this.costInput = v; }
    public BigDecimal getCostOutput() { return costOutput; }
    public void setCostOutput(BigDecimal v) { this.costOutput = v; }
    public BigDecimal getCostTotal() { return costTotal; }
    public void setCostTotal(BigDecimal v) { this.costTotal = v; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String v) { this.requestType = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer v) { this.durationMs = v; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String v) { this.requestedBy = v; }
    public Timestamp getCreatedDate() { return createdDate; }
    public void setCreatedDate(Timestamp v) { this.createdDate = v; }
}
