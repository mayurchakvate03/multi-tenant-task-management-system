package com.example.taskmanagement.repository;

import com.example.taskmanagement.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, String> {
}
