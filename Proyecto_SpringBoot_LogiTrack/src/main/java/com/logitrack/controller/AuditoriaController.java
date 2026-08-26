package com.logitrack.controller;

import com.logitrack.model.Auditoria;
import com.logitrack.model.TipoOperacion;
import com.logitrack.service.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auditorias")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<?> obtenerTodas(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Pageable pageable) {
        if (page != null || size != null) {
            return ResponseEntity.ok(auditoriaService.obtenerTodas(pageable));
        }
        return ResponseEntity.ok(auditoriaService.obtenerTodas());
    }

    // === NEW: Advanced Search ===
    @GetMapping("/search")
    public ResponseEntity<Page<Auditoria>> buscarAvanzado(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) TipoOperacion operacion,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) Long entidadId,
            Pageable pageable) {
        return ResponseEntity.ok(auditoriaService.buscarAvanzado(
                entidad, operacion, usuarioId, fechaDesde, fechaHasta, entidadId, pageable));
    }

    // === NEW: Por rango de fechas ===
    @GetMapping("/rango-fechas")
    public ResponseEntity<Page<Auditoria>> buscarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            Pageable pageable) {
        return ResponseEntity.ok(auditoriaService.buscarPorRangoFechas(desde, hasta, pageable));
    }

    // === NEW: Por entidadId ===
    @GetMapping("/entidad-id/{entidadId}")
    public ResponseEntity<Page<Auditoria>> buscarPorEntidadId(
            @PathVariable Long entidadId, Pageable pageable) {
        return ResponseEntity.ok(auditoriaService.buscarPorEntidadId(entidadId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Auditoria> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.obtenerPorId(id));
    }

    @GetMapping("/entidad/{entidad}")
    public ResponseEntity<?> buscarPorEntidad(@PathVariable String entidad, Pageable pageable) {
        if (pageable != null && pageable.getPageSize() > 0) {
            return ResponseEntity.ok(auditoriaService.buscarPorEntidad(entidad, pageable));
        }
        return ResponseEntity.ok(auditoriaService.buscarPorEntidad(entidad));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> buscarPorUsuario(@PathVariable Long usuarioId, Pageable pageable) {
        if (pageable != null && pageable.getPageSize() > 0) {
            return ResponseEntity.ok(auditoriaService.buscarPorUsuario(usuarioId, pageable));
        }
        return ResponseEntity.ok(auditoriaService.buscarPorUsuario(usuarioId));
    }

    @GetMapping("/operacion/{tipo}")
    public ResponseEntity<?> buscarPorTipoOperacion(@PathVariable TipoOperacion tipo, Pageable pageable) {
        if (pageable != null && pageable.getPageSize() > 0) {
            return ResponseEntity.ok(auditoriaService.buscarPorTipoOperacion(tipo, pageable));
        }
        return ResponseEntity.ok(auditoriaService.buscarPorTipoOperacion(tipo));
    }
}
