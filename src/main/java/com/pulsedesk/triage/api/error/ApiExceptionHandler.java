package com.pulsedesk.triage.api.error;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.pulsedesk.triage.service.NotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<?> handleNotFound(NotFoundException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI(), null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      errors.put(fe.getField(), fe.getDefaultMessage());
    }
    return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), errors));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<?> handleMissingRoute(Exception ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(problem(HttpStatus.NOT_FOUND, "Not found", req.getRequestURI(), null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleGeneric(Exception ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req.getRequestURI(), null));
  }

  private Map<String, Object> problem(HttpStatus status, String message, String path, Object details) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    body.put("path", path);
    if (details != null) body.put("details", details);
    return body;
  }
}

