package com.logitrack.service;

import com.logitrack.dto.StockPorBodegaDTO;
import com.logitrack.dto.ValorInventarioBodegaDTO;
import com.logitrack.model.Bodega;
import com.logitrack.model.InventarioBodega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BodegaService {

    List<Bodega> obtenerTodas();

    Page<Bodega> obtenerTodas(Pageable pageable);

    Bodega obtenerPorId(Long id);

    Bodega guardar(Bodega bodega);

    Bodega actualizar(Long id, Bodega bodega);

    void eliminar(Long id);

    List<Bodega> buscarPorNombre(String nombre);

    Page<Bodega> buscarPorNombre(String nombre, Pageable pageable);

    List<InventarioBodega> obtenerInventarioPorBodega(Long bodegaId);

    List<StockPorBodegaDTO> obtenerStockTodas();

    // Advanced search
    Page<Bodega> buscarAvanzado(String nombre, String ubicacion, Boolean sinEncargado,
                                 Long capacidadMin, Long capacidadMax, Pageable pageable);

    Page<Bodega> buscarPorUbicacion(String ubicacion, Pageable pageable);

    Page<Bodega> buscarSinEncargado(Pageable pageable);

    Page<Bodega> buscarPorCapacidadMinima(Integer capacidad, Pageable pageable);

    Page<Bodega> buscarPorCapacidadMaxima(Integer capacidad, Pageable pageable);

    // Analytics
    List<ValorInventarioBodegaDTO> obtenerValorInventarioTodas();
}
