package ru.bmstu.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class Creature {
    private final String id;
    private String name;
    private String species;
    private String temperament;
    private double dailyCost;       // в золотых
    private double adoptionCost;
    private List<String> abilities;
}