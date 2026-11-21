package com.example.taskmanagement.dto;

import java.time.Instant;

public record TenantRegistrationResponse(
        String tenantId,
        String tenantName,
        Long adminUserId,
        String adminEmail,
        Instant createdAt) {
}
