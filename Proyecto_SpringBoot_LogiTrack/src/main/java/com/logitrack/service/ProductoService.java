package com.logitrack.service;

import com.logitrack.dto.DistribucionStockDTO;
import com.logitrack.dto.ProductoConInventarioDTO;
import com.logitrack.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductoService {

    List<Producto> obtenerTodos();

    Page<Producto> obtenerTodos(Pageable pageable);

    Producto obtenerPorId(Long id);

    Producto guardar(Producto producto);

    Producto actualizar(Long id, Producto producto);

    void eliminar(Long id);

    List<Producto> buscarPorNombre(String nombre);

    List<Producto> buscarBajoStock(Integer umbral);

    List<Producto> filtrarProductos(String nombre, String categoria, Boolean bajoStock);

    Page<Producto> filtrarProductos(String nombre, String categoria, Boolean bajoStock, Pageable pageable);

    // Advanced search
    Page<Producto> buscarAvanzado(String nombre, String categoria, Boolean bajoStock,
                                   BigDecimal precioMin, BigDecimal precioMax,
                                   Integer stockMin, Integer stockMax,
                                   Boolean sinStock, Boolean sinCategoria,
                                   Long bodegaId, String sortBy, String sortDir,
                                   Pageable pageable);

    Page<Producto> buscarPorRangoPrecio(BigDecimal min, BigDecimal max, Pageable pageable);

    Page<Producto> buscarPorRangoStock(Integer min, Integer max, Pageable pageable);

    List<Producto> buscarSinStock();

    Page<Producto> buscarPorBodega(Long bodegaId, Pageable pageable);

    // Analytics
    List<DistribucionStockDTO> obtenerDistribucionStock(Long productoId);

    Producto guardarConInventario(Producto producto, Map<Long, Integer> stockPorBodega);

    Producto actualizarConInventario(Long id, Producto producto, Map<Long, Integer> stockPorBodega);

    List<ProductoConInventarioDTO> obtenerTodosConInventario();

    ProductoConInventarioDTO obtenerConInventarioPorId(Long id);
}
