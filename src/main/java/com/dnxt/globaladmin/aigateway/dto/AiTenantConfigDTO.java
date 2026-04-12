package com.dnxt.globaladmin.aigateway.dto;

import com.dnxt.globaladmin.aigateway.entity.AiTenantConfig;
import java.math.BigDecimal;

public class AiTenantConfigDTO {
    public String configId;
    public String aiEnabled;
    public String defaultProviderId;
    public String defaultModelId;
    public Integer maxRequestsPerMinute;
    public BigDecimal maxCostPerDay;
    public BigDecimal maxCostPerMonth;
    public Integer costAlertThreshold;

    public static AiTenantConfigDTO fromEntity(AiTenantConfig c) {
        if (c == null) return null;
        AiTenantConfigDTO d = new AiTenantConfigDTO();
        d.configId = c.getConfigId(); d.aiEnabled = c.getAiEnabled();
        d.defaultProviderId = c.getDefaultProviderId(); d.defaultModelId = c.getDefaultModelId();
        d.maxRequestsPerMinute = c.getMaxRequestsPerMinute();
        d.maxCostPerDay = c.getMaxCostPerDay(); d.maxCostPerMonth = c.getMaxCostPerMonth();
        d.costAlertThreshold = c.getCostAlertThreshold();
        return d;
    }

    public void applyTo(AiTenantConfig c) {
        if (aiEnabled != null) c.setAiEnabled(aiEnabled);
        c.setDefaultProviderId(defaultProviderId);
        c.setDefaultModelId(defaultModelId);
        c.setMaxRequestsPerMinute(maxRequestsPerMinute);
        c.setMaxCostPerDay(maxCostPerDay);
        c.setMaxCostPerMonth(maxCostPerMonth);
        c.setCostAlertThreshold(costAlertThreshold);
    }
}
