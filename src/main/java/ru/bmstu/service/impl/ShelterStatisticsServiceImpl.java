package ru.bmstu.service.impl;

import org.springframework.stereotype.Service;
import ru.bmstu.service.AdoptionService;
import ru.bmstu.service.CreatureService;
import ru.bmstu.service.ShelterStatisticsService;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShelterStatisticsServiceImpl implements ShelterStatisticsService {

    private final CreatureService creatureService;
    private final AdoptionService adoptionService;

    public ShelterStatisticsServiceImpl(CreatureService creatureService,
                                        AdoptionService adoptionService) {
        this.creatureService = creatureService;
        this.adoptionService = adoptionService;
    }

    @Override
    public int getTotalCount() {
        return creatureService.findAll().size();
    }

    @Override
    public int getAdoptedCount() {
        // Считаем тех, кого уже нет в списке доступных (бюджет 0 = только уже принятые)
        return getTotalCount() - adoptionService.findAffordable(Double.MAX_VALUE).size();
    }

    @Override
    public String getMostPopularSpecies() {
        return creatureService.findAll().stream()
                .collect(Collectors.groupingBy(c -> c.getSpecies(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }
}