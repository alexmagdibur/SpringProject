package ru.bmstu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 401 / 403 из RoleCheckAspect */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        return error(ex.getStatusCode().value(), ex.getReason());
    }

    /** 404 — нет обработчика для URL */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException ex) {
        return error(404, "No endpoint: " + ex.getHttpMethod() + " " + ex.getRequestURL());
    }

    /** 405 — метод не поддерживается (GET вместо POST и т.п.) */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return error(405, "Method not allowed: " + ex.getMethod());
    }

    /** 400 — невалидный / нечитаемый JSON в теле запроса */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(HttpMessageNotReadableException ex) {
        return error(400, "Malformed JSON request body");
    }

    /** 400 — отсутствует обязательный параметр запроса */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return error(400, "Missing required parameter: " + ex.getParameterName());
    }

    /** 500 — любое непойманное исключение из контроллеров */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return error(500, "Internal server error");
    }

    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status, "error", message));
    }
}
