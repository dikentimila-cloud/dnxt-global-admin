package com.dnxt.globaladmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GlobalAdminConfig {

    /**
     * Run {@code flyway.repair()} before every migrate, so checksum mismatches
     * from edited-after-apply migrations self-heal on startup instead of
     * crashing the app. Safe because Flyway's repair only touches the
     * {@code flyway_schema_history} table — it never re-runs migrations.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> { flyway.repair(); flyway.migrate(); };
    }

    @Value("${admin.operations-service.url:http://localhost:8102}")
    private String operationsServiceUrl;

    @Value("${admin.mail-service.url:http://localhost:8089}")
    private String mailServiceUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getOperationsServiceUrl() {
        return operationsServiceUrl;
    }

    public String getMailServiceUrl() {
        return mailServiceUrl;
    }
}
