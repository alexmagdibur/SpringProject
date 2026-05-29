package ru.bmstu.service.impl;

import org.springframework.stereotype.Service;
import ru.bmstu.service.AuditLogService;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final List<String> log = new ArrayList<>();

    @Override
    public void log(String entry) {
        log.add(entry);
    }

    @Override
    public List<String> getLog() {
        return List.copyOf(log);
    }
}
