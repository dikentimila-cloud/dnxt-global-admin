package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.entity.PlatformConfig;
import com.dnxt.globaladmin.repository.PlatformConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    @Autowired
    private PlatformConfigRepository configRepository;

    @Transactional(readOnly = true)
    public List<PlatformConfig> getAllConfig() {
        List<PlatformConfig> configs = configRepository.findAll();
        // Mask secret values
        configs.forEach(c -> {
            if (Boolean.TRUE.equals(c.getIsSecret())) {
                c.setConfigValue("********");
            }
        });
        return configs;
    }

    @Transactional(readOnly = true)
    public List<PlatformConfig> getConfigByCategory(String category) {
        List<PlatformConfig> configs = configRepository.findByCategoryOrderByConfigKeyAsc(category);
        configs.forEach(c -> {
            if (Boolean.TRUE.equals(c.getIsSecret())) {
                c.setConfigValue("********");
            }
        });
        return configs;
    }

    @Transactional
    public void updateConfig(Map<String, String> updates, String modifiedBy) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            PlatformConfig config = configRepository.findById(entry.getKey()).orElse(null);
            if (config != null) {
                config.setConfigValue(entry.getValue());
                config.setModifiedBy(modifiedBy);
                config.setModifiedDate(new Timestamp(System.currentTimeMillis()));
                configRepository.save(config);
                log.info("Config updated: {} by {}", entry.getKey(), modifiedBy);
            }
        }
    }

    @Transactional(readOnly = true)
    public String getConfigValue(String key) {
        return configRepository.findById(key)
                .map(PlatformConfig::getConfigValue)
                .orElse(null);
    }
}
