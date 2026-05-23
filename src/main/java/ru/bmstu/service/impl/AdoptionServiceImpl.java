package ru.bmstu.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.AdoptionService;
import ru.bmstu.service.CreatureService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdoptionServiceImpl implements AdoptionService {

    private final CreatureService creatureService;
    private final Set<String> adoptedIds = new HashSet<>();

    @Override
    public String adopt(String visitorName, double monthlyBudget, String creatureId) {
        Creature creature = creatureService.findById(creatureId);
        if (creature == null) {
            return "Error: creature not found with id " + creatureId;
        }
        if (adoptedIds.contains(creatureId)) {
            return "Error: this creature has already been adopted.";
        }
        double dailyBudget = monthlyBudget / 30.0;
        if (dailyBudget < creature.getDailyCost()) {
            return String.format(
                    "Adoption denied. Daily budget %.2f < required %.2f gold. " +
                            "Consider these alternatives: %s",
                    dailyBudget,
                    creature.getDailyCost(),
                    findAffordable(monthlyBudget).stream().map(Creature::getName).toList()
            );
        }
        adoptedIds.add(creatureId);
        return String.format(
                "Contract generated! %s has bonded with %s (ID: %s). Adoption cost: %.2f gold.",
                visitorName, creature.getName(), creatureId, creature.getAdoptionCost()
        );
    }

    @Override
    public List<Creature> findAffordable(double monthlyBudget) {
        double dailyBudget = monthlyBudget / 30.0;
        return creatureService.findAll().stream()
                .filter(c -> c.getDailyCost() <= dailyBudget && !adoptedIds.contains(c.getId()))
                .toList();
    }
}
