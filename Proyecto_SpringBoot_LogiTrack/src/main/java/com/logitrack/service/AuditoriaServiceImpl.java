package com.logitrack.service;

import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.Auditoria;
import com.logitrack.model.TipoOperacion;
import com.logitrack.repository.AuditoriaRepository;
import com.logitrack.repository.AuditoriaSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuditoriaServiceImpl implements AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaServiceImpl(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Override
    public List<Auditoria> obtenerTodas() {
        return auditoriaRepository.findAllByOrderByFechaHoraDesc();
    }

    @Override
    public Page<Auditoria> obtenerTodas(Pageable pageable) {
        return auditoriaRepository.findAllByOrderByFechaHoraDesc(pageable);
    }

    @Override
    public Auditoria obtenerPorId(Long id) {
        return auditoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auditoria", "id", id));
    }

    @Override
    public List<Auditoria> buscarPorEntidad(String entidad) {
        return auditoriaRepository.findByEntidadAfectadaIgnoreCase(entidad);
    }

    @Override
    public Page<Auditoria> buscarPorEntidad(String entidad, Pageable pageable) {
        return auditoriaRepository.findByEntidadAfectadaIgnoreCase(entidad, pageable);
    }

    @Override
    public List<Auditoria> buscarPorUsuario(Long usuarioId) {
        return auditoriaRepository.findByUsuario_Id(usuarioId);
    }

    @Override
    public Page<Auditoria> buscarPorUsuario(Long usuarioId, Pageable pageable) {
        return auditoriaRepository.findByUsuario_Id(usuarioId, pageable);
    }

    @Override
    public List<Auditoria> buscarPorTipoOperacion(TipoOperacion tipo) {
        return auditoriaRepository.findByTipoOperacion(tipo);
    }

    @Override
    public Page<Auditoria> buscarPorTipoOperacion(TipoOperacion tipo, Pageable pageable) {
        return auditoriaRepository.findByTipoOperacion(tipo, pageable);
    }

    // === Advanced Search Methods ===

    @Override
    public Page<Auditoria> buscarAvanzado(String entidad, TipoOperacion operacion, Long usuarioId,
                                           LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                           Long entidadId, Pageable pageable) {
Specification<Auditoria> spec = AuditoriaSpecification.withFilters(
                entidad, operacion, usuarioId, entidadId, fechaDesde, fechaHasta);
        return auditoriaRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Auditoria> buscarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        return auditoriaRepository.findByFechaHoraBetween(desde, hasta, pageable);
    }

    @Override
    public Page<Auditoria> buscarPorEntidadId(Long entidadId, Pageable pageable) {
        return auditoriaRepository.findByEntidadId(entidadId, pageable);
    }
}
