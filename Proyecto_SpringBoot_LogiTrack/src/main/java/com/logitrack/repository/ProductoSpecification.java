package com.logitrack.repository;

import com.logitrack.model.InventarioBodega;
import com.logitrack.model.Producto;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductoSpecification {

    public static Specification<Producto> withFilters(
            String nombre,
            String categoria,
            Boolean bajoStock,
            BigDecimal precioMin,
            BigDecimal precioMax,
            Integer stockMin,
            Integer stockMax,
            Boolean sinStock,
            Boolean sinCategoria,
            Long bodegaId) {

        return (Root<Producto> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (nombre != null && !nombre.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")),
                        "%" + nombre.toLowerCase() + "%"));
            }

            if (categoria != null && !categoria.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("categoria")),
                        categoria.toLowerCase()));
            }

            if (bajoStock != null && bajoStock) {
                predicates.add(cb.lessThan(root.get("stock"), 10));
            }

            if (precioMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("precio"), precioMin));
            }

            if (precioMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precio"), precioMax));
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

            if (sinCategoria != null && sinCategoria) {
                predicates.add(cb.isNull(root.get("categoria")));
            }

            if (bodegaId != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<InventarioBodega> invRoot = subquery.from(InventarioBodega.class);
                subquery.select(invRoot.get("producto").get("id"));
                subquery.where(cb.equal(invRoot.get("bodega").get("id"), bodegaId));
                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Producto> orderBy(String sortBy, String sortDir) {
        return (Root<Producto> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (sortBy != null) {
                Path<?> path;
                switch (sortBy) {
                    case "precio": path = root.get("precio"); break;
                    case "stock": path = root.get("stock"); break;
                    case "nombre": path = root.get("nombre"); break;
                    case "categoria": path = root.get("categoria"); break;
                    default: path = root.get("id");
                }
                if ("desc".equalsIgnoreCase(sortDir)) {
                    query.orderBy(cb.desc(path));
                } else {
                    query.orderBy(cb.asc(path));
                }
            }
            return cb.conjunction();
        };
    }
}
