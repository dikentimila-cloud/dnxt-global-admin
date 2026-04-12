package com.dnxt.globaladmin.aigateway.service;

import com.dnxt.globaladmin.aigateway.dto.*;
import com.dnxt.globaladmin.aigateway.entity.*;
import com.dnxt.globaladmin.aigateway.repository.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AiConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiConfigService.class);
    private static final String SINGLETON_ID = "default";

    private final AiProviderRepository providerRepo;
    private final AiModelRepository modelRepo;
    private final AiCredentialRepository credentialRepo;
    private final AiTenantConfigRepository tenantRepo;
    private final AiConfigEncryption encryption;

    @Autowired
    public AiConfigService(AiProviderRepository providerRepo, AiModelRepository modelRepo,
                           AiCredentialRepository credentialRepo, AiTenantConfigRepository tenantRepo,
                           AiConfigEncryption encryption) {
        this.providerRepo = providerRepo;
        this.modelRepo = modelRepo;
        this.credentialRepo = credentialRepo;
        this.tenantRepo = tenantRepo;
        this.encryption = encryption;
    }

    @PostConstruct
    public void ensureConfigRow() {
        try {
            if (!tenantRepo.existsById(SINGLETON_ID)) {
                AiTenantConfig c = new AiTenantConfig();
                c.setConfigId(SINGLETON_ID);
                c.setAiEnabled("N");
                tenantRepo.save(c);
            }
        } catch (Exception e) {
            log.warn("Could not seed AI tenant config row: {}", e.getMessage());
        }
    }

    // ── Providers ────────────────────────────────────────────────────────────

    public List<AiProviderDTO> listProviders() {
        return providerRepo.findAllByOrderByDisplayNameAsc().stream().map(AiProviderDTO::fromEntity).toList();
    }

    @Transactional
    public AiProviderDTO saveProvider(AiProviderDTO input, String actor) {
        if (input.providerType == null || input.providerType.isBlank()) throw new IllegalArgumentException("providerType is required");
        if (input.providerName == null || input.providerName.isBlank()) throw new IllegalArgumentException("providerName is required");
        AiProvider entity;
        if (input.providerId != null && !input.providerId.isBlank()) {
            entity = providerRepo.findById(input.providerId).orElseThrow(() -> new IllegalArgumentException("Provider not found"));
            input.applyTo(entity);
            entity.setModifiedBy(actor);
        } else {
            entity = new AiProvider();
            input.applyTo(entity);
            entity.setCreatedBy(actor);
        }
        return AiProviderDTO.fromEntity(providerRepo.save(entity));
    }

    @Transactional
    public void deleteProvider(String providerId) {
        tenantRepo.findById(SINGLETON_ID).ifPresent(c -> {
            if (providerId.equals(c.getDefaultProviderId())) {
                c.setDefaultProviderId(null); c.setDefaultModelId(null); c.setAiEnabled("N");
                tenantRepo.save(c);
            }
        });
        providerRepo.deleteById(providerId);
    }

    // ── Models ───────────────────────────────────────────────────────────────

    public List<AiModelDTO> listModels(String providerId) {
        List<AiModel> rows = (providerId != null && !providerId.isBlank())
                ? modelRepo.findByProviderIdOrderByModelDisplayNameAsc(providerId)
                : modelRepo.findAllByOrderByModelDisplayNameAsc();
        return rows.stream().map(AiModelDTO::fromEntity).toList();
    }

    @Transactional
    public AiModelDTO saveModel(AiModelDTO input, String actor) {
        if (input.providerId == null || input.providerId.isBlank()) throw new IllegalArgumentException("providerId is required");
        if (input.modelName == null || input.modelName.isBlank()) throw new IllegalArgumentException("modelName is required");
        if (!providerRepo.existsById(input.providerId)) throw new IllegalArgumentException("Provider not found");
        AiModel entity;
        if (input.modelId != null && !input.modelId.isBlank()) {
            entity = modelRepo.findById(input.modelId).orElseThrow(() -> new IllegalArgumentException("Model not found"));
            input.applyTo(entity);
            entity.setModifiedBy(actor);
        } else {
            entity = new AiModel();
            input.applyTo(entity);
            entity.setCreatedBy(actor);
        }
        return AiModelDTO.fromEntity(modelRepo.save(entity));
    }

    @Transactional
    public void deleteModel(String modelId) {
        tenantRepo.findById(SINGLETON_ID).ifPresent(c -> {
            if (modelId.equals(c.getDefaultModelId())) { c.setDefaultModelId(null); tenantRepo.save(c); }
        });
        modelRepo.deleteById(modelId);
    }

    // ── Credentials ──────────────────────────────────────────────────────────

    public List<AiCredentialDTO> listCredentials() {
        return credentialRepo.findAll().stream().map(AiCredentialDTO::fromEntity).toList();
    }

    @Transactional
    public AiCredentialDTO saveCredential(AiCredentialDTO input, String actor) {
        if (input.providerId == null || input.providerId.isBlank()) throw new IllegalArgumentException("providerId is required");
        AiCredential entity = credentialRepo.findByProviderId(input.providerId).orElseGet(() -> {
            AiCredential c = new AiCredential(); c.setProviderId(input.providerId); c.setCreatedBy(actor); return c;
        });
        if (input.apiKey != null && !AiCredentialDTO.ENCRYPTED_MARKER.equals(input.apiKey) && !input.apiKey.isBlank()) {
            entity.setApiKeyEncrypted(encryption.encrypt(input.apiKey));
        }
        entity.setEndpointOverride(input.endpointOverride);
        entity.setResourceName(input.resourceName);
        entity.setIsActive("Y");
        entity.setModifiedBy(actor);
        return AiCredentialDTO.fromEntity(credentialRepo.save(entity));
    }

    @Transactional
    public void deleteCredential(String providerId) { credentialRepo.deleteByProviderId(providerId); }

    // ── Tenant config ────────────────────────────────────────────────────────

    public AiTenantConfigDTO getTenantConfig() {
        return tenantRepo.findById(SINGLETON_ID).map(AiTenantConfigDTO::fromEntity).orElseGet(() -> {
            ensureConfigRow();
            return tenantRepo.findById(SINGLETON_ID).map(AiTenantConfigDTO::fromEntity).orElse(null);
        });
    }

    @Transactional
    public AiTenantConfigDTO saveTenantConfig(AiTenantConfigDTO input, String actor) {
        AiTenantConfig entity = tenantRepo.findById(SINGLETON_ID).orElseGet(() -> {
            AiTenantConfig c = new AiTenantConfig(); c.setConfigId(SINGLETON_ID); return c;
        });
        input.applyTo(entity);
        entity.setModifiedBy(actor);
        return AiTenantConfigDTO.fromEntity(tenantRepo.save(entity));
    }

    // ── Active config resolver ───────────────────────────────────────────────

    public ResolvedAiConfig resolveActiveConfig() {
        AiTenantConfig tenant = tenantRepo.findById(SINGLETON_ID).orElse(null);
        if (tenant == null || !"Y".equalsIgnoreCase(tenant.getAiEnabled())) return null;
        if (tenant.getDefaultProviderId() == null) return null;

        AiProvider provider = providerRepo.findById(tenant.getDefaultProviderId()).orElse(null);
        if (provider == null || !"Y".equalsIgnoreCase(provider.getIsActive())) return null;

        AiModel model = null;
        if (tenant.getDefaultModelId() != null) model = modelRepo.findById(tenant.getDefaultModelId()).orElse(null);
        if (model == null) {
            model = modelRepo.findByProviderIdOrderByModelDisplayNameAsc(provider.getProviderId()).stream()
                    .filter(m -> "Y".equalsIgnoreCase(m.getIsActive()) && "CHAT".equalsIgnoreCase(m.getModelType()))
                    .findFirst().orElse(null);
        }
        if (model == null) return null;

        AiCredential cred = credentialRepo.findByProviderId(provider.getProviderId()).orElse(null);
        String apiKey = cred != null ? encryption.decrypt(cred.getApiKeyEncrypted()) : null;
        if (apiKey == null && !"OLLAMA".equalsIgnoreCase(provider.getProviderType())) return null;

        return new ResolvedAiConfig(provider, model, apiKey, cred);
    }

    public ResolvedAiConfig resolveProviderConfig(String providerId) {
        AiProvider provider = providerRepo.findById(providerId).orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        AiModel model = modelRepo.findByProviderIdOrderByModelDisplayNameAsc(providerId).stream()
                .filter(m -> "Y".equalsIgnoreCase(m.getIsActive()) && "CHAT".equalsIgnoreCase(m.getModelType()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No active CHAT model for this provider"));
        AiCredential cred = credentialRepo.findByProviderId(providerId).orElse(null);
        String apiKey = cred != null ? encryption.decrypt(cred.getApiKeyEncrypted()) : null;
        if (apiKey == null && !"OLLAMA".equalsIgnoreCase(provider.getProviderType()))
            throw new IllegalArgumentException("API key not configured for this provider");
        return new ResolvedAiConfig(provider, model, apiKey, cred);
    }

    // ── Quick setup ──────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> quickSetup(String actor) {
        List<AiProviderDTO> createdProviders = new ArrayList<>();
        List<AiModelDTO> createdModels = new ArrayList<>();

        record M(String name, String display, String type, String deploy, long maxIn, long maxOut) {}
        record Seed(String name, String display, String type, String endpoint, String apiVer,
                    String stream, String fn, String vision, List<M> models) {}

        List<Seed> seeds = List.of(
            new Seed("anthropic-claude", "Anthropic Claude", "ANTHROPIC",
                "https://api.anthropic.com", "2023-06-01", "Y", "Y", "Y", List.of(
                    new M("claude-sonnet-4-6", "Claude Sonnet 4.6", "CHAT", null, 200000, 8192),
                    new M("claude-opus-4-6", "Claude Opus 4.6", "CHAT", null, 200000, 8192),
                    new M("claude-haiku-4-5-20251001", "Claude Haiku 4.5", "CHAT", null, 200000, 8192))),
            new Seed("azure-openai", "Azure OpenAI", "AZURE_OPENAI",
                "https://your-resource.openai.azure.com", "2024-02-15-preview", "Y", "Y", "N", List.of(
                    new M("gpt-35-turbo", "GPT-3.5 Turbo", "CHAT", "gpt-35-turbo", 16384, 4096),
                    new M("gpt-4o", "GPT-4o", "CHAT", "gpt-4o", 128000, 4096))),
            new Seed("ollama-local", "Ollama (Local LLM)", "OLLAMA",
                "http://localhost:11434", null, "Y", "N", "N", List.of(
                    new M("llama3", "Llama 3", "CHAT", null, 8192, 4096),
                    new M("mistral:7b", "Mistral 7B", "CHAT", null, 32768, 4096)))
        );

        AiProvider anthropic = null;
        AiModel anthropicChat = null;

        for (Seed sp : seeds) {
            AiProvider provider = providerRepo.findByProviderName(sp.name()).orElse(null);
            if (provider == null) {
                provider = new AiProvider();
                provider.setProviderName(sp.name()); provider.setDisplayName(sp.display());
                provider.setProviderType(sp.type()); provider.setBaseEndpoint(sp.endpoint());
                provider.setApiVersion(sp.apiVer()); provider.setIsActive("Y");
                provider.setSupportsStreaming(sp.stream()); provider.setSupportsFunctions(sp.fn());
                provider.setSupportsVision(sp.vision()); provider.setCreatedBy(actor);
                provider = providerRepo.save(provider);
                createdProviders.add(AiProviderDTO.fromEntity(provider));
            }
            if ("anthropic-claude".equals(sp.name())) anthropic = provider;

            for (M sm : sp.models()) {
                AiModel model = modelRepo.findByProviderIdAndModelName(provider.getProviderId(), sm.name()).orElse(null);
                if (model == null) {
                    model = new AiModel();
                    model.setProviderId(provider.getProviderId()); model.setModelName(sm.name());
                    model.setModelDisplayName(sm.display()); model.setModelType(sm.type());
                    model.setDeploymentName(sm.deploy()); model.setMaxInputTokens(sm.maxIn());
                    model.setMaxOutputTokens(sm.maxOut()); model.setCostPerInput1k(BigDecimal.ZERO);
                    model.setCostPerOutput1k(BigDecimal.ZERO); model.setIsActive("Y");
                    model.setCreatedBy(actor);
                    model = modelRepo.save(model);
                    createdModels.add(AiModelDTO.fromEntity(model));
                }
                if (anthropic != null && provider.getProviderId().equals(anthropic.getProviderId())
                        && "claude-sonnet-4-6".equals(sm.name())) {
                    anthropicChat = model;
                }
            }
        }

        AiTenantConfig tenant = tenantRepo.findById(SINGLETON_ID).orElseGet(() -> {
            AiTenantConfig c = new AiTenantConfig(); c.setConfigId(SINGLETON_ID); return c;
        });
        if (tenant.getDefaultProviderId() == null && anthropic != null) {
            tenant.setAiEnabled("Y");
            tenant.setDefaultProviderId(anthropic.getProviderId());
            if (anthropicChat != null) tenant.setDefaultModelId(anthropicChat.getModelId());
            tenant.setModifiedBy(actor);
            tenantRepo.save(tenant);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("createdProviders", createdProviders);
        result.put("createdModels", createdModels);
        result.put("tenantConfig", AiTenantConfigDTO.fromEntity(tenantRepo.findById(SINGLETON_ID).orElse(tenant)));
        return result;
    }
}
