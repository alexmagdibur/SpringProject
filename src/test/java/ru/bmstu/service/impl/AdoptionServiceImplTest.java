package ru.bmstu.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.AdoptionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdoptionServiceImplTest {

    private AdoptionService adoptionService;

    @BeforeEach
    void setUp() {
        List<Creature> testData = List.of(
                new Creature("1", "Ember", "Phoenix", "Calm", 5.0, 100.0, List.of("Healing")),
                new Creature("2", "Gloom", "Shadow Wolf", "Fierce", 8.0, 200.0, List.of("Tracking")),
                new Creature("3", "Pip", "Pixie Fox", "Playful", 2.0, 50.0, List.of("Speed"))
        );
        CreatureServiceImpl creatureService = new CreatureServiceImpl(testData);
        adoptionService = new AdoptionServiceImpl(creatureService, 1000.0);
    }

    @Test
    void adopt_successfulAdoption() {
        // monthly 300 / 30 = 10.0 >= 5.0 (Ember's daily cost), adoption cost 100 <= 1000 budget
        String result = adoptionService.adopt("Alice", 300.0, "1");
        assertTrue(result.startsWith("Contract generated!"));
        assertTrue(result.contains("Ember"));
    }

    @Test
    void adopt_insufficientDailyBudget() {
        // monthly 90 / 30 = 3.0 < 5.0 (Ember's daily cost)
        String result = adoptionService.adopt("Bob", 90.0, "1");
        assertTrue(result.startsWith("Adoption denied"));
    }

    @Test
    void adopt_creatureNotFound() {
        String result = adoptionService.adopt("Alice", 300.0, "999");
        assertTrue(result.startsWith("Error: creature not found"));
    }

    @Test
    void adopt_alreadyAdopted() {
        adoptionService.adopt("Alice", 300.0, "1");
        String result = adoptionService.adopt("Bob", 300.0, "1");
        assertTrue(result.contains("already been adopted"));
    }

    @Test
    void findAffordable_returnsCreaturesWithinBudget() {
        // monthly 60 / 30 = 2.0, only Pip (daily 2.0) fits
        List<Creature> affordable = adoptionService.findAffordable(60.0);
        assertEquals(1, affordable.size());
        assertEquals("Pip", affordable.get(0).getName());
    }

    @Test
    void findAffordable_excludesAlreadyAdopted() {
        adoptionService.adopt("Alice", 300.0, "3"); // adopt Pip
        List<Creature> affordable = adoptionService.findAffordable(60.0);
        assertTrue(affordable.stream().noneMatch(c -> c.getId().equals("3")));
    }

    @Test
    void getRemainingBudget_decreasesAfterAdoption() {
        double before = adoptionService.getRemainingBudget();
        adoptionService.adopt("Alice", 300.0, "1"); // Ember costs 100
        assertEquals(before - 100.0, adoptionService.getRemainingBudget(), 0.001);
    }
}
