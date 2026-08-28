package com.logitrack.service;

import com.logitrack.dto.CoberturaDTO;
import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.*;
import com.logitrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private MovimientoInventarioService movimientoService;

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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
        normalizarDetalles(orden);
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setEstado(EstadoOrden.BORRADOR);
        orden.setTotal(calcularTotal(orden));
        return ordenCompraRepository.save(orden);
    }

    @Override
    @Transactional
    public OrdenCompra actualizarOrdenCompra(Long id, OrdenCompra ordenActualizada) {
        OrdenCompra existente = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada"));
        if (existente.getEstado() != EstadoOrden.BORRADOR) {
            throw new BadRequestException("Solo se pueden editar órdenes en estado BORRADOR.");
        }
        normalizarDetalles(ordenActualizada);
        existente.setProveedor(ordenActualizada.getProveedor());
        existente.setBodegaDestino(ordenActualizada.getBodegaDestino());
        existente.getDetalles().clear();
        for (OrdenCompraDetalle detalle : ordenActualizada.getDetalles()) {
            detalle.setOrdenCompra(existente);
            existente.getDetalles().add(detalle);
        }
        OrdenCompraDetalle primero = existente.getDetalles().get(0);
        existente.setProducto(primero.getProducto());
        existente.setCantidad(primero.getCantidad());
        existente.setPrecioUnitario(primero.getPrecioUnitario());
        existente.setTotal(calcularTotal(existente));
        existente.setPdfData(null);
        existente.setPdfFechaGeneracion(null);
        return ordenCompraRepository.save(existente);
    }

    private void normalizarDetalles(OrdenCompra orden) {
        if (orden.getDetalles() == null || orden.getDetalles().isEmpty()) {
            if (orden.getProducto() == null || orden.getCantidad() == null || orden.getPrecioUnitario() == null
                    || orden.getCantidad() <= 0) {
                throw new BadRequestException("La orden debe contener al menos una línea válida.");
            }
            OrdenCompraDetalle detalle = new OrdenCompraDetalle();
            detalle.setProducto(orden.getProducto());
            detalle.setCantidad(orden.getCantidad());
            detalle.setPrecioUnitario(orden.getPrecioUnitario());
            orden.getDetalles().add(detalle);
        }
        for (OrdenCompraDetalle detalle : orden.getDetalles()) {
            if (detalle == null || detalle.getProducto() == null || detalle.getCantidad() == null
                    || detalle.getCantidad() <= 0 || detalle.getPrecioUnitario() == null) {
                throw new BadRequestException("Cada línea requiere producto, cantidad y precio unitario válidos.");
            }
            detalle.setOrdenCompra(orden);
        }
        OrdenCompraDetalle primero = orden.getDetalles().get(0);
        orden.setProducto(primero.getProducto());
        orden.setCantidad(primero.getCantidad());
        orden.setPrecioUnitario(primero.getPrecioUnitario());
    }

    private BigDecimal calcularTotal(OrdenCompra orden) {
        return orden.getDetalles().stream()
                .map(detalle -> detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional // Hace que la actualización de la orden y la inserción del movimiento ocurran en un solo bloque atómico [7]
    public OrdenCompra cambiarEstadoOrden(Long id, EstadoOrden nuevoEstado) {
        if (nuevoEstado == null) {
            throw new BadRequestException("El nuevo estado no puede ser nulo");
        }
        
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada"));

        EstadoOrden estadoActual = orden.getEstado();
        
        if (estadoActual == null) {
            throw new BadRequestException("La orden no tiene un estado definido");
        }

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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("El usuario autenticado no existe."));
            mov.setUsuario(usuario);
        }

        List<MovimientoDetalle> detalles = orden.getDetalles().stream().map(ordenDetalle -> {
            MovimientoDetalle detalle = new MovimientoDetalle();
            detalle.setProducto(ordenDetalle.getProducto());
            detalle.setCantidad(ordenDetalle.getCantidad());
            detalle.setMovimiento(mov);
            return detalle;
        }).toList();
        if (detalles.isEmpty()) {
            MovimientoDetalle detalle = new MovimientoDetalle();
            detalle.setProducto(orden.getProducto());
            detalle.setCantidad(orden.getCantidad());
            detalle.setMovimiento(mov);
            detalles = List.of(detalle);
        }
        mov.setDetalles(detalles);

        // El repositorio de movimientos de inventario persistirá la entrada
            movimientoService.registrarMovimiento(mov);
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