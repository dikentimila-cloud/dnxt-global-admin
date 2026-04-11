package com.dnxt.globaladmin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "platform_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PlatformConfig {

    @Id
    @Column(name = "config_key")
    private String configKey;

    @Column(name = "config_value")
    private String configValue;

    @Column(name = "category")
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "is_secret")
    private Boolean isSecret;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_date")
    private Timestamp modifiedDate;
}
