package com.logitrack.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.dto.ResumenPanelDTO;
import com.logitrack.exception.BadRequestException;
import com.logitrack.model.ResumenPanel;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ResumenPanelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ResumenPanelServiceImpl implements ResumenPanelService {

    private static final Set<String> SEVERIDADES_VALIDAS = Set.of("BAJA", "MEDIA", "ALTA");

    private final ResumenPanelRepository resumenPanelRepository;
    private final ProductoRepository productoRepository;
    private final BodegaRepository bodegaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumenPanelServiceImpl(ResumenPanelRepository resumenPanelRepository,
                                   ProductoRepository productoRepository,
                                   BodegaRepository bodegaRepository) {
        this.resumenPanelRepository = resumenPanelRepository;
        this.productoRepository = productoRepository;
        this.bodegaRepository = bodegaRepository;
    }

    @Override
    public ResumenPanel publicarResumen(ResumenPanelDTO resumenDTO) {
        validarResumen(resumenDTO);

        LocalDate fecha = parsearFecha(resumenDTO.getFecha());
        ResumenPanel resumen = resumenPanelRepository.findByFecha(fecha)
                .orElseGet(ResumenPanel::new);
        resumen.setFecha(fecha);
        resumen.setContenidoJson(serializar(resumenDTO));
        return resumenPanelRepository.save(resumen);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenPanel obtenerUltimoResumen() {
        return resumenPanelRepository.findFirstByOrderByFechaDesc()
                .orElseThrow(() -> new BadRequestException("No existe un resumen publicado."));
    }

    private void validarResumen(ResumenPanelDTO resumenDTO) {
        if (resumenDTO == null) {
            throw new BadRequestException("El resumen es obligatorio.");
        }
        parsearFecha(resumenDTO.getFecha());
        if (resumenDTO.getNarrativa() == null || resumenDTO.getNarrativa().isBlank()) {
            throw new BadRequestException("La narrativa del resumen es obligatoria.");
        }

        validarAlertas(resumenDTO.getAlertas());
        validarAcciones(resumenDTO.getAccionesSugeridas());
    }

    private void validarAlertas(List<ResumenPanelDTO.Alerta> alertas) {
        if (alertas == null) {
            return;
        }
        for (ResumenPanelDTO.Alerta alerta : alertas) {
            if (alerta == null) {
                throw new BadRequestException("La severidad de la alerta no es válida.");
            }
            validarProducto(alerta.getProductoId());
            validarBodega(alerta.getBodegaId());
            if (!SEVERIDADES_VALIDAS.contains(alerta.getSeveridad())) {
                throw new BadRequestException("La severidad de la alerta no es válida.");
            }
        }
    }

    private void validarAcciones(List<ResumenPanelDTO.AccionSugerida> acciones) {
        if (acciones == null) {
            return;
        }
        for (ResumenPanelDTO.AccionSugerida accion : acciones) {
            if (accion == null) {
                throw new BadRequestException("La acción sugerida no es válida.");
            }
            validarProducto(accion.getProductoId());
            validarBodega(accion.getBodegaId());
        }
    }

    private void validarProducto(Long id) {
        if (id != null && !productoRepository.existsById(id)) {
            throw new BadRequestException("El producto indicado no existe.");
        }
    }

    private void validarBodega(Long id) {
        if (id != null && !bodegaRepository.existsById(id)) {
            throw new BadRequestException("La bodega indicada no existe.");
        }
    }

    private LocalDate parsearFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            throw new BadRequestException("La fecha del resumen es obligatoria.");
        }
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("La fecha debe tener formato YYYY-MM-DD.");
        }
    }

    private String serializar(ResumenPanelDTO resumenDTO) {
        try {
            return objectMapper.writeValueAsString(resumenDTO);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el resumen.", exception);
        }
    }
}
