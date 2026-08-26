package com.logitrack.service;

import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioService {

    List<MovimientoInventario> obtenerTodos();

    Page<MovimientoInventario> obtenerTodos(Pageable pageable);

    MovimientoInventario obtenerPorId(Long id);

    MovimientoInventario registrarMovimiento(MovimientoInventario movimiento);

    List<MovimientoInventario> buscarPorTipo(TipoMovimiento tipo);

    Page<MovimientoInventario> buscarPorTipo(TipoMovimiento tipo, Pageable pageable);

    List<MovimientoInventario> buscarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta);

    Page<MovimientoInventario> buscarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    List<MovimientoInventario> buscarPorBodega(Long bodegaId);

    // Advanced search
    Page<MovimientoInventario> buscarAvanzado(TipoMovimiento tipo, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                               Long bodegaId, Long bodegaDestinoId, Long bodegaOrigenId,
                                               Long usuarioId, Long productoId, Pageable pageable);

    Page<MovimientoInventario> buscarPorUsuario(Long usuarioId, Pageable pageable);

    List<MovimientoInventario> buscarPorProducto(Long productoId);
}
