package com.example.taskmanagement.service;

import com.example.taskmanagement.domain.AppUser;
import com.example.taskmanagement.dto.CreateUserRequest;
import com.example.taskmanagement.dto.UserResponse;
import com.example.taskmanagement.exception.ConflictException;
import com.example.taskmanagement.security.TenantUserPrincipal;
import com.example.taskmanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, TenantUserPrincipal actor) {
        String email = TenantService.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        AppUser saved = userRepository.save(new AppUser(
                actor.getTenantId(), email, passwordEncoder.encode(request.password()), request.role()));
        return new UserResponse(saved.getId(), saved.getEmail(), saved.getRole());
    }
}
