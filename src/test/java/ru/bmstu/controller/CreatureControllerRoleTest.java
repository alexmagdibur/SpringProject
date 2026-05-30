package ru.bmstu.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.bmstu.config.AppConfig;
import ru.bmstu.config.WebConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AppConfig.class, WebConfig.class})
class CreatureControllerRoleTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void getAllAdmin_returns403WhenKeyIsUser() throws Exception {
        mockMvc.perform(get("/api/v1/creatures/all").header("X-Api-Key", "user-key-67890"))
               .andExpect(status().isForbidden());
    }

    @Test
    void getAllAdmin_returns200WhenKeyIsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/creatures/all").header("X-Api-Key", "admin-key-12345"))
               .andExpect(status().isOk());
    }

    @Test
    void audit_returns403WhenKeyIsUser() throws Exception {
        mockMvc.perform(get("/api/v1/audit").header("X-Api-Key", "user-key-67890"))
               .andExpect(status().isForbidden());
    }

    @Test
    void getAllAdmin_returns403WhenNoKeyProvided() throws Exception {
        // без ключа роль USER по умолчанию → ADMIN endpoint → 403
        mockMvc.perform(get("/api/v1/creatures/all"))
               .andExpect(status().isForbidden());
    }

    @Test
    void getAllAdmin_returns401WhenKeyIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/creatures/all").header("X-Api-Key", "wrong-key"))
               .andExpect(status().isUnauthorized());
    }
}
