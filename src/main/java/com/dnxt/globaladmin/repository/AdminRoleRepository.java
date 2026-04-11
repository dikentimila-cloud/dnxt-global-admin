package com.dnxt.globaladmin.repository;

import com.dnxt.globaladmin.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRoleRepository extends JpaRepository<AdminRole, String> {

    Optional<AdminRole> findByRoleName(String roleName);
}
