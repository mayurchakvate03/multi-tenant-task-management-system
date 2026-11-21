package com.example.taskmanagement.service;

import com.example.taskmanagement.domain.Task;
import com.example.taskmanagement.dto.AssignTaskRequest;
import com.example.taskmanagement.dto.CreateTaskRequest;
import com.example.taskmanagement.dto.TaskResponse;
import com.example.taskmanagement.dto.UpdateTaskStatusRequest;
import com.example.taskmanagement.exception.NotFoundException;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.TenantUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.example.taskmanagement.domain.UserRole.USER;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, TenantUserPrincipal actor) {
        Task saved = taskRepository.save(new Task(actor.getTenantId(), request.title().trim(), request.description()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(TenantUserPrincipal actor) {
        List<Task> tasks = actor.getRole() == USER
                ? taskRepository.findAllByTenantIdAndAssignedToOrderByCreatedAtDesc(actor.getTenantId(), actor.getId())
                : taskRepository.findAllByTenantIdOrderByCreatedAtDesc(actor.getTenantId());
        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TaskResponse assignTask(Long id, AssignTaskRequest request, TenantUserPrincipal actor) {
        Task task = findTask(id, actor);
        userRepository.findByIdAndTenantId(request.assignedTo(), actor.getTenantId())
                .orElseThrow(() -> new NotFoundException("User was not found in this tenant"));
        task.assignTo(request.assignedTo());
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long id, UpdateTaskStatusRequest request, TenantUserPrincipal actor) {
        Task task = findTask(id, actor);
        if (!Objects.equals(task.getAssignedTo(), actor.getId())) {
            throw new AccessDeniedException("Users may update only tasks assigned to them");
        }
        task.updateStatus(request.status());
        return toResponse(taskRepository.save(task));
    }

    private Task findTask(Long id, TenantUserPrincipal actor) {
        return taskRepository.findByIdAndTenantId(id, actor.getTenantId())
                .orElseThrow(() -> new NotFoundException("Task was not found in this tenant"));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getAssignedTo(), task.getCreatedAt());
    }
}
