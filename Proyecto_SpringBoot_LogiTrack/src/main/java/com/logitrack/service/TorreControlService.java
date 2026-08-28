package com.logitrack.service;

import com.logitrack.dto.CoberturaDTO;
import com.logitrack.model.OrdenCompra;
import com.logitrack.model.EstadoOrden;
import com.logitrack.model.Producto;
import java.util.List;

public interface TorreControlService {
    CoberturaDTO calcularCoberturaProducto(Producto producto);
    boolean evaluarProductoEnRiesgo(Producto producto);
    OrdenCompra crearOrdenCompra(OrdenCompra orden);
    OrdenCompra actualizarOrdenCompra(Long id, OrdenCompra ordenActualizada);
    OrdenCompra cambiarEstadoOrden(Long id, EstadoOrden nuevoEstado);
    void generarPdfOrden(Long id);
    byte[] obtenerPdfOrden(Long id);
}
