package com.logitrack.controller;

import com.logitrack.model.Proveedor;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ProveedorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;

    public ProveedorController(ProductoRepository productoRepository, ProveedorRepository proveedorRepository) {
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @GetMapping
    public ResponseEntity<List<Proveedor>> listar() {
        return ResponseEntity.ok(proveedorRepository.findAll().stream()
                .filter(proveedor -> Boolean.TRUE.equals(proveedor.getActivo()))
                .toList());
    }

    @GetMapping("/by-product/{productoId}")
    public ResponseEntity<List<Proveedor>> obtenerPorProducto(@PathVariable Long productoId) {
        return productoRepository.findById(productoId)
                .map(producto -> {
                    Proveedor proveedor = producto.getProveedorPrincipal();
                    if (proveedor == null || !Boolean.TRUE.equals(proveedor.getActivo())) {
                        return ResponseEntity.ok(List.<Proveedor>of());
                    }
                    return ResponseEntity.ok(List.of(proveedor));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}