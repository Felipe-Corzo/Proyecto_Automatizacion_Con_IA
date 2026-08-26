package com.logitrack.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.logitrack.service.TorreControlService;

@SpringBootTest
@AutoConfigureMockMvc
public class OrdenCompraSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TorreControlService torreControlService;

    // PRUEBA 6: Intento de aprobación por rol AGENTE -> 403 Forbidden
    @Test
    @WithMockUser(username = "agente_automatizado", roles = "AGENTE") // Simular usuario con rol AGENTE
    @DisplayName("6. Un usuario con rol AGENTE no tiene autorización para cambiar el estado de una orden (Aprobar) -> 403 Forbidden")
    void testAgenteIntentaAprobarRetornaForbidden() throws Exception {
        // Enviar petición PATCH para cambiar el estado de la orden 1 a APROBADA
        mockMvc.perform(patch("/api/ordenes/1/estado")
            .with(user("agente_automatizado").roles("AGENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"APROBADA\"}"))
                .andExpect(status().isForbidden()); // Validar que retorne el estado HTTP 403 (Forbidden)
    }
}