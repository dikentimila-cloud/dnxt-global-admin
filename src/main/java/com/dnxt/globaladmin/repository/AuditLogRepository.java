package com.dnxt.globaladmin.repository;

import com.dnxt.globaladmin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    Page<AuditLog> findByOrderByCreatedDateDesc(Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedDateDesc(String action, Pageable pageable);

    Page<AuditLog> findByUserIdOrderByCreatedDateDesc(String userId, Pageable pageable);

    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedDateDesc(String targetType, String targetId);

    long countByActionAndCreatedDateAfter(String action, Timestamp after);
}
