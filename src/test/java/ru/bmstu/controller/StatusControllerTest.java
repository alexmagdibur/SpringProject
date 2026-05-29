package ru.bmstu.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatusControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StatusController controller = new StatusController();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void status_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
               .andExpect(status().isOk());
    }

    @Test
    void status_bodyContainsUp() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
               .andExpect(jsonPath("$.status").value("UP"))
               .andExpect(jsonPath("$.service").value("Magical Creature Shelter"));
    }
}
