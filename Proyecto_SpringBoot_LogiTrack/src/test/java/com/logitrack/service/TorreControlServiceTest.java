package com.logitrack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.logitrack.exception.BadRequestException;
import com.logitrack.model.EstadoOrden;
import com.logitrack.model.OrdenCompra;
import com.logitrack.model.Producto;
import com.logitrack.model.Proveedor;
import com.logitrack.repository.MovimientoInventarioRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.dto.CoberturaDTO;

@ExtendWith(MockitoExtension.class)
public class TorreControlServiceTest {

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @Mock
    private MovimientoInventarioService movimientoService;

    @InjectMocks
    private TorreControlServiceImpl torreControlService;


    // PRUEBA 1: Consumo 0 -> cobertura null y estado SIN_CONSUMO
    @Test
    @DisplayName("1. Cuando el consumo de los últimos 30 días es 0, la cobertura debe ser null y el estado SIN_CONSUMO")
    void testConsumoCeroRetornaCoberturaNullYSinConsumo() {
        // Simular que el repositorio de movimientos reporta 0 salidas para el producto ID 1
        when(movimientoRepository.calcularSalidasUltimos30Dias(eq(1L), any())).thenReturn(0);

        // Instanciar un producto de prueba
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Mouse Inalámbrico");

        // Ejecutar el cálculo del servicio
        CoberturaDTO resultado = torreControlService.calcularCoberturaProducto(producto);

        // Verificar resultados esperados
        assertNull(resultado.getDiasCobertura(), "Los días de cobertura deben ser null");
        assertEquals("SIN_CONSUMO", resultado.getEstadoCobertura(), "El estado debe ser SIN_CONSUMO");
    }

    // PRUEBA 2: Stock igual al punto de reorden -> no está en riesgo
    @Test
    @DisplayName("2. Cuando el stock es exactamente igual al punto de reorden, el producto no debe considerarse en riesgo")
    void testStockIgualAPuntoReordenNoEstaEnRiesgo() {
        // Proveedor con 10 días de entrega
        Proveedor prov = new Proveedor();
        prov.setDiasEntrega(10); // diasEntrega = 10

        // Producto con proveedor principal
        Producto producto = new Producto();
        producto.setId(2L);
        producto.setProveedorPrincipal(prov);

        // Simular que el consumo promedio diario es de 2 unidades
        // Punto de reorden = consumoPromedio (2) * diasEntrega (10) * 1.5 = 30 unidades
        when(movimientoRepository.calcularSalidasUltimos30Dias(eq(2L), any())).thenReturn(60); // 60 salidas en 30 días -> 2 diarias
        when(movimientoRepository.calcularStockActual(eq(2L))).thenReturn(30); // Stock actual = 30 (Igual al punto de reorden)

        boolean enRiesgo = torreControlService.evaluarProductoEnRiesgo(producto);

        // Si el stock es igual al punto de reorden, no está en riesgo (debe ser estrictamente menor)
        assertFalse(enRiesgo, "El producto no debe estar en riesgo si su stock es igual al punto de reorden");
    }

    // PRUEBA 3: Cantidad de orden 0 o negativa -> 400 Bad Request
    @Test
    @DisplayName("3. Intentar crear una orden de compra con cantidad 0 o negativa debe lanzar BadRequestException (400)")
    void testCrearOrdenCantidadInvalidaLanzaBadRequest() {
        OrdenCompra ordenInvalida = new OrdenCompra();
        ordenInvalida.setCantidad(0); // Cantidad inválida

        // Se espera que la validación del servicio lance un BadRequestException (mapeado a un HTTP 400)
        assertThrows(BadRequestException.class, () -> {
            torreControlService.crearOrdenCompra(ordenInvalida);
        }, "Debería lanzar BadRequestException cuando la cantidad es 0");
    }

    // PRUEBA 4: Orden cancelada -> no se puede aprobar (400 Bad Request)
    @Test
    @DisplayName("4. Una orden en estado CANCELADA no puede transicionar a APROBADA y debe lanzar BadRequestException (400)")
    void testAprobarOrdenCanceladaLanzaBadRequest() {
        OrdenCompra ordenCancelada = new OrdenCompra();
        ordenCancelada.setId(10L);
        ordenCancelada.setEstado(EstadoOrden.CANCELADA); // Estado actual: CANCELADA

        when(ordenCompraRepository.findById(10L)).thenReturn(Optional.of(ordenCancelada));

        // Transición inválida: CANCELADA -> APROBADA
        assertThrows(BadRequestException.class, () -> {
            torreControlService.cambiarEstadoOrden(10L, EstadoOrden.APROBADA);
        }, "No se puede aprobar una orden cancelada; debe lanzar error 400");
    }

    // PRUEBA 5: Orden aprobada a recibida -> genera entrada de forma transaccional
    @Test
    @DisplayName("5. Al cambiar el estado de APROBADA a RECIBIDA, se debe registrar automáticamente un movimiento de ENTRADA")
    void testOrdenAprobadaARecibidaGeneraMovimientoEntrada() {
        OrdenCompra ordenAprobada = new OrdenCompra();
        ordenAprobada.setId(15L);
        ordenAprobada.setEstado(EstadoOrden.APROBADA); // Estado actual: APROBADA
        ordenAprobada.setCantidad(100);
        
        Producto producto = new Producto();
        producto.setId(100L);
        ordenAprobada.setProducto(producto);

        when(ordenCompraRepository.findById(15L)).thenReturn(Optional.of(ordenAprobada));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(i -> i.getArgument(0));

        // Cambiar el estado a RECIBIDA
        OrdenCompra resultado = torreControlService.cambiarEstadoOrden(15L, EstadoOrden.RECIBIDA);

        assertEquals(EstadoOrden.RECIBIDA, resultado.getEstado(), "El estado de la orden debe cambiar a RECIBIDA");
        
        // Verificar que el repositorio de movimientos fue llamado exactamente una vez para guardar la ENTRADA de inventario
        verify(movimientoService, times(1)).registrarMovimiento(any());
    }
}
