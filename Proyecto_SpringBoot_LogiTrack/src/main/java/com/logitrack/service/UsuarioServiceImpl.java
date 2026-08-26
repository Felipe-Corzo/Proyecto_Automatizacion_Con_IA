package com.logitrack.service;

import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.Rol;
import com.logitrack.model.Usuario;
import com.logitrack.repository.UsuarioRepository;
import com.logitrack.repository.UsuarioSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Page<Usuario> obtenerTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    @Override
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    @Override
    public Page<Usuario> buscarAvanzado(String username, String email, Rol rol, Pageable pageable) {
        Specification<Usuario> spec = UsuarioSpecification.withFilters(username, email, rol);
        return usuarioRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Usuario> buscarPorUsername(String username, Pageable pageable) {
        return usuarioRepository.findByUsernameContainingIgnoreCase(username, pageable);
    }

    @Override
    public Page<Usuario> buscarPorRol(Rol rol, Pageable pageable) {
        return usuarioRepository.findByRol(rol, pageable);
    }
}
