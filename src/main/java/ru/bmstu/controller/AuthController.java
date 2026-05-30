package ru.bmstu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bmstu.security.ApiKeyStore;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ApiKeyStore apiKeyStore;

    public AuthController(ApiKeyStore apiKeyStore) {
        this.apiKeyStore = apiKeyStore;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String apiKey = apiKeyStore.findApiKeyByCredentials(
                request.getUsername(), request.getPassword());

        if (apiKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }

        String role = apiKeyStore.getRoleByKey(apiKey);
        return ResponseEntity.ok(Map.of(
                "apiKey", apiKey,
                "role",   role
        ));
    }
}
