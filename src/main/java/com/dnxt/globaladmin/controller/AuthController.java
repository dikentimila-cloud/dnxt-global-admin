package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.dto.ChangePasswordRequest;
import com.dnxt.globaladmin.dto.LoginRequest;
import com.dnxt.globaladmin.dto.LoginResponse;
import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.security.LoginRateLimiter;
import com.dnxt.globaladmin.service.AuditService;
import com.dnxt.globaladmin.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private LoginRateLimiter rateLimiter;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);

        // Rate limit check
        if (rateLimiter.isRateLimited(clientIp)) {
            auditService.log("LOGIN_RATE_LIMITED",
                    "IP: " + clientIp + ", email: " + request.getEmail(), httpRequest);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Too many login attempts. Please try again later."));
        }

        try {
            LoginResponse response = authService.login(request);
            auditService.log(response.getUserId(), response.getUsername(), "LOGIN_SUCCESS",
                    "USER", response.getUserId(), response.getEmail(),
                    "Role: " + response.getRole(), httpRequest);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (SecurityException e) {
            auditService.log("LOGIN_REJECTED",
                    "Email: " + request.getEmail() + " — " + e.getMessage(), httpRequest);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            auditService.log("LOGIN_FAILED",
                    "Email: " + request.getEmail() + " — " + e.getMessage(), httpRequest);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        try {
            String userId = authentication.getName();
            LoginResponse response = authService.getCurrentUser(userId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(Authentication authentication,
                                                       @Valid @RequestBody ChangePasswordRequest request,
                                                       HttpServletRequest httpRequest) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        try {
            String userId = authentication.getName();
            authService.changePassword(userId, request);
            auditService.log(userId, null, "PASSWORD_CHANGED",
                    "USER", userId, null, null, httpRequest);
            return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
