package com.dnxt.globaladmin.aigateway.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "aig_tenant_config")
public class AiTenantConfig {

    @Id @Column(name = "config_id", length = 40) private String configId;
    @Column(name = "ai_enabled", length = 1, nullable = false) private String aiEnabled = "N";
    @Column(name = "default_provider_id", length = 40) private String defaultProviderId;
    @Column(name = "default_model_id", length = 40) private String defaultModelId;
    @Column(name = "max_requests_per_minute") private Integer maxRequestsPerMinute;
    @Column(name = "max_cost_per_day", precision = 12, scale = 2) private BigDecimal maxCostPerDay;
    @Column(name = "max_cost_per_month", precision = 12, scale = 2) private BigDecimal maxCostPerMonth;
    @Column(name = "cost_alert_threshold") private Integer costAlertThreshold;
    @Column(name = "modified_by", length = 100) private String modifiedBy;
    @Column(name = "modified_date") private Timestamp modifiedDate;

    @PrePersist @PreUpdate public void touch() { this.modifiedDate = new Timestamp(System.currentTimeMillis()); }

    public AiTenantConfig() {}

    public String getConfigId() { return configId; }
    public void setConfigId(String v) { this.configId = v; }
    public String getAiEnabled() { return aiEnabled; }
    public void setAiEnabled(String v) { this.aiEnabled = v; }
    public String getDefaultProviderId() { return defaultProviderId; }
    public void setDefaultProviderId(String v) { this.defaultProviderId = v; }
    public String getDefaultModelId() { return defaultModelId; }
    public void setDefaultModelId(String v) { this.defaultModelId = v; }
    public Integer getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
    public void setMaxRequestsPerMinute(Integer v) { this.maxRequestsPerMinute = v; }
    public BigDecimal getMaxCostPerDay() { return maxCostPerDay; }
    public void setMaxCostPerDay(BigDecimal v) { this.maxCostPerDay = v; }
    public BigDecimal getMaxCostPerMonth() { return maxCostPerMonth; }
    public void setMaxCostPerMonth(BigDecimal v) { this.maxCostPerMonth = v; }
    public Integer getCostAlertThreshold() { return costAlertThreshold; }
    public void setCostAlertThreshold(Integer v) { this.costAlertThreshold = v; }
    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String v) { this.modifiedBy = v; }
    public Timestamp getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Timestamp v) { this.modifiedDate = v; }
}
