package com.logitrack.service;

import com.logitrack.model.Rol;
import com.logitrack.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UsuarioService {

    List<Usuario> obtenerTodos();

    Page<Usuario> obtenerTodos(Pageable pageable);

    Usuario obtenerPorId(Long id);

    // Advanced search
    Page<Usuario> buscarAvanzado(String username, String email, Rol rol, Pageable pageable);

    Page<Usuario> buscarPorUsername(String username, Pageable pageable);

    Page<Usuario> buscarPorRol(Rol rol, Pageable pageable);
}
