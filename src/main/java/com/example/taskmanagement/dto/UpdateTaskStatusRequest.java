package com.example.taskmanagement.dto;

import com.example.taskmanagement.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull TaskStatus status) {
}
