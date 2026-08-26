package com.logitrack.repository;

import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long>, JpaSpecificationExecutor<MovimientoInventario> {

    List<MovimientoInventario> findByTipoMovimiento(TipoMovimiento tipoMovimiento);

    Page<MovimientoInventario> findByTipoMovimiento(TipoMovimiento tipoMovimiento, Pageable pageable);

    List<MovimientoInventario> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);

    Page<MovimientoInventario> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    @Query("SELECT m FROM MovimientoInventario m WHERE m.bodegaOrigen.id = :bodegaId OR m.bodegaDestino.id = :bodegaId")
    List<MovimientoInventario> findByBodegaId(@Param("bodegaId") Long bodegaId);

    @Query("SELECT m FROM MovimientoInventario m ORDER BY m.fecha DESC")
    List<MovimientoInventario> findAllOrderByFechaDesc();

    @Query("SELECT m FROM MovimientoInventario m ORDER BY m.fecha DESC")
    Page<MovimientoInventario> findAllOrderByFechaDesc(Pageable pageable);

    List<MovimientoInventario> findByUsuarioId(Long usuarioId);

    Page<MovimientoInventario> findByUsuarioId(Long usuarioId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM MovimientoInventario m JOIN m.detalles d WHERE d.producto.id = :productoId AND m.tipoMovimiento = com.logitrack.model.TipoMovimiento.SALIDA AND m.fecha >= :desde")
    Integer calcularSalidasUltimos30Dias(@Param("productoId") Long productoId, @Param("desde") LocalDateTime desde);

    @Query("SELECT COALESCE(SUM(ib.stock), 0) FROM InventarioBodega ib WHERE ib.producto.id = :productoId")
    Integer calcularStockActual(@Param("productoId") Long productoId);
}
