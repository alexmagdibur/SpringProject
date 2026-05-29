package ru.bmstu.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.AdoptionService;
import ru.bmstu.service.CreatureService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreatureControllerTest {

    private MockMvc mockMvc;
    private CreatureService creatureService;
    private AdoptionService adoptionService;

    @BeforeEach
    void setUp() {
        creatureService = Mockito.mock(CreatureService.class);
        adoptionService = Mockito.mock(AdoptionService.class);
        CreatureController controller = new CreatureController(creatureService, adoptionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void getAll_returns200() throws Exception {
        List<Creature> creatures = List.of(
                new Creature("1", "Ember", "Phoenix", "Calm", 5.0, 100.0, List.of("Healing"))
        );
        when(adoptionService.findAffordable(Double.MAX_VALUE)).thenReturn(creatures);

        mockMvc.perform(get("/api/v1/creatures").accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].name").value("Ember"));
    }

    @Test
    void getById_returns200() throws Exception {
        Creature creature = new Creature("1", "Ember", "Phoenix", "Calm", 5.0, 100.0, List.of());
        when(creatureService.findById("1")).thenReturn(creature);

        mockMvc.perform(get("/api/v1/creatures/1").accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Ember"))
               .andExpect(jsonPath("$.species").value("Phoenix"));
    }

    @Test
    void getById_unknown_returns404() throws Exception {
        when(creatureService.findById("999")).thenReturn(null);

        mockMvc.perform(get("/api/v1/creatures/999"))
               .andExpect(status().isNotFound());
    }

    @Test
    void searchByName_returns200() throws Exception {
        List<Creature> result = List.of(
                new Creature("1", "Ember", "Phoenix", "Calm", 5.0, 100.0, List.of())
        );
        when(creatureService.findByName("ember")).thenReturn(result);

        mockMvc.perform(get("/api/v1/creatures/search").param("name", "ember")
                       .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].name").value("Ember"));
    }

    @Test
    void filterBySpecies_returns200() throws Exception {
        List<Creature> result = List.of(
                new Creature("1", "Ember", "Phoenix", "Calm", 5.0, 100.0, List.of())
        );
        when(creatureService.findBySpecies("Phoenix")).thenReturn(result);

        mockMvc.perform(get("/api/v1/creatures/filter").param("species", "Phoenix")
                       .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].species").value("Phoenix"));
    }
}
