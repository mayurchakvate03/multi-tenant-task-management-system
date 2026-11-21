package com.example.taskmanagement;

import com.example.taskmanagement.domain.UserRole;
import com.example.taskmanagement.dto.ApiError;
import com.example.taskmanagement.exception.GlobalExceptionHandler;
import com.example.taskmanagement.security.JwtService;
import com.example.taskmanagement.security.TenantUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskManagementIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private JwtService jwtService;

    private GlobalExceptionHandler exceptionHandler;

    private ConfigurableApplicationContext applicationContext;

    @BeforeAll
    void startApplication() {
        applicationContext = SpringApplication.run(TaskManagementApplication.class);
        mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) applicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = applicationContext.getBean(ObjectMapper.class);
        jwtService = applicationContext.getBean(JwtService.class);
        exceptionHandler = applicationContext.getBean(GlobalExceptionHandler.class);
    }

    @AfterAll
    void stopApplication() {
        applicationContext.close();
    }

    @Test
    void registrationAndLoginIssueJwtWithTenantAndRoleClaims() throws Exception {
        String adminEmail = newEmail();
        MvcResult registration = mockMvc.perform(post("/tenant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tenantName", "Acme", "adminEmail", adminEmail, "adminPassword", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").isNotEmpty())
                .andExpect(jsonPath("$.adminEmail").value(adminEmail))
                .andReturn();
        String tenantId = read(registration).path("tenantId").asText();

        String token = login(adminEmail);
        TenantUserPrincipal principal = jwtService.parsePrincipal(token);
        assertEquals(tenantId, principal.getTenantId());
        assertEquals(UserRole.ADMIN, principal.getRole());
        assertEquals(adminEmail, principal.getUsername());
        assertNotNull(principal.getAuthorities());
        assertTrue(principal.isEnabled());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path").value("/tasks"));
        mockMvc.perform(get("/tasks").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managerCreatesAndAssignsTasksAndUserSeesOnlyAssignedTasks() throws Exception {
        String adminEmail = registerTenant();
        String adminToken = login(adminEmail);
        String managerEmail = newEmail();
        String userEmail = newEmail();
        long managerId = createUser(adminToken, managerEmail, "MANAGER");
        long userId = createUser(adminToken, userEmail, "USER");
        String managerToken = login(managerEmail);
        String userToken = login(userEmail);

        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", newEmail(), "password", PASSWORD, "role", "USER")))
                .andExpect(status().isForbidden());

        long assignedTaskId = createTask(managerToken, "Prepare release");
        long unassignedTaskId = createTask(managerToken, "Write notes");
        mockMvc.perform(put("/tasks/{id}/assign", assignedTaskId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("assignedTo", userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value(userId));

        mockMvc.perform(get("/tasks").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assignedTaskId))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/tasks").header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/tasks").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(put("/tasks/{id}/status", assignedTaskId)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", "COMPLETED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(put("/tasks/{id}/status", unassignedTaskId)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", "IN_PROGRESS")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/tasks")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "Forbidden", "description", "")))
                .andExpect(status().isForbidden());
        assertTrue(managerId > 0);
    }

    @Test
    void otherTenantsCannotReadOrAssignTasksOrUsers() throws Exception {
        String adminA = registerTenant();
        String adminB = registerTenant();
        String tokenA = login(adminA);
        String tokenB = login(adminB);
        String managerA = newEmail();
        String managerB = newEmail();
        String userA = newEmail();
        String userB = newEmail();
        createUser(tokenA, managerA, "MANAGER");
        createUser(tokenA, userA, "USER");
        createUser(tokenB, managerB, "MANAGER");
        createUser(tokenB, userB, "USER");
        String managerTokenA = login(managerA);
        String managerTokenB = login(managerB);
        long taskA = createTask(managerTokenA, "Tenant A task");
        long taskB = createTask(managerTokenB, "Tenant B task");
        long userBId = createUser(tokenB, newEmail(), "USER");

        mockMvc.perform(get("/tasks").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(taskB));
        mockMvc.perform(put("/tasks/{id}/assign", taskA)
                        .header("Authorization", bearer(managerTokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("assignedTo", userBId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/tasks/{id}/assign", taskB)
                        .header("Authorization", bearer(managerTokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("assignedTo", 99999999)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotChangeAnotherUsersAssignedTask() throws Exception {
        String adminEmail = registerTenant();
        String adminToken = login(adminEmail);
        String managerEmail = newEmail();
        String ownerEmail = newEmail();
        String otherEmail = newEmail();
        createUser(adminToken, managerEmail, "MANAGER");
        long ownerId = createUser(adminToken, ownerEmail, "USER");
        createUser(adminToken, otherEmail, "USER");
        String managerToken = login(managerEmail);
        String ownerToken = login(ownerEmail);
        String otherToken = login(otherEmail);
        long taskId = createTask(managerToken, "Owned task");
        mockMvc.perform(put("/tasks/{id}/assign", taskId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("assignedTo", ownerId)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/tasks/{id}/status", taskId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", "COMPLETED")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/tasks").header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(put("/tasks/{id}/status", taskId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", "IN_PROGRESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void validationRejectsUnknownTenantInputAndMalformedPayloads() throws Exception {
        mockMvc.perform(post("/tenant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tenantName", "Invalid", "adminEmail", newEmail(), "adminPassword", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.adminPassword").exists());
        mockMvc.perform(post("/tenant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tenantName", "Invalid", "adminEmail", newEmail(), "adminPassword", PASSWORD,
                                "tenant_id", "attacker-controlled")))
                .andExpect(status().isBadRequest());

        String adminEmail = registerTenant();
        String adminToken = login(adminEmail);
        mockMvc.perform(post("/tenant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tenantName", "Duplicate", "adminEmail", adminEmail, "adminPassword", PASSWORD)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "not-an-email", "password", PASSWORD, "role", "USER")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", adminEmail, "password", "wrong-password")))
                .andExpect(status().isUnauthorized());

        String managerEmail = newEmail();
        String userEmail = newEmail();
        createUser(adminToken, managerEmail, "MANAGER");
        long userId = createUser(adminToken, userEmail, "USER");
        String managerToken = login(managerEmail);
        String userToken = login(userEmail);
        mockMvc.perform(post("/tasks")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "", "description", "")))
                .andExpect(status().isBadRequest());
        long taskId = createTask(managerToken, "Valid task");
        mockMvc.perform(put("/tasks/{id}/assign", taskId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("assignedTo", 0)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/tasks/{id}/assign", taskId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("assignedTo", userId, "tenant_id", "attacker-controlled")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/tasks/{id}/status", taskId)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void globalHandlerReturnsSanitizedUnexpectedErrorsAndConstraintErrors() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        ApiError violation = exceptionHandler.handleConstraintViolation(
                new ConstraintViolationException("invalid value", Set.of()), request);
        assertEquals(400, violation.status());
        assertEquals("invalid value", violation.message());

        ApiError unexpected = exceptionHandler.handleUnexpected(new IllegalStateException("secret"), request);
        assertEquals(500, unexpected.status());
        assertEquals("An unexpected error occurred", unexpected.message());
        assertTrue(unexpected.details().isEmpty());
    }

    private String registerTenant() throws Exception {
        String email = newEmail();
        mockMvc.perform(post("/tenant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tenantName", "Tenant " + UUID.randomUUID(), "adminEmail", email,
                                "adminPassword", PASSWORD)))
                .andExpect(status().isCreated());
        return email;
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        return read(result).path("accessToken").asText();
    }

    private long createUser(String adminToken, String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", PASSWORD, "role", role)))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).path("id").asLong();
    }

    private long createTask(String managerToken, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/tasks")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", title, "description", "Integration test task")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        return read(result).path("id").asLong();
    }

    private String json(Object... fields) throws Exception {
        var body = objectMapper.createObjectNode();
        for (int i = 0; i < fields.length; i += 2) {
            body.putPOJO((String) fields[i], fields[i + 1]);
        }
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String newEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
