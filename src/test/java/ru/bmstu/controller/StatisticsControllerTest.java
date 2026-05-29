package ru.bmstu.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bmstu.service.AuditLogService;
import ru.bmstu.service.ShelterStatisticsService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsControllerTest {

    private MockMvc mockMvc;
    private ShelterStatisticsService statsService;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        statsService = Mockito.mock(ShelterStatisticsService.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        StatisticsController controller = new StatisticsController(statsService, auditLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void statistics_returns200() throws Exception {
        when(statsService.getTotalCount()).thenReturn(5);
        when(statsService.getAdoptedCount()).thenReturn(2);
        when(statsService.getMostPopularSpecies()).thenReturn("Phoenix");

        mockMvc.perform(get("/api/v1/statistics").accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.total").value(5))
               .andExpect(jsonPath("$.adopted").value(2))
               .andExpect(jsonPath("$.mostPopularSpecies").value("Phoenix"));
    }

    @Test
    void auditLog_returns200() throws Exception {
        when(auditLogService.getLog()).thenReturn(List.of("[AUDIT] entry1", "[AUDIT] entry2"));

        mockMvc.perform(get("/api/v1/audit").accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0]").value("[AUDIT] entry1"));
    }
}
