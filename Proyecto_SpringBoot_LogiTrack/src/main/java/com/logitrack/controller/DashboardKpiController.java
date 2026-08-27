package com.logitrack.controller;

import com.logitrack.dto.DashboardKpiDTO;
import com.logitrack.model.Bodega;
import com.logitrack.model.InventarioBodega;
import com.logitrack.model.Producto;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.InventarioBodegaRepository;
import com.logitrack.repository.ProductoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DashboardKpiController {

    private static final int UMBRAL_RIESGO = 10;

    private final ProductoRepository productoRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final BodegaRepository bodegaRepository;

    public DashboardKpiController(ProductoRepository productoRepository,
                                 InventarioBodegaRepository inventarioBodegaRepository,
                                 BodegaRepository bodegaRepository) {
        this.productoRepository = productoRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
        this.bodegaRepository = bodegaRepository;
    }

    @GetMapping("/kpis")
    public ResponseEntity<DashboardKpiDTO> obtenerKpis() {
        List<Producto> productos = productoRepository.findAll();
        Map<Long, Integer> stockPorProducto = inventarioBodegaRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getProducto() != null ? inv.getProducto().getId() : -1L,
                        Collectors.summingInt(InventarioBodega::getStock)));

        long totalProductos = productos.size();
        long productosEnQuiebre = stockPorProducto.values().stream()
                .filter(stock -> stock == 0)
                .count();
        long productosEnRiesgo = stockPorProducto.values().stream()
                .filter(stock -> stock > 0 && stock <= UMBRAL_RIESGO)
                .count();

        BigDecimal valorTotalInventario = inventarioBodegaRepository.findAll().stream()
                .map(inv -> {
                    BigDecimal precio = inv.getProducto() != null && inv.getProducto().getPrecio() != null
                            ? inv.getProducto().getPrecio()
                            : BigDecimal.ZERO;
                    return precio.multiply(BigDecimal.valueOf(inv.getStock()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Bodega> bodegas = bodegaRepository.findAll();
        BigDecimal ocupacionPromedioBodegas = calcularOcupacionPromedioBodegas(bodegas);

        DashboardKpiDTO dto = DashboardKpiDTO.builder()
                .totalProductos(totalProductos)
                .productosEnRiesgo(productosEnRiesgo)
                .productosEnQuiebre(productosEnQuiebre)
                .valorTotalInventario(valorTotalInventario)
                .ocupacionPromedioBodegas(ocupacionPromedioBodegas)
                .build();

        return ResponseEntity.ok(dto);
    }

    private BigDecimal calcularOcupacionPromedioBodegas(List<Bodega> bodegas) {
        if (bodegas.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double promedio = bodegas.stream()
                .mapToDouble(bodega -> {
                    Integer capacidad = bodega.getCapacidad();
                    if (capacidad == null || capacidad <= 0) {
                        return 0d;
                    }
                    Integer stockTotal = inventarioBodegaRepository.sumStockByBodegaId(bodega.getId());
                    double stock = stockTotal != null ? stockTotal : 0;
                    return (stock * 100d) / capacidad;
                })
                .average()
                .orElse(0d);

        return BigDecimal.valueOf(promedio).setScale(2, RoundingMode.HALF_UP);
    }
}
