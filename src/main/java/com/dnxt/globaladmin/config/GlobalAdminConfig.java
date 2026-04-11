package com.dnxt.globaladmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GlobalAdminConfig {

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
