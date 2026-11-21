package com.example.taskmanagement.exception;

import com.example.taskmanagement.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidRequest(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or contains an invalid value", Map.of(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "A record with the supplied unique value already exists", Map.of(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", Map.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of(), request);
    }

    private ApiError error(HttpStatus status, String message, Map<String, String> details, HttpServletRequest request) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details, request.getRequestURI());
    }
}
