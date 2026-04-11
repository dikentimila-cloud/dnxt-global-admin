package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.security.PermissionCheck;
import com.dnxt.globaladmin.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/overview")
    @PermissionCheck("DASHBOARD_VIEW")
    public ResponseEntity<ApiResponse> getOverview() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getOverview()));
    }

    @GetMapping("/health")
    @PermissionCheck("DASHBOARD_VIEW")
    public ResponseEntity<ApiResponse> getPlatformHealth() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getPlatformHealth()));
    }
}
