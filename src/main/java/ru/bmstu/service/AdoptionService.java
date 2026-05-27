package ru.bmstu.service;

import ru.bmstu.domain.Creature;
import java.util.List;

public interface AdoptionService {
    String adopt(String visitorName, double monthlyBudget, String creatureId);
    List<Creature> findAffordable(double monthlyBudget);
    double getRemainingBudget();
}