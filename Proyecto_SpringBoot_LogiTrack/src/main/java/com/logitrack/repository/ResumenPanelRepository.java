package com.logitrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.logitrack.model.ResumenPanel;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ResumenPanelRepository extends JpaRepository<ResumenPanel, Long> {
    Optional<ResumenPanel> findByFecha(LocalDate fecha);
    Optional<ResumenPanel> findFirstByOrderByFechaDesc();
}
