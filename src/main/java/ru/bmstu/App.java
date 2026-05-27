package ru.bmstu;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.bmstu.config.AppConfig;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.AdoptionService;
import ru.bmstu.service.CreatureService;
import ru.bmstu.service.ShelterStatisticsService;

import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        var context = new AnnotationConfigApplicationContext(AppConfig.class);
        var creatureService = context.getBean(CreatureService.class);
        var adoptionService = context.getBean(AdoptionService.class);
        var statsService = context.getBean(ShelterStatisticsService.class);
        var scanner = new Scanner(System.in);

        System.out.println("=== Magical Creature Shelter ===");
        System.out.printf("Starting budget: %.2f gold%n", adoptionService.getRemainingBudget());
        boolean running = true;
        while (running) {
            System.out.println("\n1. List all creatures");
            System.out.println("2. Filter by species");
            System.out.println("3. Filter by temperament");
            System.out.println("4. Search by name");
            System.out.println("5. Adopt a creature");
            System.out.println("6. Shelter statistics");
            System.out.println("0. Exit");
            System.out.print("Choose: ");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> creatureService.findAll().forEach(App::printCreature);
                case "2" -> {
                    System.out.print("Species: ");
                    creatureService.findBySpecies(scanner.nextLine()).forEach(App::printCreature);
                }
                case "3" -> {
                    System.out.print("Temperament: ");
                    creatureService.findByTemperament(scanner.nextLine()).forEach(App::printCreature);
                }
                case "4" -> {
                    System.out.print("Name: ");
                    creatureService.findByName(scanner.nextLine()).forEach(App::printCreature);
                }
                case "5" -> {
                    System.out.printf("Budget available: %.2f gold%n", adoptionService.getRemainingBudget());
                    System.out.print("Your name: ");
                    String name = scanner.nextLine();
                    System.out.print("Monthly budget (gold): ");
                    double budget = Double.parseDouble(scanner.nextLine());
                    System.out.print("Creature ID: ");
                    String id = scanner.nextLine();
                    System.out.println(adoptionService.adopt(name, budget, id));
                }
                case "6" -> {
                    System.out.println("Total creatures: " + statsService.getTotalCount());
                    System.out.println("Adopted: " + statsService.getAdoptedCount());
                    System.out.println("Most popular species: " + statsService.getMostPopularSpecies());
                }
                case "0" -> running = false;
                default -> System.out.println("Unknown option.");
            }
        }
        context.close();
    }

    private static void printCreature(Creature c) {
        System.out.printf("[%s] %s (%s) | Temp: %s | Daily: %.1f | Adoption: %.1f | %s%n",
                c.getId(), c.getName(), c.getSpecies(), c.getTemperament(),
                c.getDailyCost(), c.getAdoptionCost(), c.getAbilities());
    }
}