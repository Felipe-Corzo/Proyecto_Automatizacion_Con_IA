package com.logitrack.controller;

import com.logitrack.dto.ResumenPanelDTO;
import com.logitrack.model.ResumenPanel;
import com.logitrack.service.ResumenPanelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/resumenes-panel")
public class ResumenPanelController {

    private final ResumenPanelService resumenPanelService;

    public ResumenPanelController(ResumenPanelService resumenPanelService) {
        this.resumenPanelService = resumenPanelService;
    }

    @PostMapping
    public ResponseEntity<ResumenPanel> publicar(@RequestBody DailySummaryRequest request) {
        ResumenPanelDTO dto = new ResumenPanelDTO();
        dto.setFecha(LocalDate.now().toString());
        dto.setNarrativa(request.resumenEjecutivo());
        dto.setAlertas(buildAlertas(request.alertasCriticas()));
        dto.setAccionesSugeridas(buildAcciones(request.recomendacionesAgente()));

        ResumenPanel resumen = resumenPanelService.publicarResumen(dto);
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "system";
        resumen.setAutor(username);
        return ResponseEntity.ok(resumen);
    }

    @GetMapping("/ultimo") // Consulta del último resumen diario para el Dashboard de la Torre de Control
    public ResponseEntity<ResumenPanel> obtenerUltimo() {
        return ResponseEntity.ok(resumenPanelService.obtenerUltimoResumen());
    }

    private List<ResumenPanelDTO.Alerta> buildAlertas(String alertasCriticas) {
        List<ResumenPanelDTO.Alerta> alertas = new ArrayList<>();
        if (alertasCriticas == null || alertasCriticas.isBlank()) {
            return alertas;
        }

        ResumenPanelDTO.Alerta alerta = new ResumenPanelDTO.Alerta();
        alerta.setSeveridad("ALTA");
        alerta.setTitulo("ALERTA_CRITICA");
        alerta.setDetalle(alertasCriticas);
        alertas.add(alerta);
        return alertas;
    }

    private List<ResumenPanelDTO.AccionSugerida> buildAcciones(String recomendacionesAgente) {
        List<ResumenPanelDTO.AccionSugerida> acciones = new ArrayList<>();
        if (recomendacionesAgente == null || recomendacionesAgente.isBlank()) {
            return acciones;
        }

        ResumenPanelDTO.AccionSugerida accion = new ResumenPanelDTO.AccionSugerida();
        accion.setTipo("RECOMENDACION");
        accion.setDescripcion(recomendacionesAgente);
        acciones.add(accion);
        return acciones;
    }

    public record DailySummaryRequest(String resumenEjecutivo, String alertasCriticas,
                                      String recomendacionesAgente) {
    }
}