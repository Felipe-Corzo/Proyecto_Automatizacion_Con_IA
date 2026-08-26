package com.logitrack.repository;

import com.logitrack.model.Rol;
import com.logitrack.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<Usuario> findByUsernameContainingIgnoreCase(String username);

    Page<Usuario> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    List<Usuario> findByRol(Rol rol);

    Page<Usuario> findByRol(Rol rol, Pageable pageable);
}
