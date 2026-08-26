package com.logitrack.service;

import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.*;
import com.logitrack.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private static final Logger log = LoggerFactory.getLogger(MovimientoInventarioServiceImpl.class);

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final BodegaRepository bodegaRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;

    public MovimientoInventarioServiceImpl(MovimientoInventarioRepository movimientoRepository,
            ProductoRepository productoRepository,
            BodegaRepository bodegaRepository,
            UsuarioRepository usuarioRepository,
            InventarioBodegaRepository inventarioBodegaRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.bodegaRepository = bodegaRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
    }

    @Override
    public List<MovimientoInventario> obtenerTodos() {
        return movimientoRepository.findAllOrderByFechaDesc();
    }

    @Override
    public Page<MovimientoInventario> obtenerTodos(Pageable pageable) {
        return movimientoRepository.findAllOrderByFechaDesc(pageable);
    }

    @Override
    public MovimientoInventario obtenerPorId(Long id) {
        return movimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MovimientoInventario", "id", id));
    }

    @Override
    @Transactional
    public MovimientoInventario registrarMovimiento(MovimientoInventario movimiento) {
        if (movimiento.getDetalles() == null || movimiento.getDetalles().isEmpty()) {
            throw new BadRequestException("El movimiento debe contener al menos un detalle de producto.");
        }

        // Set manualmente un usuario por defecto si no hay contexto de seguridad
        if (movimiento.getUsuario() == null || movimiento.getUsuario().getId() == null) {
            // Buscar un usuario ADMIN por defecto
            Usuario usuarioDefault = usuarioRepository.findAll().stream()
                    .filter(u -> u.getRol() == Rol.ADMIN)
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("No se encontró un usuario ADMIN para asignar al movimiento."));
            movimiento.setUsuario(usuarioDefault);
        } else {
            Usuario usuario = usuarioRepository.findById(movimiento.getUsuario().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", movimiento.getUsuario().getId()));
            movimiento.setUsuario(usuario);
        }

        validarBodegasYTipo(movimiento);
        validarCapacidadBodega(movimiento);

        for (MovimientoDetalle detalle : movimiento.getDetalles()) {
            detalle.setMovimiento(movimiento);
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", detalle.getProducto().getId()));

            if (movimiento.getTipoMovimiento() == TipoMovimiento.ENTRADA) {
                InventarioBodega invDestino = inventarioBodegaRepository
                        .findByProductoIdAndBodegaId(producto.getId(), movimiento.getBodegaDestino().getId())
                        .orElseGet(() -> InventarioBodega.builder()
                                .producto(producto)
                                .bodega(movimiento.getBodegaDestino())
                                .stock(0)
                                .build());

                invDestino.setStock(invDestino.getStock() + detalle.getCantidad());
                inventarioBodegaRepository.save(invDestino);
                producto.setStock(producto.getStock() + detalle.getCantidad());

            } else if (movimiento.getTipoMovimiento() == TipoMovimiento.SALIDA) {
                InventarioBodega invOrigen = inventarioBodegaRepository
                        .findByProductoIdAndBodegaId(producto.getId(), movimiento.getBodegaOrigen().getId())
                        .orElseThrow(() -> new BadRequestException(String.format(
                                "El producto '%s' no tiene inventario registrado en la bodega '%s'.",
                                producto.getNombre(), movimiento.getBodegaOrigen().getNombre())));

                if (invOrigen.getStock() < detalle.getCantidad()) {
                    throw new BadRequestException(String.format(
                            "Stock insuficiente en bodega '%s' para el producto '%s'. Stock en bodega: %d, Solicitado: %d",
                            movimiento.getBodegaOrigen().getNombre(), producto.getNombre(),
                            invOrigen.getStock(), detalle.getCantidad()));
                }

                invOrigen.setStock(invOrigen.getStock() - detalle.getCantidad());
                inventarioBodegaRepository.save(invOrigen);
                producto.setStock(producto.getStock() - detalle.getCantidad());

            } else if (movimiento.getTipoMovimiento() == TipoMovimiento.TRANSFERENCIA) {
                InventarioBodega invOrigen = inventarioBodegaRepository
                        .findByProductoIdAndBodegaId(producto.getId(), movimiento.getBodegaOrigen().getId())
                        .orElseThrow(() -> new BadRequestException(String.format(
                                "El producto '%s' no tiene inventario en la bodega origen '%s'.",
                                producto.getNombre(), movimiento.getBodegaOrigen().getNombre())));

                if (invOrigen.getStock() < detalle.getCantidad()) {
                    throw new BadRequestException(String.format(
                            "Stock insuficiente en bodega '%s' para transferir. Stock: %d, Solicitado: %d",
                            movimiento.getBodegaOrigen().getNombre(), invOrigen.getStock(), detalle.getCantidad()));
                }

                invOrigen.setStock(invOrigen.getStock() - detalle.getCantidad());
                inventarioBodegaRepository.save(invOrigen);

                InventarioBodega invDestino = inventarioBodegaRepository
                        .findByProductoIdAndBodegaId(producto.getId(), movimiento.getBodegaDestino().getId())
                        .orElseGet(() -> InventarioBodega.builder()
                                .producto(producto)
                                .bodega(movimiento.getBodegaDestino())
                                .stock(0)
                                .build());

                invDestino.setStock(invDestino.getStock() + detalle.getCantidad());
                inventarioBodegaRepository.save(invDestino);
            }

            productoRepository.save(producto);
            detalle.setProducto(producto);
        }

        return movimientoRepository.save(movimiento);
    }

    private void validarBodegasYTipo(MovimientoInventario m) {
        if (m.getTipoMovimiento() == TipoMovimiento.ENTRADA) {
            if (m.getBodegaDestino() == null || m.getBodegaDestino().getId() == null) {
                throw new BadRequestException("Para movimientos de ENTRADA se requiere especificar la Bodega Destino.");
            }
            Bodega destino = bodegaRepository.findById(m.getBodegaDestino().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bodega Destino", "id", m.getBodegaDestino().getId()));
            m.setBodegaDestino(destino);
            m.setBodegaOrigen(null);
        } else if (m.getTipoMovimiento() == TipoMovimiento.SALIDA) {
            if (m.getBodegaOrigen() == null || m.getBodegaOrigen().getId() == null) {
                throw new BadRequestException("Para movimientos de SALIDA se requiere especificar la Bodega Origen.");
            }
            Bodega origen = bodegaRepository.findById(m.getBodegaOrigen().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bodega Origen", "id", m.getBodegaOrigen().getId()));
            m.setBodegaOrigen(origen);
            m.setBodegaDestino(null);
        } else if (m.getTipoMovimiento() == TipoMovimiento.TRANSFERENCIA) {
            if (m.getBodegaOrigen() == null || m.getBodegaOrigen().getId() == null ||
                    m.getBodegaDestino() == null || m.getBodegaDestino().getId() == null) {
                throw new BadRequestException("Para movimientos de TRANSFERENCIA se requieren Bodega Origen y Bodega Destino.");
            }
            if (m.getBodegaOrigen().getId().equals(m.getBodegaDestino().getId())) {
                throw new BadRequestException("La Bodega Origen y Bodega Destino no pueden ser la misma para una transferencia.");
            }
            Bodega origen = bodegaRepository.findById(m.getBodegaOrigen().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bodega Origen", "id", m.getBodegaOrigen().getId()));
            Bodega destino = bodegaRepository.findById(m.getBodegaDestino().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bodega Destino", "id", m.getBodegaDestino().getId()));
            m.setBodegaOrigen(origen);
            m.setBodegaDestino(destino);
        }
    }

    private void validarCapacidadBodega(MovimientoInventario movimiento) {
        int totalProductos = movimiento.getDetalles().stream()
                .filter(d -> d.getCantidad() != null && d.getCantidad() > 0)
                .mapToInt(MovimientoDetalle::getCantidad)
                .sum();

        if (movimiento.getTipoMovimiento() == TipoMovimiento.ENTRADA) {
            Bodega destino = movimiento.getBodegaDestino();
            Integer currentStock = inventarioBodegaRepository.sumStockByBodegaId(destino.getId());
            if (destino.getCapacidad() < currentStock + totalProductos) {
                throw new BadRequestException(String.format(
                        "La bodega '%s' tiene la capacidad al máximo. Capacidad: %d, Stock actual: %d, Intentando ingresar: %d",
                        destino.getNombre(), destino.getCapacidad(), currentStock, totalProductos));
            }
        } else if (movimiento.getTipoMovimiento() == TipoMovimiento.TRANSFERENCIA) {
            Bodega destino = movimiento.getBodegaDestino();
            Integer currentStockDestino = inventarioBodegaRepository.sumStockByBodegaId(destino.getId());
            if (destino.getCapacidad() < currentStockDestino + totalProductos) {
                throw new BadRequestException(String.format(
                        "La bodega '%s' tiene la capacidad al máximo. Capacidad: %d, Stock actual: %d, Intentando transferir: %d",
                        destino.getNombre(), destino.getCapacidad(), currentStockDestino, totalProductos));
            }
        }
    }

    @Override
    public List<MovimientoInventario> buscarPorTipo(TipoMovimiento tipo) {
        return movimientoRepository.findByTipoMovimiento(tipo);
    }

    @Override
    public Page<MovimientoInventario> buscarPorTipo(TipoMovimiento tipo, Pageable pageable) {
        return movimientoRepository.findByTipoMovimiento(tipo, pageable);
    }

    @Override
    public List<MovimientoInventario> buscarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        return movimientoRepository.findByFechaBetween(desde, hasta);
    }

    @Override
    public Page<MovimientoInventario> buscarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        return movimientoRepository.findByFechaBetween(desde, hasta, pageable);
    }

    @Override
    public List<MovimientoInventario> buscarPorBodega(Long bodegaId) {
        return movimientoRepository.findByBodegaId(bodegaId);
    }

    // === Advanced Search Methods ===

    @Override
    public Page<MovimientoInventario> buscarAvanzado(TipoMovimiento tipo, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                                      Long bodegaId, Long bodegaDestinoId, Long bodegaOrigenId,
                                                      Long usuarioId, Long productoId, Pageable pageable) {
        Specification<MovimientoInventario> spec = MovimientoSpecification.withFilters(
                tipo, fechaDesde, fechaHasta, bodegaId, bodegaDestinoId, bodegaOrigenId, usuarioId, productoId);
        return movimientoRepository.findAll(spec, pageable);
    }

    @Override
    public Page<MovimientoInventario> buscarPorUsuario(Long usuarioId, Pageable pageable) {
        return movimientoRepository.findByUsuarioId(usuarioId, pageable);
    }

    @Override
    public List<MovimientoInventario> buscarPorProducto(Long productoId) {
        // Buscar movimientos que contengan este producto en sus detalles
        List<MovimientoInventario> todos = movimientoRepository.findAllOrderByFechaDesc();
        List<MovimientoInventario> result = new ArrayList<>();
        for (MovimientoInventario mov : todos) {
            if (mov.getDetalles() != null) {
                for (MovimientoDetalle det : mov.getDetalles()) {
                    if (det.getProducto() != null && det.getProducto().getId().equals(productoId)) {
                        result.add(mov);
                        break;
                    }
                }
            }
        }
        return result;
    }
}
