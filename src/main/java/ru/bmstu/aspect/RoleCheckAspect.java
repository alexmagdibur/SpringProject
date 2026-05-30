package ru.bmstu.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import ru.bmstu.annotation.RequiresRole;
import ru.bmstu.security.ApiKeyStore;

@Aspect
@Component
public class RoleCheckAspect {

    private final ApiKeyStore apiKeyStore;

    public RoleCheckAspect(ApiKeyStore apiKeyStore) {
        this.apiKeyStore = apiKeyStore;
    }

    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint pjp, RequiresRole requiresRole) throws Throwable {
        String requiredRole = requiresRole.value();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        String apiKey = request.getHeader("X-Api-Key");

        String role;
        if (apiKey == null) {
            // без ключа — гость с ролью USER по умолчанию
            role = "USER";
        } else {
            role = apiKeyStore.getRoleByKey(apiKey);
            if (role == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid API key");
            }
        }

        if (!role.equalsIgnoreCase(requiredRole)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: requires role " + requiredRole + ", your role: " + role);
        }

        return pjp.proceed();
    }
}
