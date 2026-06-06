package ru.bmstu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Принимает ошибки, которые Tomcat форвардит на /error
 * (ошибки до Spring: фильтры, инициализация контекста).
 * Обычные ошибки REST перехватывает GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/error")
public class ErrorController {

    @RequestMapping
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object statusAttr = request.getAttribute("jakarta.servlet.error.status_code");
        int status = statusAttr instanceof Integer s ? s : 500;
        return ResponseEntity.status(status)
                .body(Map.of("status", status, "error", "Unexpected server error"));
    }
}
