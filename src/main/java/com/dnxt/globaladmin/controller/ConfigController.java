package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.security.PermissionCheck;
import com.dnxt.globaladmin.service.AuditService;
import com.dnxt.globaladmin.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PermissionCheck("CONFIG_VIEW")
    public ResponseEntity<ApiResponse> getAllConfig() {
        return ResponseEntity.ok(ApiResponse.ok(configService.getAllConfig()));
    }

    @GetMapping("/category/{category}")
    @PermissionCheck("CONFIG_VIEW")
    public ResponseEntity<ApiResponse> getConfigByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.ok(configService.getConfigByCategory(category)));
    }

    @PutMapping
    @PermissionCheck("CONFIG_EDIT")
    public ResponseEntity<ApiResponse> updateConfig(@RequestBody Map<String, String> updates,
                                                     Authentication auth,
                                                     HttpServletRequest httpRequest) {
        configService.updateConfig(updates, auth.getName());
        auditService.log(auth.getName(), null, "CONFIG_UPDATED",
                "CONFIG", null, null,
                "Keys: " + String.join(", ", updates.keySet()), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Configuration updated"));
    }
}
