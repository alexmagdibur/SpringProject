package ru.bmstu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
public class StatusController {

    @GetMapping("/")
    public RedirectView root() {
        return new RedirectView("/swagger-ui/index.html");
    }

    @GetMapping("/api/v1/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Magical Creature Shelter"
        ));
    }
}
