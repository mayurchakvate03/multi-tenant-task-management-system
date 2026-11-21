package com.example.taskmanagement.service;

import com.example.taskmanagement.domain.AppUser;
import com.example.taskmanagement.domain.Tenant;
import com.example.taskmanagement.domain.UserRole;
import com.example.taskmanagement.dto.RegisterTenantRequest;
import com.example.taskmanagement.dto.TenantRegistrationResponse;
import com.example.taskmanagement.exception.ConflictException;
import com.example.taskmanagement.repository.TenantRepository;
import com.example.taskmanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TenantRegistrationResponse register(RegisterTenantRequest request) {
        String email = normalizeEmail(request.adminEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        Tenant tenant = tenantRepository.save(new Tenant(UUID.randomUUID().toString(), request.tenantName().trim()));
        AppUser admin = userRepository.save(new AppUser(
                tenant.getTenantId(), email, passwordEncoder.encode(request.adminPassword()), UserRole.ADMIN));
        return new TenantRegistrationResponse(
                tenant.getId(), tenant.getName(), admin.getId(), admin.getEmail(), tenant.getCreatedAt());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
