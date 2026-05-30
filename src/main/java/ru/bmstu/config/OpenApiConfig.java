package ru.bmstu.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shelterOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Magical Creature Shelter API")
                        .version("v1")
                        .description("REST API for managing magical creature shelter."));
    }
}
