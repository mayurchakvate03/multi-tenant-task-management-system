package com.example.taskmanagement.security;

import com.example.taskmanagement.domain.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class TenantUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String tenantId;
    private final UserRole role;

    public TenantUserPrincipal(Long id, String email, String tenantId, UserRole role) {
        this.id = id;
        this.email = email;
        this.tenantId = tenantId;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UserRole getRole() {
        return role;
    }
}
