package com.logitrack.service;

import com.logitrack.dto.StockPorBodegaDTO;
import com.logitrack.dto.ValorInventarioBodegaDTO;
import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.Bodega;
import com.logitrack.model.InventarioBodega;
import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.Producto;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.BodegaSpecification;
import com.logitrack.repository.InventarioBodegaRepository;
import com.logitrack.repository.MovimientoInventarioRepository;
import com.logitrack.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BodegaServiceImpl implements BodegaService {

    private static final Logger log = LoggerFactory.getLogger(BodegaServiceImpl.class);

    private final BodegaRepository bodegaRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;

    public BodegaServiceImpl(BodegaRepository bodegaRepository,
                              InventarioBodegaRepository inventarioBodegaRepository,
                              MovimientoInventarioRepository movimientoRepository,
                              ProductoRepository productoRepository) {
        this.bodegaRepository = bodegaRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public List<Bodega> obtenerTodas() {
        return bodegaRepository.findAll();
    }

    @Override
    public Page<Bodega> obtenerTodas(Pageable pageable) {
        return bodegaRepository.findAll(pageable);
    }

    @Override
    public Bodega obtenerPorId(Long id) {
        return bodegaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega", "id", id));
    }

    @Override
    @Transactional
    public Bodega guardar(Bodega bodega) {
        if (bodegaRepository.existsByNombreIgnoreCase(bodega.getNombre())) {
            throw new BadRequestException("Ya existe una bodega con el nombre: " + bodega.getNombre());
        }
        if (bodega.getEncargado() != null && (bodega.getEncargado().getId() == null || bodega.getEncargado().getId() <= 0)) {
            bodega.setEncargado(null);
        }
        return bodegaRepository.save(bodega);
    }

    @Override
    @Transactional
    public Bodega actualizar(Long id, Bodega bodega) {
        Bodega bodegaExistente = obtenerPorId(id);

        if (!bodegaExistente.getNombre().equalsIgnoreCase(bodega.getNombre()) &&
            bodegaRepository.existsByNombreIgnoreCase(bodega.getNombre())) {
            throw new BadRequestException("Ya existe otra bodega con el nombre: " + bodega.getNombre());
        }

        if (bodega.getEncargado() != null && (bodega.getEncargado().getId() == null || bodega.getEncargado().getId() <= 0)) {
            bodega.setEncargado(null);
        }

        bodegaExistente.setNombre(bodega.getNombre());
        bodegaExistente.setUbicacion(bodega.getUbicacion());
        bodegaExistente.setCapacidad(bodega.getCapacidad());
        bodegaExistente.setEncargado(bodega.getEncargado());

        return bodegaRepository.save(bodegaExistente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Bodega bodega = obtenerPorId(id);

        List<InventarioBodega> inventarios = inventarioBodegaRepository.findByBodegaId(id);
        if (!inventarios.isEmpty()) {
            for (InventarioBodega inv : inventarios) {
                Producto producto = inv.getProducto();
                producto.setStock(producto.getStock() - inv.getStock());
                if (producto.getStock() < 0) {
                    producto.setStock(0);
                }
                productoRepository.save(producto);
            }
            inventarioBodegaRepository.deleteAll(inventarios);
        }

        List<MovimientoInventario> movimientos = movimientoRepository.findByBodegaId(id);
        for (MovimientoInventario mov : movimientos) {
            if (mov.getBodegaOrigen() != null && mov.getBodegaOrigen().getId().equals(id)) {
                mov.setBodegaOrigen(null);
            }
            if (mov.getBodegaDestino() != null && mov.getBodegaDestino().getId().equals(id)) {
                mov.setBodegaDestino(null);
            }
            movimientoRepository.save(mov);
        }

        bodegaRepository.delete(bodega);
    }

    @Override
    public List<Bodega> buscarPorNombre(String nombre) {
        return bodegaRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    public Page<Bodega> buscarPorNombre(String nombre, Pageable pageable) {
        return bodegaRepository.findByNombreContainingIgnoreCase(nombre, pageable);
    }

    @Override
    public List<InventarioBodega> obtenerInventarioPorBodega(Long bodegaId) {
        obtenerPorId(bodegaId);
        return inventarioBodegaRepository.findByBodegaId(bodegaId);
    }

    @Override
    public List<StockPorBodegaDTO> obtenerStockTodas() {
        List<Bodega> bodegas = bodegaRepository.findAll();
        return bodegas.stream().map(bodega -> {
            Integer stockTotal = inventarioBodegaRepository.sumStockByBodegaId(bodega.getId());
            return StockPorBodegaDTO.builder()
                    .bodegaId(bodega.getId())
                    .bodegaNombre(bodega.getNombre())
                    .stockTotal(stockTotal != null ? stockTotal.longValue() : 0L)
                    .build();
        }).collect(Collectors.toList());
    }

    // === Advanced Search Methods ===

    @Override
    public Page<Bodega> buscarAvanzado(String nombre, String ubicacion, Boolean sinEncargado,
                                        Long capacidadMin, Long capacidadMax, Pageable pageable) {
        Specification<Bodega> spec = BodegaSpecification.withFilters(
                nombre, ubicacion, sinEncargado, capacidadMin, capacidadMax);
        return bodegaRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Bodega> buscarPorUbicacion(String ubicacion, Pageable pageable) {
        return bodegaRepository.findByUbicacionContainingIgnoreCase(ubicacion, pageable);
    }

    @Override
    public Page<Bodega> buscarSinEncargado(Pageable pageable) {
        return bodegaRepository.findByEncargadoIsNull(pageable);
    }

    @Override
    public Page<Bodega> buscarPorCapacidadMinima(Integer capacidad, Pageable pageable) {
        return bodegaRepository.findByCapacidadGreaterThanEqual(capacidad, pageable);
    }

    @Override
    public Page<Bodega> buscarPorCapacidadMaxima(Integer capacidad, Pageable pageable) {
        return bodegaRepository.findByCapacidadLessThanEqual(capacidad, pageable);
    }

    @Override
    public List<ValorInventarioBodegaDTO> obtenerValorInventarioTodas() {
        List<Bodega> bodegas = bodegaRepository.findAll();
        return bodegas.stream().map(bodega -> {
            List<InventarioBodega> inventarios = inventarioBodegaRepository.findByBodegaId(bodega.getId());
            long totalProductos = inventarios.size();
            long totalUnidades = inventarios.stream().mapToLong(InventarioBodega::getStock).sum();

            BigDecimal valorTotal = inventarios.stream()
                    .map(inv -> inv.getProducto().getPrecio()
                            .multiply(BigDecimal.valueOf(inv.getStock())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal capacidadOcupacion = bodega.getCapacidad() > 0
                    ? BigDecimal.valueOf(totalUnidades)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(bodega.getCapacidad()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return ValorInventarioBodegaDTO.builder()
                    .bodegaId(bodega.getId())
                    .bodegaNombre(bodega.getNombre())
                    .totalProductos(totalProductos)
                    .totalUnidades(totalUnidades)
                    .valorTotalInventario(valorTotal)
                    .capacidadOcupacion(capacidadOcupacion)
                    .build();
        }).collect(Collectors.toList());
    }
}
