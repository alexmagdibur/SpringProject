package ru.bmstu.reader;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.bmstu.domain.Creature;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class CreatureCsvReader {

    public List<Creature> read(String resourcePath) {
        List<Creature> creatures = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                String line;
                boolean firstLine = true;
                while ((line = br.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    String abilitiesRaw = parts[6].replace("\"", "");
                    List<String> abilities = Arrays.asList(abilitiesRaw.split(","));
                    creatures.add(new Creature(
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim(),
                            Double.parseDouble(parts[4].trim()),
                            Double.parseDouble(parts[5].trim()),
                            abilities
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read creature CSV: " + resourcePath, e);
        }
        return creatures;
    }
}