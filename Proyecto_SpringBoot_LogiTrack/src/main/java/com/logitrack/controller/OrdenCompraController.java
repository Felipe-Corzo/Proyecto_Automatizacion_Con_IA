package com.logitrack.controller;

import com.logitrack.model.OrdenCompra;
import com.logitrack.model.EstadoOrden;
import com.logitrack.model.Bodega;
import com.logitrack.model.Producto;
import com.logitrack.model.Proveedor;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ProveedorRepository;
import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.service.TorreControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenCompraController {

    @Autowired
    private TorreControlService service;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private BodegaRepository bodegaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private com.logitrack.repository.OrdenCompraRepository ordenCompraRepository;

    @GetMapping // Listado de órdenes para el módulo frontend de la Torre de Control
    public ResponseEntity<List<OrdenCompra>> listar(@RequestParam(required = false) EstadoOrden estado) {
        List<OrdenCompra> ordenes = (estado != null)
                ? ordenCompraRepository.findByEstado(estado)
                : ordenCompraRepository.findAll();
        return ResponseEntity.ok(ordenes);
    }

    @GetMapping("/{id}") // Detalle de una orden para el frontend
    public ResponseEntity<OrdenCompra> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraRepository.findById(id)
                .orElseThrow(() -> new com.logitrack.exception.ResourceNotFoundException("Orden de compra no encontrada")));
    }

    @PostMapping // El rol AGENTE y ADMIN pueden crear órdenes en BORRADOR [10]
    public ResponseEntity<OrdenCompra> crearOrden(@RequestBody DraftOrderRequest request) {
        if (request == null || request.proveedorId() == null || request.bodegaDestinoId() == null) {
            throw new BadRequestException("proveedorId y bodegaDestinoId son obligatorios.");
        }
        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new BadRequestException("La orden debe contener al menos una línea de detalle.");
        }
        if (request.detalles().size() > 1) {
            throw new BadRequestException("El backend actual admite una sola línea por orden de compra.");
        }
        DraftOrderDetail detail = request.detalles().get(0);
        if (detail == null || detail.productoId() == null || detail.cantidad() == null
                || detail.precioUnitario() == null) {
            throw new BadRequestException("Cada línea requiere productoId, cantidad y precioUnitario.");
        }
        Producto producto = productoRepository.findById(detail.productoId())
                .orElseThrow(() -> new BadRequestException("El producto indicado no existe."));
        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> new BadRequestException("El proveedor indicado no existe."));
        Bodega bodega = bodegaRepository.findById(request.bodegaDestinoId())
                .orElseThrow(() -> new BadRequestException("La bodega indicada no existe."));

        OrdenCompra orden = new OrdenCompra();
        orden.setProducto(producto);
        orden.setProveedor(proveedor);
        orden.setBodegaDestino(bodega);
        orden.setCantidad(detail.cantidad());
        orden.setPrecioUnitario(detail.precioUnitario());
        return ResponseEntity.ok(service.crearOrdenCompra(orden));
    }

    public record DraftOrderRequest(Long proveedorId, Long bodegaDestinoId,
                                    List<DraftOrderDetail> detalles) {
    }

    public record DraftOrderDetail(Long productoId, Integer cantidad, BigDecimal precioUnitario) {
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
        byte[] pdfData;
        try {
            pdfData = service.obtenerPdfOrden(id);
        } catch (ResourceNotFoundException e) {
            // Auto-generar PDF si no existe
            service.generarPdfOrden(id);
            pdfData = service.obtenerPdfOrden(id);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "orden_" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfData);
    }
}