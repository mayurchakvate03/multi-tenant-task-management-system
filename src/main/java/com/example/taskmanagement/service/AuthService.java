package com.example.taskmanagement.service;

import com.example.taskmanagement.domain.AppUser;
import com.example.taskmanagement.dto.AuthResponse;
import com.example.taskmanagement.dto.LoginRequest;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        String email = TenantService.normalizeEmail(request.email());
        AppUser user = userRepository.findByEmail(email)
                .filter(account -> passwordEncoder.matches(request.password(), account.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthResponse(jwtService.createToken(user), "Bearer", jwtService.getExpirationSeconds());
    }
}
