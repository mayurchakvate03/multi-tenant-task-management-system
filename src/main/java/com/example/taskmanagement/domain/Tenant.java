package com.example.taskmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false, unique = true)
    private String tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Tenant() {
    }

    public Tenant(String id, String name) {
        this.id = id;
        this.tenantId = id;
        this.name = name;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
