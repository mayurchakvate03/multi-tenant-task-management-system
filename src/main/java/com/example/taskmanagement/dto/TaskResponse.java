package com.example.taskmanagement.dto;

import com.example.taskmanagement.domain.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Long assignedTo,
        Instant createdAt) {
}
