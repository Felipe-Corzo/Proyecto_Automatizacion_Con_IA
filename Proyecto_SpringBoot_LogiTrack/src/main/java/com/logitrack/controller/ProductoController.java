package com.logitrack.controller;

import com.logitrack.dto.DistribucionStockDTO;
import com.logitrack.dto.ProductoConInventarioDTO;
import com.logitrack.model.Producto;
import com.logitrack.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<?> obtenerTodos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean bajoStock,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Pageable pageable) {
        boolean hasPagination = page != null || size != null;
        if (nombre != null || categoria != null || bajoStock != null) {
            if (hasPagination) {
                return ResponseEntity.ok(productoService.filtrarProductos(nombre, categoria, bajoStock, pageable));
            }
            return ResponseEntity.ok(productoService.filtrarProductos(nombre, categoria, bajoStock));
        }
        if (hasPagination) {
            return ResponseEntity.ok(productoService.obtenerTodos(pageable));
        }
        return ResponseEntity.ok(productoService.obtenerTodos());
    }

    // === NEW: Advanced Search Endpoint ===
    @GetMapping("/search")
    public ResponseEntity<Page<Producto>> buscarAvanzado(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean bajoStock,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) Integer stockMin,
            @RequestParam(required = false) Integer stockMax,
            @RequestParam(required = false) Boolean sinStock,
            @RequestParam(required = false) Boolean sinCategoria,
            @RequestParam(required = false) Long bodegaId,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productoService.buscarAvanzado(
                nombre, categoria, bajoStock, precioMin, precioMax,
                stockMin, stockMax, sinStock, sinCategoria, bodegaId,
                sortBy, sortDir, pageable));
    }

    // === NEW: Rango de Precio ===
    @GetMapping("/rango-precio")
    public ResponseEntity<Page<Producto>> buscarPorRangoPrecio(
            @RequestParam BigDecimal min, @RequestParam BigDecimal max,
            Pageable pageable) {
        return ResponseEntity.ok(productoService.buscarPorRangoPrecio(min, max, pageable));
    }

    // === NEW: Rango de Stock ===
    @GetMapping("/rango-stock")
    public ResponseEntity<Page<Producto>> buscarPorRangoStock(
            @RequestParam(required = false) Integer min,
            @RequestParam(required = false) Integer max,
            Pageable pageable) {
        if (min == null) min = 0;
        if (max == null) max = Integer.MAX_VALUE;
        return ResponseEntity.ok(productoService.buscarPorRangoStock(min, max, pageable));
    }

    // === NEW: Productos sin Stock ===
    @GetMapping("/sin-stock")
    public ResponseEntity<List<Producto>> obtenerSinStock() {
        return ResponseEntity.ok(productoService.buscarSinStock());
    }

    // === NEW: Productos en una bodega ===
    @GetMapping("/por-bodega/{bodegaId}")
    public ResponseEntity<Page<Producto>> buscarPorBodega(
            @PathVariable Long bodegaId, Pageable pageable) {
        return ResponseEntity.ok(productoService.buscarPorBodega(bodegaId, pageable));
    }

    // === NEW: Distribución de stock por bodega ===
    @GetMapping("/{id}/distribucion-stock")
    public ResponseEntity<List<DistribucionStockDTO>> obtenerDistribucionStock(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerDistribucionStock(id));
    }

    @GetMapping("/con-inventario")
    public ResponseEntity<List<ProductoConInventarioDTO>> obtenerTodosConInventario() {
        return ResponseEntity.ok(productoService.obtenerTodosConInventario());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @GetMapping("/{id}/con-inventario")
    public ResponseEntity<ProductoConInventarioDTO> obtenerConInventarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerConInventarioPorId(id));
    }

    @GetMapping("/bajo-stock")
    public ResponseEntity<List<Producto>> obtenerBajoStock(@RequestParam(defaultValue = "10") Integer umbral) {
        return ResponseEntity.ok(productoService.buscarBajoStock(umbral));
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody Producto producto) {
        return new ResponseEntity<>(productoService.guardar(producto), HttpStatus.CREATED);
    }

    @PostMapping("/con-inventario")
    public ResponseEntity<Producto> crearConInventario(@Valid @RequestBody ProductoRequest request) {
        return new ResponseEntity<>(
            productoService.guardarConInventario(request.producto(), request.stockPorBodega()),
            HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id, @Valid @RequestBody Producto producto) {
        return ResponseEntity.ok(productoService.actualizar(id, producto));
    }

    @PutMapping("/{id}/con-inventario")
    public ResponseEntity<Producto> actualizarConInventario(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(
            productoService.actualizarConInventario(id, request.producto(), request.stockPorBodega())
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

record ProductoRequest(Producto producto, Map<Long, Integer> stockPorBodega) {}
