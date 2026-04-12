package com.dnxt.globaladmin.aigateway.dto;

import com.dnxt.globaladmin.aigateway.entity.AiCredential;

public class AiCredentialDTO {
    public static final String ENCRYPTED_MARKER = "***ENCRYPTED***";

    public String credentialId;
    public String providerId;
    public String apiKey;
    public String endpointOverride;
    public String resourceName;
    public String isActive;

    public static AiCredentialDTO fromEntity(AiCredential c) {
        if (c == null) return null;
        AiCredentialDTO d = new AiCredentialDTO();
        d.credentialId = c.getCredentialId();
        d.providerId = c.getProviderId();
        d.apiKey = c.getApiKeyEncrypted() != null ? ENCRYPTED_MARKER : null;
        d.endpointOverride = c.getEndpointOverride();
        d.resourceName = c.getResourceName();
        d.isActive = c.getIsActive();
        return d;
    }
}
