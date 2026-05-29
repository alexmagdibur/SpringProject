package ru.bmstu.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import ru.bmstu.service.AuditLogService;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    public AuditLogAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Before("execution(public * ru.bmstu.service.impl.*.*(..)) " +
            "&& !within(ru.bmstu.service.impl.AuditLogServiceImpl)")
    public void audit(JoinPoint joinPoint) {
        String timestamp = LocalDateTime.now().toString();
        String signature = joinPoint.getSignature().toShortString();
        String args = Arrays.toString(joinPoint.getArgs());
        String entry = "[AUDIT] " + timestamp + " | " + signature + " | " + args;
        System.out.println(entry);
        auditLogService.log(entry);
    }
}
