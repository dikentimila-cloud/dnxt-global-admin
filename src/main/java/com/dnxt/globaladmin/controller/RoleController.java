package com.dnxt.globaladmin.controller;

import com.dnxt.globaladmin.model.ApiResponse;
import com.dnxt.globaladmin.repository.AdminRoleRepository;
import com.dnxt.globaladmin.security.PermissionCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private AdminRoleRepository roleRepository;

    @GetMapping
    @PermissionCheck("USER_VIEW")
    public ResponseEntity<ApiResponse> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok(roleRepository.findAll()));
    }
}
