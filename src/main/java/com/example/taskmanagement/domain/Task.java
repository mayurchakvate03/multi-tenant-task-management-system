package com.example.taskmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Task() {
    }

    public Task(String tenantId, String title, String description) {
        this.tenantId = tenantId;
        this.title = title;
        this.description = description;
        this.status = TaskStatus.OPEN;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void assignTo(Long userId) {
        assignedTo = userId;
    }

    public void updateStatus(TaskStatus newStatus) {
        status = newStatus;
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
