package com.dnxt.globaladmin.aigateway.dto;

import com.dnxt.globaladmin.aigateway.entity.AiProvider;

public class AiProviderDTO {
    public String providerId;
    public String providerName;
    public String displayName;
    public String providerType;
    public String baseEndpoint;
    public String apiVersion;
    public String isActive;
    public String supportsStreaming;
    public String supportsFunctions;
    public String supportsVision;

    public static AiProviderDTO fromEntity(AiProvider p) {
        if (p == null) return null;
        AiProviderDTO d = new AiProviderDTO();
        d.providerId = p.getProviderId(); d.providerName = p.getProviderName();
        d.displayName = p.getDisplayName(); d.providerType = p.getProviderType();
        d.baseEndpoint = p.getBaseEndpoint(); d.apiVersion = p.getApiVersion();
        d.isActive = p.getIsActive(); d.supportsStreaming = p.getSupportsStreaming();
        d.supportsFunctions = p.getSupportsFunctions(); d.supportsVision = p.getSupportsVision();
        return d;
    }

    public void applyTo(AiProvider p) {
        if (providerName != null) p.setProviderName(providerName);
        if (displayName != null) p.setDisplayName(displayName);
        if (providerType != null) p.setProviderType(providerType);
        p.setBaseEndpoint(baseEndpoint);
        p.setApiVersion(apiVersion);
        if (isActive != null) p.setIsActive(isActive);
        if (supportsStreaming != null) p.setSupportsStreaming(supportsStreaming);
        if (supportsFunctions != null) p.setSupportsFunctions(supportsFunctions);
        if (supportsVision != null) p.setSupportsVision(supportsVision);
    }
}
