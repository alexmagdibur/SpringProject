package ru.bmstu.security;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ApiKeyStore {

    private record UserRecord(String password, String apiKey) {}

    /** username → { password, apiKey } */
    private static final Map<String, UserRecord> USERS = Map.of(
            "admin", new UserRecord("admin123", "admin-key-12345"),
            "user",  new UserRecord("user123",  "user-key-67890")
    );

    /** apiKey → role */
    private static final Map<String, String> KEYS = Map.of(
            "admin-key-12345", "ADMIN",
            "user-key-67890",  "USER"
    );

    /**
     * Возвращает роль по API-ключу, или null если ключ неизвестен.
     */
    public String getRoleByKey(String apiKey) {
        if (apiKey == null) return null;
        return KEYS.get(apiKey);
    }

    /**
     * Возвращает API-ключ по логину/паролю, или null при неверных данных.
     */
    public String findApiKeyByCredentials(String username, String password) {
        if (username == null || password == null) return null;
        UserRecord user = USERS.get(username);
        if (user == null || !user.password().equals(password)) return null;
        return user.apiKey();
    }
}
