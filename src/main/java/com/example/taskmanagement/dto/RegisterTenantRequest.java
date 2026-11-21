package com.example.taskmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTenantRequest(
        @NotBlank @Size(max = 120) String tenantName,
        @NotBlank @Email @Size(max = 254) String adminEmail,
        @NotBlank @Size(min = 12, max = 72) String adminPassword) {
}
