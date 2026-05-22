package ru.bmstu.service;

public interface ShelterStatisticsService {
    int getTotalCount();
    int getAdoptedCount();
    String getMostPopularSpecies();
}