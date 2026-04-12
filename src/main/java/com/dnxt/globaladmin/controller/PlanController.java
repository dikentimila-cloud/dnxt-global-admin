package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.entity.ModulePlan;
import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.repository.ModulePlanRepository;
import com.dnxt.globaladmin.security.PermissionCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

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

    @PostMapping
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> createPlan(@RequestBody Map<String, Object> body) {
        try {
            ModulePlan plan = new ModulePlan();
            plan.setPlanId(UUID.randomUUID().toString());
            plan.setModuleName((String) body.get("moduleName"));
            plan.setPlanName((String) body.get("planName"));
            plan.setPlanLabel((String) body.get("planLabel"));
            plan.setDescription((String) body.get("description"));
            plan.setFeatures((String) body.get("features"));
            plan.setSortOrder(body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : 0);
            plan.setIsActive(true);
            plan.setCreatedDate(new Timestamp(System.currentTimeMillis()));

            planRepository.save(plan);
            return ResponseEntity.ok(ApiResponse.ok(plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{planId}")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> updatePlan(@PathVariable String planId, @RequestBody Map<String, Object> body) {
        try {
            ModulePlan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

            if (body.containsKey("planLabel")) plan.setPlanLabel((String) body.get("planLabel"));
            if (body.containsKey("description")) plan.setDescription((String) body.get("description"));
            if (body.containsKey("features")) plan.setFeatures((String) body.get("features"));
            if (body.containsKey("sortOrder")) plan.setSortOrder(((Number) body.get("sortOrder")).intValue());
            if (body.containsKey("isActive")) plan.setIsActive((Boolean) body.get("isActive"));

            planRepository.save(plan);
            return ResponseEntity.ok(ApiResponse.ok(plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{planId}")
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> deletePlan(@PathVariable String planId) {
        try {
            ModulePlan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
            plan.setIsActive(false);
            planRepository.save(plan);
            return ResponseEntity.ok(ApiResponse.ok("Plan deactivated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
