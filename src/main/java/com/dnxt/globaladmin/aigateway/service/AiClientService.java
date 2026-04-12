package com.dnxt.globaladmin.aigateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiClientService {

    private static final Logger log = LoggerFactory.getLogger(AiClientService.class);

    private final AiConfigService configService;
    private final RestTemplate restTemplate;
    private final AiUsageService usageService;

    @Autowired
    public AiClientService(AiConfigService configService, RestTemplate restTemplate, AiUsageService usageService) {
        this.configService = configService;
        this.restTemplate = restTemplate;
        this.usageService = usageService;
    }

    public static class Options {
        public Integer maxTokens;
        public Double temperature;
        public String systemPrompt;
        public String modelOverride;

        public static Options defaults() { return new Options(); }
        public Options maxTokens(int n) { this.maxTokens = n; return this; }
        public Options temperature(double t) { this.temperature = t; return this; }
        public Options system(String s) { this.systemPrompt = s; return this; }
        public Options model(String name) { this.modelOverride = name; return this; }
    }

    public String chatCompletion(String prompt, Options options) {
        ResolvedAiConfig cfg = configService.resolveActiveConfig();
        if (cfg == null) throw new IllegalStateException("AI is not configured. Open AI Configuration to add a provider and enable AI.");
        return chatCompletionWith(cfg, prompt, options);
    }

    public String chatCompletion(String prompt) { return chatCompletion(prompt, Options.defaults()); }

    public String chatCompletionWith(ResolvedAiConfig cfg, String prompt, Options options) {
        return chatCompletionWith(cfg, prompt, options, null, null, null, null);
    }

    /**
     * Full completion call with usage tracking.
     */
    public String chatCompletionWith(ResolvedAiConfig cfg, String prompt, Options options,
                                     String tenantId, String tenantName,
                                     String requestType, String requestedBy) {
        if (options == null) options = Options.defaults();
        String type = cfg.provider.getProviderType().toUpperCase(Locale.ROOT);
        long start = System.currentTimeMillis();
        try {
            String result = switch (type) {
                case "ANTHROPIC" -> callAnthropic(cfg, prompt, options);
                case "AZURE_OPENAI" -> callAzureOpenAI(cfg, prompt, options);
                case "OPENAI" -> callOpenAI(cfg, prompt, options);
                case "OLLAMA" -> callOllama(cfg, prompt, options);
                default -> throw new IllegalStateException("Unsupported provider type: " + type);
            };
            long duration = System.currentTimeMillis() - start;
            usageService.logUsage(cfg, prompt, result, tenantId, tenantName, requestType, requestedBy, duration, true, null);
            return result;
        } catch (HttpStatusCodeException e) {
            long duration = System.currentTimeMillis() - start;
            String errMsg = e.getStatusCode() + ": " + e.getResponseBodyAsString();
            usageService.logUsage(cfg, prompt, null, tenantId, tenantName, requestType, requestedBy, duration, false, errMsg);
            log.error("LLM call failed [{}]: {}", type, errMsg);
            throw new IllegalStateException(cfg.provider.getDisplayName() + " " + errMsg, e);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            usageService.logUsage(cfg, prompt, null, tenantId, tenantName, requestType, requestedBy, duration, false, e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private String callAnthropic(ResolvedAiConfig cfg, String prompt, Options o) {
        String url = trimSlash(cfg.endpoint != null ? cfg.endpoint : "https://api.anthropic.com") + "/v1/messages";
        Map<String, Object> body = new HashMap<>();
        body.put("model", o.modelOverride != null ? o.modelOverride : cfg.model.getModelName());
        body.put("max_tokens", o.maxTokens != null ? o.maxTokens : Math.min(4096, cfg.model.getMaxOutputTokens().intValue()));
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (o.systemPrompt != null && !o.systemPrompt.isBlank()) body.put("system", o.systemPrompt);
        if (o.temperature != null) body.put("temperature", o.temperature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", cfg.apiKey);
        headers.set("anthropic-version", cfg.apiVersion != null ? cfg.apiVersion : "2023-06-01");

        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> respBody = resp.getBody();
        if (respBody == null) throw new IllegalStateException("Empty Anthropic response");
        List<Map<String, Object>> content = (List<Map<String, Object>>) respBody.get("content");
        if (content == null || content.isEmpty()) return "";
        Object text = content.get(0).get("text");
        return text != null ? text.toString().trim() : "";
    }

    @SuppressWarnings("unchecked")
    private String callAzureOpenAI(ResolvedAiConfig cfg, String prompt, Options o) {
        if (cfg.endpoint == null || cfg.endpoint.isBlank()) throw new IllegalStateException("Azure OpenAI endpoint not configured");
        String apiVer = cfg.apiVersion != null ? cfg.apiVersion : "2024-02-15-preview";
        String url = trimSlash(cfg.endpoint) + "/openai/deployments/" + cfg.deploymentName + "/chat/completions?api-version=" + apiVer;

        List<Map<String, Object>> messages = new ArrayList<>();
        if (o.systemPrompt != null && !o.systemPrompt.isBlank()) messages.add(Map.of("role", "system", "content", o.systemPrompt));
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> body = new HashMap<>();
        body.put("messages", messages);
        body.put("max_tokens", o.maxTokens != null ? o.maxTokens : 1000);
        body.put("temperature", o.temperature != null ? o.temperature : 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", cfg.apiKey);

        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        return extractOpenAiContent(resp.getBody());
    }

    @SuppressWarnings("unchecked")
    private String callOpenAI(ResolvedAiConfig cfg, String prompt, Options o) {
        String url = trimSlash(cfg.endpoint != null ? cfg.endpoint : "https://api.openai.com") + "/v1/chat/completions";
        List<Map<String, Object>> messages = new ArrayList<>();
        if (o.systemPrompt != null && !o.systemPrompt.isBlank()) messages.add(Map.of("role", "system", "content", o.systemPrompt));
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", o.modelOverride != null ? o.modelOverride : cfg.model.getModelName());
        body.put("messages", messages);
        body.put("max_tokens", o.maxTokens != null ? o.maxTokens : 1000);
        body.put("temperature", o.temperature != null ? o.temperature : 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.apiKey);

        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        return extractOpenAiContent(resp.getBody());
    }

    @SuppressWarnings("unchecked")
    private String callOllama(ResolvedAiConfig cfg, String prompt, Options o) {
        String url = trimSlash(cfg.endpoint != null ? cfg.endpoint : "http://localhost:11434") + "/api/chat";
        List<Map<String, Object>> messages = new ArrayList<>();
        if (o.systemPrompt != null && !o.systemPrompt.isBlank()) messages.add(Map.of("role", "system", "content", o.systemPrompt));
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> opts = new HashMap<>();
        opts.put("temperature", o.temperature != null ? o.temperature : 0.7);
        opts.put("num_predict", o.maxTokens != null ? o.maxTokens : 1000);

        Map<String, Object> body = new HashMap<>();
        body.put("model", o.modelOverride != null ? o.modelOverride : cfg.model.getModelName());
        body.put("messages", messages); body.put("stream", false); body.put("options", opts);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> respBody = resp.getBody();
        if (respBody == null) return "";
        Map<String, Object> message = (Map<String, Object>) respBody.get("message");
        return message != null && message.get("content") != null ? message.get("content").toString().trim() : "";
    }

    @SuppressWarnings("unchecked")
    private String extractOpenAiContent(Map<String, Object> respBody) {
        if (respBody == null) throw new IllegalStateException("Empty OpenAI response");
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
        if (choices == null || choices.isEmpty()) return "";
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message != null && message.get("content") != null ? message.get("content").toString().trim() : "";
    }

    private String trimSlash(String s) { return s != null && s.endsWith("/") ? s.substring(0, s.length() - 1) : s; }
}
