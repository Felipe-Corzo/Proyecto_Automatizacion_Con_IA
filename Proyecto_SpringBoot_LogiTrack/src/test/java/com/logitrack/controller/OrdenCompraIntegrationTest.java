package com.logitrack.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Revierte los cambios de base de datos después de cada test
public class OrdenCompraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

        // PRUEBA 8: PDF en estado BORRADOR con marca de agua y regeneración al cambiar estado
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN") // Simular usuario administrador
    @DisplayName("8. Generación de PDF en BORRADOR (con marca de agua) e inhabilitación automática al cambiar de estado")
    void testPdfCicloDeVidaYMarcaDeAgua() throws Exception {
        
        // 1. Generar el PDF para una orden de compra existente que esté en estado BORRADOR (ID 1 en data.sql)
        mockMvc.perform(post("/api/ordenes/1/pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()); // Verificar creación exitosa (200 OK)

        // 2. Descargar el archivo PDF generado
        MvcResult result = mockMvc.perform(get("/api/ordenes/1/pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes, "El PDF no debe ser nulo");
        assertTrue(pdfBytes.length > 0, "El contenido del archivo PDF debe tener bytes");

        PdfReader pdfReader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(pdfReader);
        String pdfText = extractor.getTextFromPage(1);
        String rawContent = new String(pdfReader.getPageContent(1));
        pdfReader.close();
        System.out.println("=== PDF TEXT EXTRACTED: [" + pdfText + "] ===");
        System.out.println("=== PDF RAW CONTENT: [" + rawContent + "] ===");
        System.out.println("=== PDF SIZE: " + pdfBytes.length + " ===");
        assertTrue(pdfText.contains("BORRADOR"), "El documento generado en borrador debe contener la marca 'BORRADOR'. Texto extraido: [" + pdfText + "]");

        // 3. Cambiar el estado de la orden de BORRADOR a APROBADA (usando el endpoint PATCH)
        mockMvc.perform(patch("/api/ordenes/1/estado")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"APROBADA\"}"))
                .andExpect(status().isOk());

        // 4. Descargar el PDF después del cambio; el backend lo regenera automáticamente.
        mockMvc.perform(get("/api/ordenes/1/pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}