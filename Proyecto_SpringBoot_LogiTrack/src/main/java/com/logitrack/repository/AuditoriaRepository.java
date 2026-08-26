package com.logitrack.repository;

import com.logitrack.model.Auditoria;
import com.logitrack.model.TipoOperacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {

    List<Auditoria> findByEntidadAfectadaIgnoreCase(String entidadAfectada);

    Page<Auditoria> findByEntidadAfectadaIgnoreCase(String entidadAfectada, Pageable pageable);

    List<Auditoria> findByTipoOperacion(TipoOperacion tipoOperacion);

    Page<Auditoria> findByTipoOperacion(TipoOperacion tipoOperacion, Pageable pageable);

    List<Auditoria> findByUsuario_Id(Long usuarioId);

    Page<Auditoria> findByUsuario_Id(Long usuarioId, Pageable pageable);

    List<Auditoria> findAllByOrderByFechaHoraDesc();

    Page<Auditoria> findAllByOrderByFechaHoraDesc(Pageable pageable);

    List<Auditoria> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);

    Page<Auditoria> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    List<Auditoria> findByEntidadId(Long entidadId);

    Page<Auditoria> findByEntidadId(Long entidadId, Pageable pageable);
}
