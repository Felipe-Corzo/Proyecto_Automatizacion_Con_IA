package com.logitrack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.logitrack.dto.ResumenPanelDTO;
import com.logitrack.exception.BadRequestException;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ResumenPanelRepository;

@ExtendWith(MockitoExtension.class)
public class ResumenPanelServiceTest {

    @Mock
    private ResumenPanelRepository resumenPanelRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private BodegaRepository bodegaRepository;

    @InjectMocks
    private ResumenPanelServiceImpl resumenPanelService;

    // PRUEBA 7: Resumen con severidad inválida o ID inexistente -> 400 Bad Request
    @Test
    @DisplayName("7. Publicar un resumen con severidad inválida o un ID de producto que no existe debe lanzar BadRequestException (400)")
    void testPublicarResumenConDatosInvalidosLanzaBadRequest() {
        ResumenPanelDTO resumenInvalido = new ResumenPanelDTO();
        resumenInvalido.setFecha("2026-08-25");
        resumenInvalido.setNarrativa("Hay alertas críticas en el sistema de inventarios.");
        
        // Alerta con ID de producto inexistente (ID = 999)
        ResumenPanelDTO.Alerta alertaInvalida = new ResumenPanelDTO.Alerta();
        alertaInvalida.setSeveridad("SUPER_CRITICA"); // Severidad no permitida (Solo BAJA, MEDIA, ALTA)
        alertaInvalida.setProductoId(999L);
        alertaInvalida.setBodegaId(1L);
        
        resumenInvalido.setAlertas(Collections.singletonList(alertaInvalida));
        resumenInvalido.setAccionesSugeridas(Collections.emptyList());

        // Simular que el producto 999L no existe en la base de datos
        when(productoRepository.existsById(999L)).thenReturn(false);

        // Se espera que falle con código 400
        assertThrows(BadRequestException.class, () -> {
            resumenPanelService.publicarResumen(resumenInvalido);
        }, "Debería lanzar BadRequestException por severidad inválida o ID inexistente");

        // Verificar que no se guardó ningún cambio en el repositorio de resumenes
        verify(resumenPanelRepository, never()).save(any());
    }
}