package com.dnxt.globaladmin.security;

import com.dnxt.globaladmin.entity.AdminPermission;
import com.dnxt.globaladmin.entity.AdminUser;
import com.dnxt.globaladmin.repository.AdminPermissionRepository;
import com.dnxt.globaladmin.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AdminUserRepository userRepository;

    @Autowired
    private AdminPermissionRepository permissionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (token != null && tokenProvider.validateToken(token)) {
                String userId = tokenProvider.getUserIdFromToken(token);

                Optional<AdminUser> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    AdminUser user = userOpt.get();

                    if (Boolean.TRUE.equals(user.getIsActive())) {
                        // Check if account is locked
                        if (user.getLockedUntil() != null &&
                            user.getLockedUntil().getTime() > System.currentTimeMillis()) {
                            log.warn("JWT valid but account is locked: {}", userId);
                            chain.doFilter(request, response);
                            return;
                        }

                        List<AdminPermission> permissions = permissionRepository.findByRoleId(user.getRoleId());
                        List<SimpleGrantedAuthority> authorities = permissions.stream()
                                .map(p -> new SimpleGrantedAuthority(p.getCode()))
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        log.warn("JWT valid but user account is disabled: {}", userId);
                    }
                } else {
                    log.warn("JWT valid but user not found in database: {}", userId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process JWT authentication", e);
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        if (!path.startsWith("/api/")) {
            return true;
        }
        if (path.equals("/api/auth/login") || path.startsWith("/api/auth/google/")) {
            return true;
        }
        if (path.startsWith("/actuator")) {
            return true;
        }

        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
