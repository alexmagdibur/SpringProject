package ru.bmstu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.AdoptionService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdoptionControllerTest {

    private MockMvc mockMvc;
    private AdoptionService adoptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adoptionService = Mockito.mock(AdoptionService.class);
        AdoptionController controller = new AdoptionController(adoptionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void adopt_validAdoption_returns200() throws Exception {
        when(adoptionService.adopt("Alice", 300.0, "1"))
                .thenReturn("Contract generated! Alice has bonded with Ember (ID: 1). Adoption cost: 100.00 gold.");

        AdoptionRequest request = new AdoptionRequest("Alice", 300.0, "1");
        mockMvc.perform(post("/api/v1/adoptions")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.contract").exists());
    }

    @Test
    void adopt_insufficientBudget_returns400() throws Exception {
        when(adoptionService.adopt("Bob", 50.0, "2"))
                .thenReturn("Adoption denied. Daily budget 1.67 < required 8.00 gold. Consider these alternatives: []");
        when(adoptionService.findAffordable(50.0)).thenReturn(List.of());

        AdoptionRequest request = new AdoptionRequest("Bob", 50.0, "2");
        mockMvc.perform(post("/api/v1/adoptions")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void adopt_creatureNotFound_returns404() throws Exception {
        when(adoptionService.adopt(anyString(), anyDouble(), anyString()))
                .thenReturn("Error: creature not found with id 999");

        AdoptionRequest request = new AdoptionRequest("Alice", 300.0, "999");
        mockMvc.perform(post("/api/v1/adoptions")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isNotFound());
    }

    @Test
    void adopt_alreadyAdopted_returns400() throws Exception {
        when(adoptionService.adopt("Alice", 300.0, "1"))
                .thenReturn("Error: this creature has already been adopted.");
        when(adoptionService.findAffordable(300.0)).thenReturn(List.of(
                new Creature("3", "Pip", "Pixie Fox", "Playful", 2.0, 50.0, List.of())
        ));

        AdoptionRequest request = new AdoptionRequest("Alice", 300.0, "1");
        mockMvc.perform(post("/api/v1/adoptions")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadRequest());
    }
}
