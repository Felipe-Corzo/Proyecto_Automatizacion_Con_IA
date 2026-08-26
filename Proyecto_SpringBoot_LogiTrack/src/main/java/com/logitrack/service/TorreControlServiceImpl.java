package com.logitrack.service;

import com.logitrack.dto.CoberturaDTO;
import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.*;
import com.logitrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TorreControlServiceImpl implements TorreControlService {

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PdfGeneratorService pdfGeneratorService; // Servicio auxiliar para dibujar el PDF

    @Override
    public CoberturaDTO calcularCoberturaProducto(Producto producto) {
        CoberturaDTO dto = new CoberturaDTO();
        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        
        // Obtener salidas dinámicamente de la base de datos
        Integer salidas = movimientoRepository.calcularSalidasUltimos30Dias(producto.getId(), hace30Dias);
        if (salidas == null || salidas == 0) { // Regla TDD: Consumo 0 [4, 5]
            dto.setDiasCobertura(null);
            dto.setEstadoCobertura("SIN_CONSUMO");
            return dto;
        }

        double consumoDiario = salidas / 30.0;
        Integer stockActual = movimientoRepository.calcularStockActual(producto.getId());
        if (stockActual == null) stockActual = 0;

        double cobertura = stockActual / consumoDiario;
        dto.setDiasCobertura(cobertura);
        dto.setEstadoCobertura(cobertura < 15 ? "CRÍTICO" : "ESTABLE");
        return dto;
    }

    @Override
    public boolean evaluarProductoEnRiesgo(Producto producto) {
        Proveedor prov = producto.getProveedorPrincipal();
        if (prov == null) return false; // Excluido de alerta si no tiene proveedor [1]

        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        Integer salidas = movimientoRepository.calcularSalidasUltimos30Dias(producto.getId(), hace30Dias);
        if (salidas == null || salidas == 0) return false;

        double consumoDiario = salidas / 30.0;
        double puntoReorden = consumoDiario * prov.getDiasEntrega() * 1.5; // Fórmula matemática exacta [4, 6]
        Integer stockActual = movimientoRepository.calcularStockActual(producto.getId());
        if (stockActual == null) stockActual = 0;

        // Regla TDD: Stock estrictamente menor al punto de reorden [4, 5]
        return stockActual < puntoReorden;
    }

    @Override
    @Transactional
    public OrdenCompra crearOrdenCompra(OrdenCompra orden) {
        if (orden.getCantidad() == null || orden.getCantidad() <= 0) { // Regla TDD: cantidad inválida [1, 5]
            throw new BadRequestException("La cantidad de la orden de compra debe ser estrictamente mayor a cero.");
        }
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setEstado(EstadoOrden.BORRADOR);
        orden.setTotal(orden.getPrecioUnitario().multiply(BigDecimal.valueOf(orden.getCantidad())));
        return ordenCompraRepository.save(orden);
    }

    @Override
    @Transactional // Hace que la actualización de la orden y la inserción del movimiento ocurran en un solo bloque atómico [7]
    public OrdenCompra cambiarEstadoOrden(Long id, EstadoOrden nuevoEstado) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada"));

        EstadoOrden estadoActual = orden.getEstado();

        // Validar transiciones prohibidas de la especificación [8]
        if (estadoActual == EstadoOrden.CANCELADA || estadoActual == EstadoOrden.RECIBIDA) {
            throw new BadRequestException("No se pueden realizar cambios en una orden en estado final: " + estadoActual);
        }
        if (estadoActual == EstadoOrden.BORRADOR && nuevoEstado == EstadoOrden.RECIBIDA) {
            throw new BadRequestException("No se puede recibir una orden que todavía está en BORRADOR. Debe ser aprobada primero.");
        }

        // Si cambia de estado, se invalida y elimina el PDF anterior [9]
        orden.setPdfData(null);
        orden.setPdfFechaGeneracion(null);

        orden.setEstado(nuevoEstado);

        // RECEPCIÓN ATÓMICA: Si pasa de APROBADA a RECIBIDA, registrar automáticamente una entrada de stock [7]
        if (estadoActual == EstadoOrden.APROBADA && nuevoEstado == EstadoOrden.RECIBIDA) {
            registrarEntradaInventarioAutomatica(orden);
        }

        return ordenCompraRepository.save(orden);
    }

    private void registrarEntradaInventarioAutomatica(OrdenCompra orden) {
        // Creamos un nuevo objeto Movimiento en la base de datos
        MovimientoInventario mov = new MovimientoInventario();
        mov.setFecha(LocalDateTime.now());
        mov.setTipoMovimiento(TipoMovimiento.ENTRADA); // Entrada automática de stock [7]
        mov.setBodegaDestino(orden.getBodegaDestino());

        MovimientoDetalle detalle = new MovimientoDetalle();
        detalle.setProducto(orden.getProducto());
        detalle.setCantidad(orden.getCantidad());
        detalle.setMovimiento(mov);
        
        mov.setDetalles(List.of(detalle));

        // El repositorio de movimientos de inventario persistirá la entrada
        movimientoRepository.save(mov);
    }

    @Override
    @Transactional
    public void generarPdfOrden(Long id) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        byte[] pdfBytes = pdfGeneratorService.crearPdfOrden(orden);
        orden.setPdfData(pdfBytes);
        orden.setPdfFechaGeneracion(LocalDateTime.now());
        ordenCompraRepository.save(orden);
    }

    @Override
    public byte[] obtenerPdfOrden(Long id) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (orden.getPdfData() == null) {
            throw new ResourceNotFoundException("El PDF de la orden todavía no ha sido generado."); // Retorna 404 [9]
        }
        return orden.getPdfData();
    }
}