package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.dto.UserCreateRequest;
import com.dnxt.globaladmin.entity.AdminUser;
import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.security.PermissionCheck;
import com.dnxt.globaladmin.service.AuditService;
import com.dnxt.globaladmin.service.UserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserManagementService userService;

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PermissionCheck("USER_VIEW")
    public ResponseEntity<ApiResponse> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers()));
    }

    @GetMapping("/{userId}")
    @PermissionCheck("USER_VIEW")
    public ResponseEntity<ApiResponse> getUser(@PathVariable String userId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(userService.getUser(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    @PermissionCheck("USER_CREATE")
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody UserCreateRequest request,
                                                   Authentication auth,
                                                   HttpServletRequest httpRequest) {
        try {
            AdminUser user = userService.createUser(request, auth.getName());
            auditService.log(auth.getName(), null, "USER_CREATED",
                    "USER", user.getUserId(), user.getEmail(),
                    "Role: " + user.getRoleId(), httpRequest);
            return ResponseEntity.ok(ApiResponse.ok(user));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    @PermissionCheck("USER_EDIT")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable String userId,
                                                   @Valid @RequestBody UserCreateRequest request,
                                                   Authentication auth,
                                                   HttpServletRequest httpRequest) {
        try {
            AdminUser user = userService.updateUser(userId, request, auth.getName());
            auditService.log(auth.getName(), null, "USER_UPDATED",
                    "USER", userId, user.getEmail(), null, httpRequest);
            return ResponseEntity.ok(ApiResponse.ok(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/deactivate")
    @PermissionCheck("USER_DEACTIVATE")
    public ResponseEntity<ApiResponse> deactivateUser(@PathVariable String userId,
                                                       Authentication auth,
                                                       HttpServletRequest httpRequest) {
        try {
            AdminUser user = userService.getUser(userId);
            userService.deactivateUser(userId, auth.getName());
            auditService.log(auth.getName(), null, "USER_DEACTIVATED",
                    "USER", userId, user.getEmail(), null, httpRequest);
            return ResponseEntity.ok(ApiResponse.ok("User deactivated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/reset-password")
    @PermissionCheck("USER_EDIT")
    public ResponseEntity<ApiResponse> resetPassword(@PathVariable String userId,
                                                      Authentication auth,
                                                      HttpServletRequest httpRequest) {
        try {
            userService.resetPassword(userId, auth.getName());
            auditService.log(auth.getName(), null, "PASSWORD_RESET",
                    "USER", userId, null, "Admin-initiated reset", httpRequest);
            return ResponseEntity.ok(ApiResponse.ok("Password reset. User will receive new credentials."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
