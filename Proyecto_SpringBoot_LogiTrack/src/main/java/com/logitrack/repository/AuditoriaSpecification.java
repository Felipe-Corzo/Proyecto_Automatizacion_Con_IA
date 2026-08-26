package com.logitrack.repository;

import com.logitrack.model.Auditoria;
import com.logitrack.model.TipoOperacion;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaSpecification {

    public static Specification<Auditoria> withFilters(
            String entidadAfectada,
            TipoOperacion tipoOperacion,
            Long usuarioId,
            Long entidadId,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta) {

        return (Root<Auditoria> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (entidadAfectada != null && !entidadAfectada.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("entidadAfectada")),
                        entidadAfectada.toLowerCase()));
            }

            if (tipoOperacion != null) {
                predicates.add(cb.equal(root.get("tipoOperacion"), tipoOperacion));
            }

            if (usuarioId != null) {
                predicates.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }

            if (entidadId != null) {
                predicates.add(cb.equal(root.get("entidadId"), entidadId));
            }

            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), fechaDesde));
            }

            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaHora"), fechaHasta));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
