package ru.bmstu.service;

import java.util.List;

public interface AuditLogService {
    void log(String entry);
    List<String> getLog();
}
