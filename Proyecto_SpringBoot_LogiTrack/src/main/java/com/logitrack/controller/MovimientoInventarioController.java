package com.logitrack.controller;

import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.TipoMovimiento;
import com.logitrack.service.MovimientoInventarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoService;

    public MovimientoInventarioController(MovimientoInventarioService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @GetMapping
    public ResponseEntity<?> obtenerTodos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Pageable pageable) {
        if (page != null || size != null) {
            return ResponseEntity.ok(movimientoService.obtenerTodos(pageable));
        }
        return ResponseEntity.ok(movimientoService.obtenerTodos());
    }

    // === NEW: Advanced Search ===
    @GetMapping("/search")
    public ResponseEntity<Page<MovimientoInventario>> buscarAvanzado(
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) Long bodegaId,
            @RequestParam(required = false) Long bodegaDestinoId,
            @RequestParam(required = false) Long bodegaOrigenId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long productoId,
            Pageable pageable) {
        return ResponseEntity.ok(movimientoService.buscarAvanzado(
                tipo, fechaDesde, fechaHasta, bodegaId, bodegaDestinoId,
                bodegaOrigenId, usuarioId, productoId, pageable));
    }

    // === NEW: Por usuario ===
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Page<MovimientoInventario>> buscarPorUsuario(
            @PathVariable Long usuarioId, Pageable pageable) {
        return ResponseEntity.ok(movimientoService.buscarPorUsuario(usuarioId, pageable));
    }

    // === NEW: Por producto ===
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<MovimientoInventario>> buscarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(movimientoService.buscarPorProducto(productoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimientoInventario> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(movimientoService.obtenerPorId(id));
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<?> buscarPorTipo(@PathVariable TipoMovimiento tipo, Pageable pageable) {
        if (pageable != null && pageable.getPageSize() > 0) {
            return ResponseEntity.ok(movimientoService.buscarPorTipo(tipo, pageable));
        }
        return ResponseEntity.ok(movimientoService.buscarPorTipo(tipo));
    }

    @GetMapping("/rango")
    public ResponseEntity<?> buscarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            Pageable pageable) {
        if (pageable != null && pageable.getPageSize() > 0) {
            return ResponseEntity.ok(movimientoService.buscarPorRangoFechas(desde, hasta, pageable));
        }
        return ResponseEntity.ok(movimientoService.buscarPorRangoFechas(desde, hasta));
    }

    @GetMapping("/bodega/{bodegaId}")
    public ResponseEntity<List<MovimientoInventario>> buscarPorBodega(@PathVariable Long bodegaId) {
        return ResponseEntity.ok(movimientoService.buscarPorBodega(bodegaId));
    }

    @PostMapping
    public ResponseEntity<MovimientoInventario> registrar(@Valid @RequestBody MovimientoInventario movimiento) {
        return new ResponseEntity<>(movimientoService.registrarMovimiento(movimiento), HttpStatus.CREATED);
    }
}
