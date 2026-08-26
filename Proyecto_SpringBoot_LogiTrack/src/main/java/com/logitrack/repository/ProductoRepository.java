package com.logitrack.repository;

import com.logitrack.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    List<Producto> findByCategoriaIgnoreCase(String categoria);

    List<Producto> findByStockLessThan(Integer umbralStock);

    Page<Producto> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Producto> findByCategoriaIgnoreCase(String categoria, Pageable pageable);

    Page<Producto> findByStockLessThan(Integer umbralStock, Pageable pageable);

    Page<Producto> findByPrecioBetween(BigDecimal min, BigDecimal max, Pageable pageable);

    Page<Producto> findByStockBetween(Integer min, Integer max, Pageable pageable);

    List<Producto> findByStock(Integer stock);

    @Query("SELECT p FROM Producto p WHERE (:nombre IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
           "AND (:categoria IS NULL OR LOWER(p.categoria) = LOWER(:categoria)) " +
           "AND (:bajoStock IS NULL OR (:bajoStock = true AND p.stock < 10) OR (:bajoStock = false))")
    List<Producto> filtrarProductos(String nombre, String categoria, Boolean bajoStock);

    @Query("SELECT p FROM Producto p WHERE (:nombre IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
           "AND (:categoria IS NULL OR LOWER(p.categoria) = LOWER(:categoria)) " +
           "AND (:bajoStock IS NULL OR (:bajoStock = true AND p.stock < 10) OR (:bajoStock = false))")
    Page<Producto> filtrarProductos(String nombre, String categoria, Boolean bajoStock, Pageable pageable);
}
