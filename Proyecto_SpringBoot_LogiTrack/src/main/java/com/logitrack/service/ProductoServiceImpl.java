package com.logitrack.service;

import com.logitrack.dto.DistribucionStockDTO;
import com.logitrack.dto.ProductoConInventarioDTO;
import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.Bodega;
import com.logitrack.model.InventarioBodega;
import com.logitrack.model.MovimientoDetalle;
import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.Producto;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.InventarioBodegaRepository;
import com.logitrack.repository.MovimientoInventarioRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ProductoSpecification;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductoServiceImpl implements ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);

    private final ProductoRepository productoRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final BodegaRepository bodegaRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                                InventarioBodegaRepository inventarioBodegaRepository,
                                BodegaRepository bodegaRepository,
                                MovimientoInventarioRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
        this.bodegaRepository = bodegaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Override
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    @Override
    public Page<Producto> obtenerTodos(Pageable pageable) {
        return productoRepository.findAll(pageable);
    }

    @Override
    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
    }

    @Override
    @Transactional
    public Producto guardar(Producto producto) {
        producto.setStock(0);
        return productoRepository.save(producto);
    }

    @Override
    @Transactional
    public Producto guardarConInventario(Producto producto, Map<Long, Integer> stockPorBodega) {
        int stockTotal = 0;
        producto.setStock(0);
        Producto saved = productoRepository.save(producto);

        if (stockPorBodega != null && !stockPorBodega.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : stockPorBodega.entrySet()) {
                Long bodegaId = entry.getKey();
                Integer cantidad = entry.getValue();

                if (cantidad == null || cantidad <= 0) continue;

                Bodega bodega = bodegaRepository.findById(bodegaId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bodega", "id", bodegaId));

                Integer currentStock = inventarioBodegaRepository.sumStockByBodegaId(bodega.getId());
                if (bodega.getCapacidad() < currentStock + cantidad) {
                    throw new BadRequestException(String.format(
                            "La bodega '%s' tiene la capacidad al máximo. Capacidad: %d, Stock actual: %d, Intentando ingresar: %d",
                            bodega.getNombre(), bodega.getCapacidad(), currentStock, cantidad));
                }

                InventarioBodega inventario = InventarioBodega.builder()
                        .producto(saved)
                        .bodega(bodega)
                        .stock(cantidad)
                        .build();
                inventarioBodegaRepository.save(inventario);

                stockTotal += cantidad;
            }
        }

        saved.setStock(stockTotal);
        return productoRepository.save(saved);
    }

    @Override
    @Transactional
    public Producto actualizar(Long id, Producto producto) {
        Producto productoExistente = obtenerPorId(id);
        productoExistente.setNombre(producto.getNombre());
        productoExistente.setCategoria(producto.getCategoria());
        productoExistente.setPrecio(producto.getPrecio());
        return productoRepository.save(productoExistente);
    }

    @Override
    @Transactional
    public Producto actualizarConInventario(Long id, Producto producto, Map<Long, Integer> stockPorBodega) {
        Producto productoExistente = obtenerPorId(id);
        productoExistente.setNombre(producto.getNombre());
        productoExistente.setCategoria(producto.getCategoria());
        productoExistente.setPrecio(producto.getPrecio());

        List<InventarioBodega> inventariosExistentes = inventarioBodegaRepository.findByProductoId(id);
        inventarioBodegaRepository.deleteAll(inventariosExistentes);

        int stockTotal = 0;
        if (stockPorBodega != null && !stockPorBodega.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : stockPorBodega.entrySet()) {
                Long bodegaId = entry.getKey();
                Integer cantidad = entry.getValue();

                if (cantidad == null || cantidad <= 0) continue;

                Bodega bodega = bodegaRepository.findById(bodegaId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bodega", "id", bodegaId));

                Integer currentStock = inventarioBodegaRepository.sumStockByBodegaId(bodega.getId());
                if (bodega.getCapacidad() < currentStock + cantidad) {
                    throw new BadRequestException(String.format(
                            "La bodega '%s' tiene la capacidad al máximo. Capacidad: %d, Stock actual: %d, Intentando ingresar: %d",
                            bodega.getNombre(), bodega.getCapacidad(), currentStock, cantidad));
                }

                InventarioBodega inventario = InventarioBodega.builder()
                        .producto(productoExistente)
                        .bodega(bodega)
                        .stock(cantidad)
                        .build();
                inventarioBodegaRepository.save(inventario);

                stockTotal += cantidad;
            }
        }

        productoExistente.setStock(stockTotal);
        return productoRepository.save(productoExistente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = obtenerPorId(id);

        List<InventarioBodega> inventarios = inventarioBodegaRepository.findByProductoId(id);
        if (!inventarios.isEmpty()) {
            inventarioBodegaRepository.deleteAll(inventarios);
        }

        List<MovimientoInventario> todosMovimientos = movimientoRepository.findAllOrderByFechaDesc();
        for (MovimientoInventario mov : todosMovimientos) {
            boolean necesitaActualizar = false;
            var iter = mov.getDetalles().iterator();
            while (iter.hasNext()) {
                MovimientoDetalle detalle = iter.next();
                if (detalle.getProducto().getId().equals(id)) {
                    iter.remove();
                    necesitaActualizar = true;
                }
            }
            if (necesitaActualizar) {
                movimientoRepository.save(mov);
            }
        }

        productoRepository.delete(producto);
    }

    @Override
    public List<Producto> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    public List<Producto> buscarBajoStock(Integer umbral) {
        return productoRepository.findByStockLessThan(umbral != null ? umbral : 10);
    }

    @Override
    public List<Producto> filtrarProductos(String nombre, String categoria, Boolean bajoStock) {
        return productoRepository.filtrarProductos(
                (nombre != null && !nombre.isBlank()) ? nombre : null,
                (categoria != null && !categoria.isBlank()) ? categoria : null,
                bajoStock
        );
    }

    @Override
    public Page<Producto> filtrarProductos(String nombre, String categoria, Boolean bajoStock, Pageable pageable) {
        return productoRepository.filtrarProductos(
                (nombre != null && !nombre.isBlank()) ? nombre : null,
                (categoria != null && !categoria.isBlank()) ? categoria : null,
                bajoStock,
                pageable
        );
    }

    @Override
    public Page<Producto> buscarAvanzado(String nombre, String categoria, Boolean bajoStock,
                                          BigDecimal precioMin, BigDecimal precioMax,
                                          Integer stockMin, Integer stockMax,
                                          Boolean sinStock, Boolean sinCategoria,
                                          Long bodegaId, String sortBy, String sortDir,
                                          Pageable pageable) {
        Specification<Producto> spec = ProductoSpecification.withFilters(
                nombre, categoria, bajoStock, precioMin, precioMax,
                stockMin, stockMax, sinStock, sinCategoria, bodegaId);
        return productoRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Producto> buscarPorRangoPrecio(BigDecimal min, BigDecimal max, Pageable pageable) {
        return productoRepository.findByPrecioBetween(min, max, pageable);
    }

    @Override
    public Page<Producto> buscarPorRangoStock(Integer min, Integer max, Pageable pageable) {
        return productoRepository.findByStockBetween(min, max, pageable);
    }

    @Override
    public List<Producto> buscarSinStock() {
        return productoRepository.findByStock(0);
    }

    @Override
    public Page<Producto> buscarPorBodega(Long bodegaId, Pageable pageable) {
        return buscarAvanzado(null, null, null, null, null, null, null,
                null, null, bodegaId, null, null, pageable);
    }

    @Override
    public List<DistribucionStockDTO> obtenerDistribucionStock(Long productoId) {
        Producto producto = obtenerPorId(productoId);
        List<InventarioBodega> inventarios = inventarioBodegaRepository.findByProductoId(productoId);

        if (inventarios.isEmpty()) {
            return List.of();
        }

        int stockTotal = producto.getStock();
        BigDecimal precio = producto.getPrecio();

        return inventarios.stream()
                .map(inv -> {
                    BigDecimal porcentaje = stockTotal > 0
                            ? BigDecimal.valueOf(inv.getStock())
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(stockTotal), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal valor = precio.multiply(BigDecimal.valueOf(inv.getStock()));
                    return DistribucionStockDTO.builder()
                            .bodegaId(inv.getBodega().getId())
                            .bodegaNombre(inv.getBodega().getNombre())
                            .stockEnBodega(inv.getStock())
                            .porcentajeDelTotal(porcentaje)
                            .valorEnBodega(valor)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductoConInventarioDTO> obtenerTodosConInventario() {
        List<Producto> productos = productoRepository.findAll();
        List<Long> productoIds = productos.stream().map(Producto::getId).toList();
        List<InventarioBodega> todosInventarios = inventarioBodegaRepository.findByProductoIdIn(productoIds);

        Map<Long, List<InventarioBodega>> inventarioMap = todosInventarios.stream()
                .collect(Collectors.groupingBy(inv -> inv.getProducto().getId()));

        return productos.stream()
                .map(p -> ProductoConInventarioDTO.fromProducto(p,
                        inventarioMap.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    @Override
    public ProductoConInventarioDTO obtenerConInventarioPorId(Long id) {
        Producto producto = obtenerPorId(id);
        List<InventarioBodega> inventarios = inventarioBodegaRepository.findByProductoId(id);
        return ProductoConInventarioDTO.fromProducto(producto, inventarios);
    }
}
