package com.example.taskmanagement.dto;

import com.example.taskmanagement.domain.UserRole;

public record UserResponse(Long id, String email, UserRole role) {
}
