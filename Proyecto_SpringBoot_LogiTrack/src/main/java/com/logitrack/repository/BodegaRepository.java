package com.logitrack.repository;

import com.logitrack.model.Bodega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BodegaRepository extends JpaRepository<Bodega, Long>, JpaSpecificationExecutor<Bodega> {

    List<Bodega> findByNombreContainingIgnoreCase(String nombre);

    Page<Bodega> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Boolean existsByNombreIgnoreCase(String nombre);

    List<Bodega> findByUbicacionContainingIgnoreCase(String ubicacion);

    Page<Bodega> findByUbicacionContainingIgnoreCase(String ubicacion, Pageable pageable);

    List<Bodega> findByEncargadoIsNull();

    Page<Bodega> findByEncargadoIsNull(Pageable pageable);

    List<Bodega> findByCapacidadGreaterThanEqual(Integer capacidad);

    Page<Bodega> findByCapacidadGreaterThanEqual(Integer capacidad, Pageable pageable);

    List<Bodega> findByCapacidadLessThanEqual(Integer capacidad);

    Page<Bodega> findByCapacidadLessThanEqual(Integer capacidad, Pageable pageable);
}
