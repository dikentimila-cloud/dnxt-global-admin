package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.entity.PlatformConfig;
import com.dnxt.globaladmin.repository.PlatformConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Two-layer email domain validation:
 * Layer 1: application.properties / env var (ALLOWED_EMAIL_DOMAINS)
 * Layer 2: platform_config table (security.allowed_email_domains + security.allowed_emails)
 *
 * An email is allowed if:
 * - Its domain is in either layer's allowed domains list, OR
 * - The exact email is in the individual allowlist (for contractors/exceptions)
 */
@Service
public class EmailDomainValidator {

    private static final Logger log = LoggerFactory.getLogger(EmailDomainValidator.class);

    @Value("${admin.security.allowed-email-domains:dnxtsolutions.com}")
    private String envAllowedDomains;

    @Autowired
    private PlatformConfigRepository configRepository;

    public boolean isEmailAllowed(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.indexOf("@") + 1).toLowerCase().trim();
        String normalizedEmail = email.toLowerCase().trim();

        // Layer 1: env var / application.properties
        Set<String> envDomains = parseCsv(envAllowedDomains);
        if (envDomains.contains(domain)) {
            return true;
        }

        // Layer 2: database config — allowed domains
        String dbDomains = getConfigValue("security.allowed_email_domains");
        if (dbDomains != null && parseCsv(dbDomains).contains(domain)) {
            return true;
        }

        // Layer 2: database config — individual email exceptions
        String dbEmails = getConfigValue("security.allowed_emails");
        if (dbEmails != null && parseCsv(dbEmails).contains(normalizedEmail)) {
            return true;
        }

        log.warn("Email domain rejected: {} (domain: {})", normalizedEmail, domain);
        return false;
    }

    public String getAllowedDomainsSummary() {
        Set<String> domains = parseCsv(envAllowedDomains);
        String dbDomains = getConfigValue("security.allowed_email_domains");
        if (dbDomains != null) {
            domains.addAll(parseCsv(dbDomains));
        }
        return String.join(", ", domains);
    }

    private Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(s -> s.trim().toLowerCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private String getConfigValue(String key) {
        return configRepository.findById(key)
                .map(PlatformConfig::getConfigValue)
                .orElse(null);
    }
}
