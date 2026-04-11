package com.dnxt.globaladmin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "module_plan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ModulePlan {

    @Id
    @Column(name = "plan_id")
    private String planId;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "plan_label", nullable = false)
    private String planLabel;

    @Column(name = "description")
    private String description;

    @Column(name = "features", nullable = false)
    private String features;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_date")
    private Timestamp createdDate;
}
