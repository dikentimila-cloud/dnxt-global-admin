package com.dnxt.globaladmin.aigateway.service;

import com.dnxt.globaladmin.aigateway.entity.AiCredential;
import com.dnxt.globaladmin.aigateway.entity.AiModel;
import com.dnxt.globaladmin.aigateway.entity.AiProvider;

public class ResolvedAiConfig {
    public final AiProvider provider;
    public final AiModel model;
    public final String apiKey;
    public final String endpoint;
    public final String apiVersion;
    public final String deploymentName;
    public final String resourceName;

    public ResolvedAiConfig(AiProvider provider, AiModel model, String apiKey, AiCredential credential) {
        this.provider = provider;
        this.model = model;
        this.apiKey = apiKey;
        this.endpoint = (credential != null && credential.getEndpointOverride() != null
                && !credential.getEndpointOverride().isBlank())
                ? credential.getEndpointOverride() : provider.getBaseEndpoint();
        this.apiVersion = provider.getApiVersion();
        this.deploymentName = (model.getDeploymentName() != null && !model.getDeploymentName().isBlank())
                ? model.getDeploymentName() : model.getModelName();
        this.resourceName = credential != null ? credential.getResourceName() : null;
    }
}
