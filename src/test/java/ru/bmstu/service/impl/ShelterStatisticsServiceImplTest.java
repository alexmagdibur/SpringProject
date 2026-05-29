package ru.bmstu.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.ShelterStatisticsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShelterStatisticsServiceImplTest {

    private ShelterStatisticsService statsService;
    private AdoptionServiceImpl adoptionService;

    @BeforeEach
    void setUp() {
        List<Creature> testData = List.of(
                new Creature("1", "Ember", "Phoenix", "Calm", 5.0, 100.0, List.of()),
                new Creature("2", "Pip", "Phoenix", "Playful", 2.0, 50.0, List.of()),
                new Creature("3", "Gloom", "Shadow Wolf", "Fierce", 8.0, 200.0, List.of())
        );
        CreatureServiceImpl creatureService = new CreatureServiceImpl(testData);
        adoptionService = new AdoptionServiceImpl(creatureService, 1000.0);
        statsService = new ShelterStatisticsServiceImpl(creatureService, adoptionService);
    }

    @Test
    void getTotalCount_returnsCorrectCount() {
        assertEquals(3, statsService.getTotalCount());
    }

    @Test
    void getAdoptedCount_initiallyZero() {
        assertEquals(0, statsService.getAdoptedCount());
    }

    @Test
    void getAdoptedCount_incrementsAfterAdoption() {
        adoptionService.adopt("Alice", 300.0, "1");
        assertEquals(1, statsService.getAdoptedCount());
    }

    @Test
    void getMostPopularSpecies_returnsCorrectSpecies() {
        // Phoenix appears twice, Shadow Wolf once
        assertEquals("Phoenix", statsService.getMostPopularSpecies());
    }

    @Test
    void getMostPopularSpecies_noCreatures_returnsNA() {
        CreatureServiceImpl emptyService = new CreatureServiceImpl(List.of());
        AdoptionServiceImpl emptyAdoption = new AdoptionServiceImpl(emptyService, 0.0);
        ShelterStatisticsService emptyStats = new ShelterStatisticsServiceImpl(emptyService, emptyAdoption);
        assertEquals("N/A", emptyStats.getMostPopularSpecies());
    }
}
