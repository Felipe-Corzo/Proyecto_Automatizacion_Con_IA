package com.logitrack.repository;

import com.logitrack.model.MovimientoDetalle;
import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.TipoMovimiento;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovimientoSpecification {

    public static Specification<MovimientoInventario> withFilters(
            TipoMovimiento tipo,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Long bodegaId,
            Long bodegaDestinoId,
            Long bodegaOrigenId,
            Long usuarioId,
            Long productoId) {

        return (Root<MovimientoInventario> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipoMovimiento"), tipo));
            }

            if (fechaDesde != null && fechaHasta != null) {
                predicates.add(cb.between(root.get("fecha"), fechaDesde, fechaHasta));
            } else if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
            } else if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
            }

            if (bodegaId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("bodegaOrigen").get("id"), bodegaId),
                        cb.equal(root.get("bodegaDestino").get("id"), bodegaId)
                ));
            } else {
                if (bodegaOrigenId != null) {
                    predicates.add(cb.equal(root.get("bodegaOrigen").get("id"), bodegaOrigenId));
                }
                if (bodegaDestinoId != null) {
                    predicates.add(cb.equal(root.get("bodegaDestino").get("id"), bodegaDestinoId));
                }
            }

            if (usuarioId != null) {
                predicates.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }

            if (productoId != null) {
                Join<MovimientoInventario, MovimientoDetalle> detallesJoin = root.join("detalles");
                predicates.add(cb.equal(detallesJoin.get("producto").get("id"), productoId));
                query.distinct(true);
            }

            query.orderBy(cb.desc(root.get("fecha")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
