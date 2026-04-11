package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.dto.LoginResponse;
import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.service.AuditService;
import com.dnxt.globaladmin.service.GoogleAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthController.class);

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private AuditService auditService;

    // Temporary store for SSO tokens — keyed by one-time code, expires after use
    private final ConcurrentHashMap<String, LoginResponse> pendingTokens = new ConcurrentHashMap<>();

    /**
     * GET /api/auth/google/url — returns the Google OAuth2 authorization URL.
     * Frontend redirects the browser to this URL.
     */
    @GetMapping("/url")
    public ResponseEntity<ApiResponse> getAuthUrl() {
        String url = googleAuthService.getAuthorizationUrl();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("url", url)));
    }

    /**
     * GET /api/auth/google/callback — Google redirects here after user consents.
     * Exchanges the auth code for tokens, verifies identity, issues JWT.
     * Redirects to frontend with token in URL fragment.
     */
    @GetMapping("/callback")
    public void callback(@RequestParam("code") String code,
                         HttpServletRequest httpRequest,
                         HttpServletResponse httpResponse) throws IOException {
        try {
            LoginResponse response = googleAuthService.handleCallback(code);

            auditService.log(response.getUserId(), response.getUsername(),
                    "GOOGLE_SSO_LOGIN", "USER", response.getUserId(),
                    response.getEmail(), "Role: " + response.getRole(), httpRequest);

            // Store token with a one-time code, redirect to frontend
            // Frontend exchanges the code for the token via API (no inline script needed)
            String oneTimeCode = UUID.randomUUID().toString();
            pendingTokens.put(oneTimeCode, response);

            httpResponse.sendRedirect("/login?sso_code=" + oneTimeCode);

        } catch (SecurityException e) {
            auditService.log("GOOGLE_SSO_REJECTED", e.getMessage(), httpRequest);
            log.warn("Google SSO rejected: {}", e.getMessage());
            httpResponse.sendRedirect("/login?error=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));

        } catch (Exception e) {
            auditService.log("GOOGLE_SSO_FAILED", e.getMessage(), httpRequest);
            log.error("Google SSO failed", e);
            httpResponse.sendRedirect("/login?error=" + java.net.URLEncoder.encode("Google sign-in failed. Please try again.", "UTF-8"));
        }
    }

    /**
     * POST /api/auth/google/exchange — Exchange one-time SSO code for login response.
     * Called by the frontend after Google callback redirect.
     * The code is single-use and removed immediately after exchange.
     */
    @PostMapping("/exchange")
    public ResponseEntity<ApiResponse> exchangeCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("SSO code is required"));
        }

        LoginResponse response = pendingTokens.remove(code);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid or expired SSO code"));
        }

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * POST /api/auth/google/token — alternative flow for SPA.
     * Frontend sends the Google auth code directly, backend returns JWT in response body.
     */
    @PostMapping("/token")
    public ResponseEntity<ApiResponse> exchangeToken(@RequestBody Map<String, String> body,
                                                      HttpServletRequest httpRequest) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Authorization code is required"));
        }

        try {
            LoginResponse response = googleAuthService.handleCallback(code);

            auditService.log(response.getUserId(), response.getUsername(),
                    "GOOGLE_SSO_LOGIN", "USER", response.getUserId(),
                    response.getEmail(), "Role: " + response.getRole(), httpRequest);

            return ResponseEntity.ok(ApiResponse.ok(response));

        } catch (SecurityException e) {
            auditService.log("GOOGLE_SSO_REJECTED", e.getMessage(), httpRequest);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            auditService.log("GOOGLE_SSO_FAILED", e.getMessage(), httpRequest);
            log.error("Google SSO failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Google sign-in failed"));
        }
    }
}
