package com.logitrack.repository;

import com.logitrack.model.Rol;
import com.logitrack.model.Usuario;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UsuarioSpecification {

    public static Specification<Usuario> withFilters(
            String username,
            String email,
            Rol rol) {

        return (Root<Usuario> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("username")),
                        "%" + username.toLowerCase() + "%"));
            }

            if (email != null && !email.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"));
            }

            if (rol != null) {
                predicates.add(cb.equal(root.get("rol"), rol));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
