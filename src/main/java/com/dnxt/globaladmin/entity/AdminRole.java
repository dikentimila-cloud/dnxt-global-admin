package com.dnxt.globaladmin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "admin_role")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdminRole {

    @Id
    @Column(name = "role_id")
    private String roleId;

    @Column(name = "role_name", unique = true, nullable = false)
    private String roleName;

    @Column(name = "role_label", nullable = false)
    private String roleLabel;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system")
    private Boolean isSystem;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "modified_date")
    private Timestamp modifiedDate;
}
