package com.logitrack.controller;

import com.logitrack.dto.ResumenPanelDTO;
import com.logitrack.model.ResumenPanel;
import com.logitrack.service.ResumenPanelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public record DailySummaryRequest(String resumenEjecutivo, String alertasCriticas,
                                      String recomendacionesAgente) {
    }
}