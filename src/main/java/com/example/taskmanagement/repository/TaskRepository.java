package com.example.taskmanagement.repository;

import com.example.taskmanagement.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<Task> findAllByTenantIdAndAssignedToOrderByCreatedAtDesc(String tenantId, Long assignedTo);

    Optional<Task> findByIdAndTenantId(Long id, String tenantId);
}
