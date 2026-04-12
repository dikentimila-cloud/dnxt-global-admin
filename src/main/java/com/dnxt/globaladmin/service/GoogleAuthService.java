package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.dto.LoginResponse;
import com.dnxt.globaladmin.entity.AdminPermission;
import com.dnxt.globaladmin.entity.AdminRole;
import com.dnxt.globaladmin.entity.AdminUser;
import com.dnxt.globaladmin.repository.AdminPermissionRepository;
import com.dnxt.globaladmin.repository.AdminRoleRepository;
import com.dnxt.globaladmin.repository.AdminUserRepository;
import com.dnxt.globaladmin.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Google Workspace SSO service.
 * ALL configuration is read from platform_config (Settings page) — single source of truth.
 */
@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    @Autowired
    private AdminUserRepository userRepository;

    @Autowired
    private AdminRoleRepository roleRepository;

    @Autowired
    private AdminPermissionRepository permissionRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private EmailDomainValidator emailDomainValidator;

    @Autowired
    private ConfigService configService;

    // --- All config from platform_config DB table ---

    private String cfg(String key) {
        String val = configService.getConfigValue(key);
        return val != null ? val.trim() : "";
    }

    public boolean isSsoEnabled() {
        return "true".equalsIgnoreCase(cfg("sso.enabled"));
    }

    public boolean isPasswordFallbackEnabled() {
        return "true".equalsIgnoreCase(cfg("sso.password_fallback"));
    }

    public String getAuthorizationUrl() {
        String scopes = cfg("sso.google.scopes").replace(" ", "%20");
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + cfg("sso.google.client_id")
                + "&redirect_uri=" + cfg("sso.google.redirect_uri")
                + "&response_type=code"
                + "&scope=" + scopes
                + "&access_type=offline"
                + "&prompt=" + cfg("sso.google.prompt")
                + "&hd=" + cfg("sso.google.hosted_domain");
    }

    @Transactional
    public LoginResponse handleCallback(String authCode) throws Exception {
        String clientId = cfg("sso.google.client_id");
        String clientSecret = cfg("sso.google.client_secret");
        String redirectUri = cfg("sso.google.redirect_uri");

        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                authCode,
                redirectUri
        ).execute();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken = verifier.verify(tokenResponse.getIdToken());
        if (idToken == null) {
            throw new SecurityException("Invalid Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail().toLowerCase().trim();
        String googleId = payload.getSubject();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String avatarUrl = (String) payload.get("picture");
        Boolean emailVerified = payload.getEmailVerified();

        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new SecurityException("Google email is not verified");
        }

        if (!emailDomainValidator.isEmailAllowed(email)) {
            log.warn("Google SSO rejected — unauthorized email domain: {}", email);
            throw new SecurityException("Unauthorized email domain. Only @dnxtsolutions.com addresses are allowed.");
        }

        AdminUser user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            if ("true".equalsIgnoreCase(cfg("sso.auto_create_users"))) {
                user = createGoogleUser(email, googleId, firstName, lastName, avatarUrl);
                log.info("Auto-created user via SSO: {}", email);
            } else {
                log.warn("Google SSO rejected — user not pre-registered: {}", email);
                throw new SecurityException("Access denied. Your account has not been set up yet. Contact a Super Admin to get invited.");
            }
        } else {
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                user.setAuthProvider("GOOGLE");
            }
            if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
            if (firstName != null) user.setFirstName(firstName);
            if (lastName != null) user.setLastName(lastName);
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new SecurityException("Account is disabled. Contact your administrator.");
        }

        user.setLastLogin(new Timestamp(System.currentTimeMillis()));
        userRepository.save(user);

        List<String> permissionCodes = loadPermissionCodes(user.getRoleId());
        String token = tokenProvider.generateToken(user.getUserId(), user.getEmail());

        String roleName = user.getRole() != null ? user.getRole().getRoleLabel() : null;
        if (roleName == null && user.getRole() != null) {
            roleName = user.getRole().getRoleName();
        }

        log.info("Google SSO login successful: {} (role: {})", email, roleName);

        return new LoginResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roleName,
                permissionCodes,
                false
        );
    }

    private AdminUser createGoogleUser(String email, String googleId,
                                        String firstName, String lastName, String avatarUrl) {
        AdminRole viewerRole = roleRepository.findByRoleName("VIEWER")
                .orElseThrow(() -> new IllegalStateException("VIEWER role not found"));

        String username = email.substring(0, email.indexOf("@"));
        if (userRepository.existsByUsername(username)) {
            username = username + "-" + UUID.randomUUID().toString().substring(0, 4);
        }

        AdminUser user = new AdminUser();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(null);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoleId(viewerRole.getRoleId());
        user.setIsActive(true);
        user.setMustChangePassword(false);
        user.setFailedAttempts(0);
        user.setGoogleId(googleId);
        user.setAvatarUrl(avatarUrl);
        user.setAuthProvider("GOOGLE");
        user.setCreatedBy("GOOGLE_SSO");
        user.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        user.setModifiedBy("GOOGLE_SSO");
        user.setModifiedDate(new Timestamp(System.currentTimeMillis()));

        return userRepository.save(user);
    }

    private List<String> loadPermissionCodes(String roleId) {
        if (roleId == null) return List.of();
        return permissionRepository.findByRoleId(roleId).stream()
                .map(AdminPermission::getCode)
                .collect(Collectors.toList());
    }
}
