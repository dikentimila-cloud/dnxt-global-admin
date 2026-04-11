package com.dnxt.globaladmin.service;

import com.dnxt.globaladmin.dto.UserCreateRequest;
import com.dnxt.globaladmin.entity.AdminUser;
import com.dnxt.globaladmin.repository.AdminRoleRepository;
import com.dnxt.globaladmin.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Service
public class UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%&*";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private AdminUserRepository userRepository;

    @Autowired
    private AdminRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailDomainValidator emailDomainValidator;

    @Transactional(readOnly = true)
    public List<AdminUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AdminUser> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public AdminUser getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Transactional
    public AdminUser createUser(UserCreateRequest request, String createdBy) {
        String email = request.getEmail().toLowerCase().trim();

        // Enforce email domain restriction
        if (!emailDomainValidator.isEmailAllowed(email)) {
            throw new SecurityException("Email domain not allowed. Only @" +
                    emailDomainValidator.getAllowedDomainsSummary() + " addresses permitted.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // Validate role exists
        roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRoleId()));

        // Generate username from email
        String username = email.substring(0, email.indexOf("@"));
        if (userRepository.existsByUsername(username)) {
            username = username + "-" + UUID.randomUUID().toString().substring(0, 4);
        }

        // Generate temp password
        String tempPassword = generateTempPassword();

        AdminUser user = new AdminUser();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRoleId(request.getRoleId());
        user.setIsActive(true);
        user.setMustChangePassword(true);
        user.setFailedAttempts(0);
        user.setCreatedBy(createdBy);
        user.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        user.setModifiedBy(createdBy);
        user.setModifiedDate(new Timestamp(System.currentTimeMillis()));

        userRepository.save(user);
        log.info("Created admin user: {} ({}) with temp password", username, email);

        // TODO: Send welcome email with temp password via mail service

        return user;
    }

    @Transactional
    public AdminUser updateUser(String userId, UserCreateRequest request, String modifiedBy) {
        AdminUser user = getUser(userId);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getRoleId() != null) {
            roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRoleId()));
            user.setRoleId(request.getRoleId());
        }
        user.setModifiedBy(modifiedBy);
        user.setModifiedDate(new Timestamp(System.currentTimeMillis()));

        userRepository.save(user);
        log.info("Updated admin user: {}", user.getEmail());
        return user;
    }

    @Transactional
    public void deactivateUser(String userId, String modifiedBy) {
        AdminUser user = getUser(userId);
        user.setIsActive(false);
        user.setModifiedBy(modifiedBy);
        user.setModifiedDate(new Timestamp(System.currentTimeMillis()));
        userRepository.save(user);
        log.info("Deactivated admin user: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(String userId, String modifiedBy) {
        AdminUser user = getUser(userId);
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setModifiedBy(modifiedBy);
        user.setModifiedDate(new Timestamp(System.currentTimeMillis()));
        userRepository.save(user);
        log.info("Password reset for admin user: {}", user.getEmail());
        // TODO: Send password reset email with temp password
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
