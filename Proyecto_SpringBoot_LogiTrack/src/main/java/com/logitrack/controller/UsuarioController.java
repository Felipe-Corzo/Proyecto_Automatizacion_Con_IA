package com.logitrack.controller;

import com.logitrack.model.Rol;
import com.logitrack.model.Usuario;
import com.logitrack.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<?> obtenerTodos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Pageable pageable) {
        if (page != null || size != null) {
            return ResponseEntity.ok(usuarioService.obtenerTodos(pageable));
        }
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    // === NEW: Advanced Search ===
    @GetMapping("/search")
    public ResponseEntity<Page<Usuario>> buscarAvanzado(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Rol rol,
            Pageable pageable) {
        return ResponseEntity.ok(usuarioService.buscarAvanzado(username, email, rol, pageable));
    }

    // === NEW: Por username ===
    @GetMapping("/username")
    public ResponseEntity<Page<Usuario>> buscarPorUsername(
            @RequestParam String username, Pageable pageable) {
        return ResponseEntity.ok(usuarioService.buscarPorUsername(username, pageable));
    }

    // === NEW: Por rol ===
    @GetMapping("/rol/{rol}")
    public ResponseEntity<Page<Usuario>> buscarPorRol(@PathVariable Rol rol, Pageable pageable) {
        return ResponseEntity.ok(usuarioService.buscarPorRol(rol, pageable));
    }
}
