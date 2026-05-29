package ru.bmstu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bmstu.annotation.RequiresRole;
import ru.bmstu.service.AuditLogService;
import ru.bmstu.service.ShelterStatisticsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class StatisticsController {

    private final ShelterStatisticsService statsService;
    private final AuditLogService auditLogService;

    public StatisticsController(ShelterStatisticsService statsService, AuditLogService auditLogService) {
        this.statsService = statsService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(Map.of(
                "total", statsService.getTotalCount(),
                "adopted", statsService.getAdoptedCount(),
                "mostPopularSpecies", statsService.getMostPopularSpecies()
        ));
    }

    @GetMapping("/audit")
    @RequiresRole("ADMIN")
    public ResponseEntity<List<String>> getAuditLog() {
        return ResponseEntity.ok(auditLogService.getLog());
    }
}
