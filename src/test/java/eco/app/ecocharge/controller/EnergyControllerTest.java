package eco.app.ecocharge.controller;

import eco.app.ecocharge.model.response.ChargingWindowResponse;
import eco.app.ecocharge.model.response.DailyEnergyMix;
import eco.app.ecocharge.service.EnergyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnergyController.class)
class EnergyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnergyService energyService;

    @Test
    void shouldReturnEnergyMix() throws Exception {

        DailyEnergyMix mockMix = DailyEnergyMix.builder()
                .date("2025-01-01")
                .cleanEnergyPerc(45.5)
                .build();

        when(energyService.getAggregatedEnergyMix()).thenReturn(List.of(mockMix));


        mockMvc.perform(get("/api/energy/mix"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].date").value("2025-01-01"))
                .andExpect(jsonPath("$[0].cleanEnergyPerc").value(45.5));
    }

    @Test
    void shouldReturnOptimalChargingWindow() throws Exception {

        ChargingWindowResponse mockResponse = ChargingWindowResponse.builder()
                .startTime(LocalDateTime.of(2025, 1, 1, 12, 0))
                .endTime(LocalDateTime.of(2025, 1, 1, 14, 0))
                .cleanEnergyPerc(80.0)
                .build();

        when(energyService.findOptimalChargingWindow(anyInt())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/energy/optimal-charging").param("hours", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleanEnergyPerc").value(80.0));
    }
}