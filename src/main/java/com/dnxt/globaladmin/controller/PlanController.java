package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.repository.ModulePlanRepository;
import com.dnxt.globaladmin.security.PermissionCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    @Autowired
    private ModulePlanRepository planRepository;

    @GetMapping
    @PermissionCheck("TENANT_VIEW")
    public ResponseEntity<ApiResponse> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.ok(planRepository.findAll()));
    }

    @GetMapping("/{module}")
    @PermissionCheck("TENANT_VIEW")
    public ResponseEntity<ApiResponse> getPlansByModule(@PathVariable String module) {
        return ResponseEntity.ok(ApiResponse.ok(
                planRepository.findByModuleNameAndIsActiveTrueOrderBySortOrderAsc(module)));
    }
}
