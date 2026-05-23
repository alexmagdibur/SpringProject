package ru.bmstu.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.AdoptionService;
import ru.bmstu.service.CreatureService;
import ru.bmstu.service.ShelterStatisticsService;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShelterStatisticsServiceImpl implements ShelterStatisticsService {

    private final CreatureService creatureService;
    private final AdoptionService adoptionService;

    @Override
    public int getTotalCount() {
        return creatureService.findAll().size();
    }

    @Override
    public int getAdoptedCount() {
        return getTotalCount() - adoptionService.findAffordable(Double.MAX_VALUE).size();
    }

    @Override
    public String getMostPopularSpecies() {
        return creatureService.findAll().stream()
                .collect(Collectors.groupingBy(Creature::getSpecies, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }
}
