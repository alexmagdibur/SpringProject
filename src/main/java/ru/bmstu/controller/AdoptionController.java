package ru.bmstu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.AdoptionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/adoptions")
public class AdoptionController {

    private final AdoptionService adoptionService;

    public AdoptionController(AdoptionService adoptionService) {
        this.adoptionService = adoptionService;
    }

    @PostMapping
    public ResponseEntity<?> adopt(@RequestBody AdoptionRequest request) {
        String result = adoptionService.adopt(
                request.getVisitorName(),
                request.getMonthlyBudget(),
                request.getCreatureId()
        );
        if (result.startsWith("Error: creature not found")) {
            return ResponseEntity.notFound().build();
        }
        if (result.startsWith("Error") || result.startsWith("Adoption denied")) {
            List<Creature> alternatives = adoptionService.findAffordable(request.getMonthlyBudget());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", result,
                    "alternatives", alternatives
            ));
        }
        return ResponseEntity.ok(Map.of("contract", result));
    }
}
