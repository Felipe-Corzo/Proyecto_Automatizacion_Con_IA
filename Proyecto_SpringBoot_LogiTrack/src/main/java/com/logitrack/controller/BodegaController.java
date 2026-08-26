package com.logitrack.controller;

import com.logitrack.dto.StockPorBodegaDTO;
import com.logitrack.dto.ValorInventarioBodegaDTO;
import com.logitrack.model.Bodega;
import com.logitrack.model.InventarioBodega;
import com.logitrack.service.BodegaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bodegas")
public class BodegaController {

    private final BodegaService bodegaService;

    public BodegaController(BodegaService bodegaService) {
        this.bodegaService = bodegaService;
    }

    @GetMapping
    public ResponseEntity<?> obtenerTodas(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Pageable pageable) {
        if (page != null || size != null) {
            return ResponseEntity.ok(bodegaService.obtenerTodas(pageable));
        }
        return ResponseEntity.ok(bodegaService.obtenerTodas());
    }

    // === NEW: Advanced Search ===
    @GetMapping("/search")
    public ResponseEntity<Page<Bodega>> buscarAvanzado(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) Boolean sinEncargado,
            @RequestParam(required = false) Long capacidadMin,
            @RequestParam(required = false) Long capacidadMax,
            Pageable pageable) {
        return ResponseEntity.ok(bodegaService.buscarAvanzado(
                nombre, ubicacion, sinEncargado, capacidadMin, capacidadMax, pageable));
    }

    // === NEW: Por ubicación ===
    @GetMapping("/ubicacion")
    public ResponseEntity<Page<Bodega>> buscarPorUbicacion(
            @RequestParam String ubicacion, Pageable pageable) {
        return ResponseEntity.ok(bodegaService.buscarPorUbicacion(ubicacion, pageable));
    }

    // === NEW: Sin encargado ===
    @GetMapping("/sin-encargado")
    public ResponseEntity<Page<Bodega>> buscarSinEncargado(Pageable pageable) {
        return ResponseEntity.ok(bodegaService.buscarSinEncargado(pageable));
    }

    // === NEW: Por capacidad mínima ===
    @GetMapping("/capacidad-min")
    public ResponseEntity<Page<Bodega>> buscarPorCapacidadMinima(
            @RequestParam Integer capacidad, Pageable pageable) {
        return ResponseEntity.ok(bodegaService.buscarPorCapacidadMinima(capacidad, pageable));
    }

    // === NEW: Por capacidad máxima ===
    @GetMapping("/capacidad-max")
    public ResponseEntity<Page<Bodega>> buscarPorCapacidadMaxima(
            @RequestParam Integer capacidad, Pageable pageable) {
        return ResponseEntity.ok(bodegaService.buscarPorCapacidadMaxima(capacidad, pageable));
    }

    // === NEW: Valor del inventario por bodega ===
    @GetMapping("/valor-inventario")
    public ResponseEntity<List<ValorInventarioBodegaDTO>> obtenerValorInventario() {
        return ResponseEntity.ok(bodegaService.obtenerValorInventarioTodas());
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockPorBodegaDTO>> obtenerStockDeTodas() {
        return ResponseEntity.ok(bodegaService.obtenerStockTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bodega> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bodegaService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<?> buscarPorNombre(@RequestParam String nombre, Pageable pageable) {
        if (pageable != null && pageable.getPageSize() > 0) {
            return ResponseEntity.ok(bodegaService.buscarPorNombre(nombre, pageable));
        }
        return ResponseEntity.ok(bodegaService.buscarPorNombre(nombre));
    }

    @PostMapping
    public ResponseEntity<Bodega> crear(@Valid @RequestBody Bodega bodega) {
        return new ResponseEntity<>(bodegaService.guardar(bodega), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bodega> actualizar(@PathVariable Long id, @Valid @RequestBody Bodega bodega) {
        return ResponseEntity.ok(bodegaService.actualizar(id, bodega));
    }

    @GetMapping("/{id}/inventario")
    public ResponseEntity<List<InventarioBodega>> obtenerInventario(@PathVariable Long id) {
        return ResponseEntity.ok(bodegaService.obtenerInventarioPorBodega(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        bodegaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
