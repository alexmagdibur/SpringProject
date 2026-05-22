package ru.bmstu.service;

import ru.bmstu.domain.Creature;
import java.util.List;

public interface CreatureService {
    List<Creature> findAll();
    List<Creature> findBySpecies(String species);
    List<Creature> findByTemperament(String temperament);
    List<Creature> findByName(String name);
    Creature findById(String id);
}