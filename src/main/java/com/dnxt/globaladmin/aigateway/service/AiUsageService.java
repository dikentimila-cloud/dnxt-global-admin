package com.dnxt.globaladmin.aigateway.service;

import com.dnxt.globaladmin.aigateway.entity.AiUsageLog;
import com.dnxt.globaladmin.aigateway.repository.AiUsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AiUsageService {

    private static final Logger log = LoggerFactory.getLogger(AiUsageService.class);

    @Autowired
    private AiUsageLogRepository usageRepo;

    /**
     * Log an AI usage event. Called after every AI call completes.
     * Estimates token counts from character lengths when actual counts aren't available.
     */
    @Async
    public void logUsage(ResolvedAiConfig cfg, String prompt, String response,
                         String tenantId, String tenantName,
                         String requestType, String requestedBy,
                         long durationMs, boolean success, String errorMsg) {
        try {
            // Estimate tokens (~4 chars per token for English)
            int inputTokens = prompt != null ? prompt.length() / 4 : 0;
            int outputTokens = response != null ? response.length() / 4 : 0;
            int totalTokens = inputTokens + outputTokens;

            // Calculate cost from model pricing
            BigDecimal costIn = BigDecimal.ZERO;
            BigDecimal costOut = BigDecimal.ZERO;
            if (cfg.model.getCostPerInput1k() != null) {
                costIn = cfg.model.getCostPerInput1k()
                        .multiply(BigDecimal.valueOf(inputTokens))
                        .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
            }
            if (cfg.model.getCostPerOutput1k() != null) {
                costOut = cfg.model.getCostPerOutput1k()
                        .multiply(BigDecimal.valueOf(outputTokens))
                        .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
            }

            AiUsageLog entry = new AiUsageLog();
            entry.setTenantId(tenantId);
            entry.setTenantName(tenantName);
            entry.setProviderId(cfg.provider.getProviderId());
            entry.setProviderName(cfg.provider.getDisplayName());
            entry.setModelId(cfg.model.getModelId());
            entry.setModelName(cfg.model.getModelDisplayName() != null
                    ? cfg.model.getModelDisplayName() : cfg.model.getModelName());
            entry.setInputTokens(inputTokens);
            entry.setOutputTokens(outputTokens);
            entry.setTotalTokens(totalTokens);
            entry.setCostInput(costIn);
            entry.setCostOutput(costOut);
            entry.setCostTotal(costIn.add(costOut));
            entry.setRequestType(requestType);
            entry.setStatus(success ? "SUCCESS" : "ERROR");
            entry.setErrorMessage(errorMsg);
            entry.setDurationMs((int) durationMs);
            entry.setRequestedBy(requestedBy);

            usageRepo.save(entry);
        } catch (Exception e) {
            log.warn("Failed to log AI usage: {}", e.getMessage());
        }
    }

    /** Platform-wide usage summary for a time period. */
    public Map<String, Object> getSummary(int days) {
        Timestamp since = Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS));
        Object[] totals = usageRepo.totalsSince(since);
        List<Object[]> byTenant = usageRepo.summarizeByTenantSince(since);
        List<Object[]> byModel = usageRepo.summarizeByModelSince(since);

        Map<String, Object> result = new HashMap<>();
        result.put("period", days + " days");
        result.put("totalRequests", totals[0]);
        result.put("totalTokens", totals[1]);
        result.put("totalCost", totals[2]);

        List<Map<String, Object>> tenantBreakdown = new ArrayList<>();
        for (Object[] row : byTenant) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("tenantId", row[0]);
            t.put("tenantName", row[1]);
            t.put("requests", row[2]);
            t.put("tokens", row[3]);
            t.put("cost", row[4]);
            tenantBreakdown.add(t);
        }
        result.put("byTenant", tenantBreakdown);

        List<Map<String, Object>> modelBreakdown = new ArrayList<>();
        for (Object[] row : byModel) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("provider", row[0]);
            m.put("model", row[1]);
            m.put("requests", row[2]);
            m.put("tokens", row[3]);
            m.put("cost", row[4]);
            modelBreakdown.add(m);
        }
        result.put("byModel", modelBreakdown);

        return result;
    }

    /** Recent usage log entries. */
    public Page<AiUsageLog> getRecentUsage(int page, int size) {
        return usageRepo.findByOrderByCreatedDateDesc(PageRequest.of(page, size));
    }

    /** Usage for a specific tenant. */
    public Map<String, Object> getTenantUsage(String tenantId, int days) {
        Timestamp since = Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS));
        Object[] totals = usageRepo.totalsByTenantSince(tenantId, since);
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("period", days + " days");
        result.put("totalRequests", totals[0]);
        result.put("totalTokens", totals[1]);
        result.put("totalCost", totals[2]);
        return result;
    }
}
