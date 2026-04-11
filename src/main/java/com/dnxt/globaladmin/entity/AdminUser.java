package com.dnxt.globaladmin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "admin_user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdminUser {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "role_id")
    private String roleId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private AdminRole role;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "must_change_password")
    private Boolean mustChangePassword;

    @Column(name = "last_login")
    private Timestamp lastLogin;

    @Column(name = "failed_attempts")
    private Integer failedAttempts;

    @Column(name = "locked_until")
    private Timestamp lockedUntil;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_date")
    private Timestamp modifiedDate;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "auth_provider")
    private String authProvider;
}
