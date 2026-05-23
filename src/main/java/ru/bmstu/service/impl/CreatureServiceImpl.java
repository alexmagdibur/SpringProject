package ru.bmstu.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.CreatureService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreatureServiceImpl implements CreatureService {

    private final List<Creature> creatures;

    @Override
    public List<Creature> findAll() {
        return List.copyOf(creatures);
    }

    @Override
    public List<Creature> findBySpecies(String species) {
        return creatures.stream()
                .filter(c -> c.getSpecies().equalsIgnoreCase(species))
                .toList();
    }

    @Override
    public List<Creature> findByTemperament(String temperament) {
        return creatures.stream()
                .filter(c -> c.getTemperament().equalsIgnoreCase(temperament))
                .toList();
    }

    @Override
    public List<Creature> findByName(String name) {
        return creatures.stream()
                .filter(c -> c.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    @Override
    public Creature findById(String id) {
        return creatures.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
