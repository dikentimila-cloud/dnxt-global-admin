package com.dnxt.globaladmin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "admin_permission")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdminPermission {

    @Id
    @Column(name = "permission_id")
    private String permissionId;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "module", nullable = false)
    private String module;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "description")
    private String description;

    @Column(name = "created_date")
    private Timestamp createdDate;
}
