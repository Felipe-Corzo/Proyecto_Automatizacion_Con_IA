package com.logitrack.repository;

import com.logitrack.model.InventarioBodega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventarioBodegaRepository extends JpaRepository<InventarioBodega, Long>, JpaSpecificationExecutor<InventarioBodega> {

    List<InventarioBodega> findByProductoId(Long productoId);

    List<InventarioBodega> findByBodegaId(Long bodegaId);

    Optional<InventarioBodega> findByProductoIdAndBodegaId(Long productoId, Long bodegaId);

    @Query("SELECT COALESCE(SUM(inv.stock), 0) FROM InventarioBodega inv WHERE inv.producto.id = :productoId")
    Integer sumStockByProductoId(@Param("productoId") Long productoId);

    @Query("SELECT COALESCE(SUM(inv.stock), 0) FROM InventarioBodega inv WHERE inv.bodega.id = :bodegaId")
    Integer sumStockByBodegaId(@Param("bodegaId") Long bodegaId);

    List<InventarioBodega> findByProductoIdIn(List<Long> productoIds);

    List<InventarioBodega> findByStockLessThan(Integer umbral);

    List<InventarioBodega> findByStockGreaterThan(Integer umbral);

    @Query("SELECT inv FROM InventarioBodega inv WHERE inv.producto.id IN " +
           "(SELECT inv2.producto.id FROM InventarioBodega inv2 GROUP BY inv2.producto.id HAVING COUNT(inv2.bodega.id) = 1)")
    List<InventarioBodega> findProductosEnUnaSolaBodega();

    @Query("SELECT inv FROM InventarioBodega inv WHERE inv.producto.id IN " +
           "(SELECT inv2.producto.id FROM InventarioBodega inv2 GROUP BY inv2.producto.id HAVING COUNT(inv2.bodega.id) > 1)")
    List<InventarioBodega> findProductosEnMultiplesBodegas();
}
