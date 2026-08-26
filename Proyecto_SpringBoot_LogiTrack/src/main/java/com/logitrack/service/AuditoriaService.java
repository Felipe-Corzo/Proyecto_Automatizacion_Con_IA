package com.logitrack.service;

import com.logitrack.model.Auditoria;
import com.logitrack.model.TipoOperacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaService {
    List<Auditoria> obtenerTodas();

    Page<Auditoria> obtenerTodas(Pageable pageable);

    Auditoria obtenerPorId(Long id);

    List<Auditoria> buscarPorEntidad(String entidad);

    Page<Auditoria> buscarPorEntidad(String entidad, Pageable pageable);

    List<Auditoria> buscarPorUsuario(Long usuarioId);

    Page<Auditoria> buscarPorUsuario(Long usuarioId, Pageable pageable);

    List<Auditoria> buscarPorTipoOperacion(TipoOperacion tipo);

    Page<Auditoria> buscarPorTipoOperacion(TipoOperacion tipo, Pageable pageable);

    // Advanced search
    Page<Auditoria> buscarAvanzado(String entidad, TipoOperacion operacion, Long usuarioId,
                                    LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                    Long entidadId, Pageable pageable);

    Page<Auditoria> buscarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Auditoria> buscarPorEntidadId(Long entidadId, Pageable pageable);
}
