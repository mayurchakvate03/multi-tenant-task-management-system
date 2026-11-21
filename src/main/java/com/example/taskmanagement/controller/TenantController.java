package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.RegisterTenantRequest;
import com.example.taskmanagement.dto.TenantRegistrationResponse;
import com.example.taskmanagement.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantRegistrationResponse register(@Valid @RequestBody RegisterTenantRequest request) {
        return tenantService.register(request);
    }
}
