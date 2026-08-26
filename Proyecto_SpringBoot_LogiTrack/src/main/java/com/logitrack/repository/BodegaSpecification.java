package com.logitrack.repository;

import com.logitrack.model.Bodega;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BodegaSpecification {

    public static Specification<Bodega> withFilters(
            String nombre,
            String ubicacion,
            Boolean sinEncargado,
            Long capacidadMin,
            Long capacidadMax) {

        return (Root<Bodega> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (nombre != null && !nombre.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")),
                        "%" + nombre.toLowerCase() + "%"));
            }

            if (ubicacion != null && !ubicacion.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("ubicacion")),
                        "%" + ubicacion.toLowerCase() + "%"));
            }

            if (sinEncargado != null && sinEncargado) {
                predicates.add(cb.isNull(root.get("encargado")));
            }

            if (capacidadMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("capacidad"), capacidadMin));
            }

            if (capacidadMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("capacidad"), capacidadMax));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
