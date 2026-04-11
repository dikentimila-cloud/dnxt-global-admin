package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.entity.AuditLog;
import com.dnxt.globaladmin.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String userId, String username, String action,
                    String targetType, String targetId, String targetName,
                    String details, HttpServletRequest request) {
        AuditLog entry = new AuditLog();
        entry.setLogId(UUID.randomUUID().toString());
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setTargetName(targetName);
        entry.setDetails(details);
        entry.setIpAddress(getClientIp(request));
        entry.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        entry.setCreatedDate(new Timestamp(System.currentTimeMillis()));

        auditLogRepository.save(entry);
        log.info("AUDIT: {} by {} on {}/{} ({})", action, username, targetType, targetId, details);
    }

    public void log(String action, String details, HttpServletRequest request) {
        log(null, null, action, null, null, null, details, request);
    }

    public Page<AuditLog> getLogs(int page, int size) {
        return auditLogRepository.findByOrderByCreatedDateDesc(PageRequest.of(page, size));
    }

    public Page<AuditLog> getLogsByAction(String action, int page, int size) {
        return auditLogRepository.findByActionOrderByCreatedDateDesc(action, PageRequest.of(page, size));
    }

    public Page<AuditLog> getLogsByUser(String userId, int page, int size) {
        return auditLogRepository.findByUserIdOrderByCreatedDateDesc(userId, PageRequest.of(page, size));
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "system";
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
