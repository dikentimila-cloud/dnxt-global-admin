package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.security.PermissionCheck;
import com.dnxt.globaladmin.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PermissionCheck("AUDIT_VIEW")
    public ResponseEntity<ApiResponse> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getLogs(page, size)));
    }

    @GetMapping("/action/{action}")
    @PermissionCheck("AUDIT_VIEW")
    public ResponseEntity<ApiResponse> getLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getLogsByAction(action, page, size)));
    }

    @GetMapping("/user/{userId}")
    @PermissionCheck("AUDIT_VIEW")
    public ResponseEntity<ApiResponse> getLogsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getLogsByUser(userId, page, size)));
    }
}
