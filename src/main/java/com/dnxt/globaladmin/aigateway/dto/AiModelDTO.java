package com.dnxt.globaladmin.aigateway.dto;

import com.dnxt.globaladmin.aigateway.entity.AiModel;
import java.math.BigDecimal;

public class AiModelDTO {
    public String modelId;
    public String providerId;
    public String modelName;
    public String modelDisplayName;
    public String modelType;
    public String deploymentName;
    public Long maxInputTokens;
    public Long maxOutputTokens;
    public BigDecimal costPerInput1k;
    public BigDecimal costPerOutput1k;
    public String isActive;

    public static AiModelDTO fromEntity(AiModel m) {
        if (m == null) return null;
        AiModelDTO d = new AiModelDTO();
        d.modelId = m.getModelId(); d.providerId = m.getProviderId();
        d.modelName = m.getModelName(); d.modelDisplayName = m.getModelDisplayName();
        d.modelType = m.getModelType(); d.deploymentName = m.getDeploymentName();
        d.maxInputTokens = m.getMaxInputTokens(); d.maxOutputTokens = m.getMaxOutputTokens();
        d.costPerInput1k = m.getCostPerInput1k(); d.costPerOutput1k = m.getCostPerOutput1k();
        d.isActive = m.getIsActive();
        return d;
    }

    public void applyTo(AiModel m) {
        if (providerId != null) m.setProviderId(providerId);
        if (modelName != null) m.setModelName(modelName);
        if (modelDisplayName != null) m.setModelDisplayName(modelDisplayName);
        if (modelType != null) m.setModelType(modelType);
        m.setDeploymentName(deploymentName);
        if (maxInputTokens != null) m.setMaxInputTokens(maxInputTokens);
        if (maxOutputTokens != null) m.setMaxOutputTokens(maxOutputTokens);
        if (costPerInput1k != null) m.setCostPerInput1k(costPerInput1k);
        if (costPerOutput1k != null) m.setCostPerOutput1k(costPerOutput1k);
        if (isActive != null) m.setIsActive(isActive);
    }
}
