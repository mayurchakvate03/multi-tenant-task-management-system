package com.example.taskmanagement.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}
