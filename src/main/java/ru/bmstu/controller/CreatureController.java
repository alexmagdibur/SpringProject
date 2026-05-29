package ru.bmstu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.bmstu.annotation.RequiresRole;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.AdoptionService;
import ru.bmstu.service.CreatureService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/creatures")
public class CreatureController {

    private final CreatureService creatureService;
    private final AdoptionService adoptionService;

    public CreatureController(CreatureService creatureService, AdoptionService adoptionService) {
        this.creatureService = creatureService;
        this.adoptionService = adoptionService;
    }

    @GetMapping
    public ResponseEntity<List<Creature>> getAvailable() {
        return ResponseEntity.ok(adoptionService.findAffordable(Double.MAX_VALUE));
    }

    @GetMapping("/all")
    @RequiresRole("ADMIN")
    public ResponseEntity<List<Creature>> getAll() {
        return ResponseEntity.ok(creatureService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Creature> getById(@PathVariable("id") String id) {
        Creature creature = creatureService.findById(id);
        if (creature == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(creature);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Creature>> searchByName(@RequestParam("name") String name) {
        return ResponseEntity.ok(creatureService.findByName(name));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Creature>> filter(
            @RequestParam(name = "species", required = false) String species,
            @RequestParam(name = "temperament", required = false) String temperament) {
        if (species != null) {
            return ResponseEntity.ok(creatureService.findBySpecies(species));
        }
        if (temperament != null) {
            return ResponseEntity.ok(creatureService.findByTemperament(temperament));
        }
        return ResponseEntity.ok(creatureService.findAll());
    }
}
