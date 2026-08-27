package com.logitrack.controller;

import com.logitrack.dto.ResumenPanelDTO;
import com.logitrack.model.ResumenPanel;
import com.logitrack.service.ResumenPanelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;

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
        dto.setAlertas(new ArrayList<>());
        dto.setAccionesSugeridas(new ArrayList<>());
        return ResponseEntity.ok(resumenPanelService.publicarResumen(dto));
    }

    @GetMapping("/ultimo") // Consulta del último resumen diario para el Dashboard de la Torre de Control
    public ResponseEntity<ResumenPanel> obtenerUltimo() {
        return ResponseEntity.ok(resumenPanelService.obtenerUltimoResumen());
    }

    public record DailySummaryRequest(String resumenEjecutivo, String alertasCriticas,
                                      String recomendacionesAgente) {
    }
}