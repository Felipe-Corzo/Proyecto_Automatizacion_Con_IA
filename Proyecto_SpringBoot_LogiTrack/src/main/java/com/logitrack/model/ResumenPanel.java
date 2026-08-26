package com.logitrack.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "resumenes_panel", schema = "proyecto")
@Getter
@Setter
public class ResumenPanel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) // Restricción: un único resumen válido por fecha [1, 2]
    private LocalDate fecha;

    @Column(columnDefinition = "TEXT", nullable = false) // Guarda el JSON completo como tipo TEXT en PostgreSQL
    private String contenidoJson;

    private String autor;
}