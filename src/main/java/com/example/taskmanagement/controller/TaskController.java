package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.AssignTaskRequest;
import com.example.taskmanagement.dto.CreateTaskRequest;
import com.example.taskmanagement.dto.TaskResponse;
import com.example.taskmanagement.dto.UpdateTaskStatusRequest;
import com.example.taskmanagement.security.TenantUserPrincipal;
import com.example.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public TaskResponse create(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        return taskService.createTask(request, principal);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public List<TaskResponse> list(@AuthenticationPrincipal TenantUserPrincipal principal) {
        return taskService.listTasks(principal);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public TaskResponse assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignTaskRequest request,
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        return taskService.assignTask(id, request, principal);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('USER')")
    public TaskResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        return taskService.updateStatus(id, request, principal);
    }
}
