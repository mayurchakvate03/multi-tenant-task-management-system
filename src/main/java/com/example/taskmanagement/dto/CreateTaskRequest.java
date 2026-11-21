package com.example.taskmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 4000) String description) {
}
