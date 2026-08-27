package com.logitrack.controller;

import com.logitrack.model.InventarioBodega;
import com.logitrack.repository.InventarioBodegaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventarios")
public class InventarioController {

    private final InventarioBodegaRepository inventarioBodegaRepository;

    public InventarioController(InventarioBodegaRepository inventarioBodegaRepository) {
        this.inventarioBodegaRepository = inventarioBodegaRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<List<InventarioBodega>> obtenerEstado(
            @RequestParam(required = false) Long bodegaId) {
        if (bodegaId == null) {
            return ResponseEntity.ok(inventarioBodegaRepository.findAll());
        }
        return ResponseEntity.ok(inventarioBodegaRepository.findByBodegaId(bodegaId));
    }
}