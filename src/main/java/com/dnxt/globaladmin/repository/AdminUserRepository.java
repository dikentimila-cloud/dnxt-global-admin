package com.dnxt.globaladmin.repository;

import com.dnxt.globaladmin.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, String> {

    Optional<AdminUser> findByUsername(String username);

    Optional<AdminUser> findByEmail(String email);

    List<AdminUser> findByIsActiveTrue();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
