package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.dto.ChangePasswordRequest;
import com.dnxt.globaladmin.dto.LoginRequest;
import com.dnxt.globaladmin.dto.LoginResponse;
import com.dnxt.globaladmin.entity.AdminPermission;
import com.dnxt.globaladmin.entity.AdminUser;
import com.dnxt.globaladmin.repository.AdminPermissionRepository;
import com.dnxt.globaladmin.repository.AdminUserRepository;
import com.dnxt.globaladmin.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AdminUserRepository userRepository;

    @Autowired
    private AdminPermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private EmailDomainValidator emailDomainValidator;

    @Value("${admin.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${admin.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Gate 1: Email domain check — before anything else
        if (!emailDomainValidator.isEmailAllowed(email)) {
            log.warn("Login rejected — unauthorized email domain: {}", email);
            throw new SecurityException("Unauthorized email domain. Only @" +
                    emailDomainValidator.getAllowedDomainsSummary() + " addresses are allowed.");
        }

        // Find user by email
        AdminUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login attempt with unknown email: {}", email);
                    return new IllegalArgumentException("Invalid email or password");
                });

        // Gate 2: Account active check
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Login attempt for disabled account: {}", email);
            throw new IllegalArgumentException("Account is disabled");
        }

        // Gate 3: Account lockout check
        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().getTime() > System.currentTimeMillis()) {
                long minutesLeft = (user.getLockedUntil().getTime() - System.currentTimeMillis()) / 60000;
                log.warn("Login attempt for locked account: {} (locked for {} more minutes)", email, minutesLeft);
                throw new SecurityException("Account is locked. Try again in " + minutesLeft + " minutes.");
            } else {
                // Lock period expired — reset
                user.setLockedUntil(null);
                user.setFailedAttempts(0);
            }
        }

        // Gate 4: Password verification
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedAttempt(user);
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Login success — reset failed attempts, update last login
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(new Timestamp(System.currentTimeMillis()));
        userRepository.save(user);

        List<String> permissionCodes = loadPermissionCodes(user.getRoleId());
        String token = tokenProvider.generateToken(user.getUserId(), user.getEmail());

        String roleName = user.getRole() != null ? user.getRole().getRoleLabel() : null;
        if (roleName == null && user.getRole() != null) {
            roleName = user.getRole().getRoleName();
        }

        log.info("User '{}' logged in successfully with role '{}'", user.getEmail(), roleName);

        return new LoginResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roleName,
                permissionCodes,
                Boolean.TRUE.equals(user.getMustChangePassword())
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse getCurrentUser(String userId) {
        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<String> permissionCodes = loadPermissionCodes(user.getRoleId());

        String roleName = user.getRole() != null ? user.getRole().getRoleLabel() : null;
        if (roleName == null && user.getRole() != null) {
            roleName = user.getRole().getRoleName();
        }

        return new LoginResponse(
                null,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roleName,
                permissionCodes,
                Boolean.TRUE.equals(user.getMustChangePassword())
        );
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setModifiedBy(userId);
        user.setModifiedDate(new Timestamp(System.currentTimeMillis()));
        userRepository.save(user);

        log.info("Password changed for user {}", user.getEmail());
    }

    private void handleFailedAttempt(AdminUser user) {
        int attempts = (user.getFailedAttempts() != null ? user.getFailedAttempts() : 0) + 1;
        user.setFailedAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            long lockoutMs = lockoutDurationMinutes * 60_000L;
            user.setLockedUntil(new Timestamp(System.currentTimeMillis() + lockoutMs));
            log.warn("Account locked after {} failed attempts: {}", attempts, user.getEmail());
        }

        userRepository.save(user);
        log.warn("Failed login attempt {} of {} for: {}", attempts, maxFailedAttempts, user.getEmail());
    }

    private List<String> loadPermissionCodes(String roleId) {
        if (roleId == null) {
            return List.of();
        }
        return permissionRepository.findByRoleId(roleId).stream()
                .map(AdminPermission::getCode)
                .collect(Collectors.toList());
    }
}
