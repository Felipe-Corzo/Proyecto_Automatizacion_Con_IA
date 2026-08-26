package com.logitrack.controller;

import com.logitrack.model.OrdenCompra;
import com.logitrack.model.EstadoOrden;
import com.logitrack.service.TorreControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenCompraController {

    @Autowired
    private TorreControlService service;

    @PostMapping // El rol AGENTE y ADMIN pueden crear órdenes en BORRADOR [10]
    public ResponseEntity<OrdenCompra> crearOrden(@RequestBody OrdenCompra orden) {
        return ResponseEntity.ok(service.crearOrdenCompra(orden));
    }

    @PatchMapping("/{id}/estado") // Restringido: Solo el ADMIN puede autorizar o recibir transiciones [10]
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrdenCompra> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        EstadoOrden nuevoEstado = EstadoOrden.valueOf(body.get("estado"));
        return ResponseEntity.ok(service.cambiarEstadoOrden(id, nuevoEstado));
    }

    @PostMapping("/{id}/pdf") // Solo ADMIN genera el PDF [10]
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> generarPdf(@PathVariable Long id) {
        service.generarPdfOrden(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/pdf") // ADMIN y EMPLEADO descargan y previsualizan el archivo PDF [9, 10]
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        byte[] pdfData = service.obtenerPdfOrden(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "orden_" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfData);
    }
}