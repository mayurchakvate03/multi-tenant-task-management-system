package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.CreateUserRequest;
import com.example.taskmanagement.dto.UserResponse;
import com.example.taskmanagement.security.TenantUserPrincipal;
import com.example.taskmanagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        return userService.createUser(request, principal);
    }
}
