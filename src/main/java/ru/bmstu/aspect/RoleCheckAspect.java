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

@Aspect
@Component
public class RoleCheckAspect {

    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint pjp, RequiresRole requiresRole) throws Throwable {
        String requiredRole = requiresRole.value();
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String role = request.getHeader("X-Role");
        if (role == null) {
            role = "USER";
        }
        if (!role.equalsIgnoreCase(requiredRole)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied: requires role " + requiredRole);
        }
        return pjp.proceed();
    }
}
