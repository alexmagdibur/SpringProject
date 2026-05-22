package ru.bmstu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import ru.bmstu.domain.Creature;
import ru.bmstu.reader.CreatureCsvReader;

import java.util.List;

@Configuration
@ComponentScan("ru.bmstu")
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Value("${shelter.csv.path}")
    private String csvPath;

    @Bean
    public List<Creature> creatures(CreatureCsvReader reader) {
        return reader.read(csvPath);
    }
}