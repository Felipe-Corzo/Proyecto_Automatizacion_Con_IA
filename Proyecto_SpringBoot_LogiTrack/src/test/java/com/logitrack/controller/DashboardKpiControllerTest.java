package com.logitrack.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardKpiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/kpis devuelve KPIs consolidados para el agente")
    void getKpisReturnsDashboardMetrics() throws Exception {
        mockMvc.perform(get("/api/kpis")
                .with(user("agente_automatizado").roles("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductos").exists())
                .andExpect(jsonPath("$.productosEnRiesgo").exists())
                .andExpect(jsonPath("$.productosEnQuiebre").exists())
                .andExpect(jsonPath("$.valorTotalInventario").exists())
                .andExpect(jsonPath("$.ocupacionPromedioBodegas").exists());
    }
}
