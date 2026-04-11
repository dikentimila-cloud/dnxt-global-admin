package com.dnxt.globaladmin.repository;

import com.dnxt.globaladmin.entity.AdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminPermissionRepository extends JpaRepository<AdminPermission, String> {

    @Query(value = "SELECT p.* FROM admin_permission p " +
                   "INNER JOIN admin_role_permission rp ON p.permission_id = rp.permission_id " +
                   "WHERE rp.role_id = :roleId",
           nativeQuery = true)
    List<AdminPermission> findByRoleId(@Param("roleId") String roleId);
}
