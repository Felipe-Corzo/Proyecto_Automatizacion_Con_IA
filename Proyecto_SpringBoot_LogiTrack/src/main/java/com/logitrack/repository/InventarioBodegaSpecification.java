package com.logitrack.repository;

import com.logitrack.model.InventarioBodega;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class InventarioBodegaSpecification {

    public static Specification<InventarioBodega> withFilters(
            Long bodegaId,
            Long productoId,
            Integer stockMin,
            Integer stockMax,
            Boolean sinStock) {

        return (Root<InventarioBodega> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (bodegaId != null) {
                predicates.add(cb.equal(root.get("bodega").get("id"), bodegaId));
            }

            if (productoId != null) {
                predicates.add(cb.equal(root.get("producto").get("id"), productoId));
            }

            if (stockMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("stock"), stockMin));
            }

            if (stockMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("stock"), stockMax));
            }

            if (sinStock != null && sinStock) {
                predicates.add(cb.equal(root.get("stock"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
