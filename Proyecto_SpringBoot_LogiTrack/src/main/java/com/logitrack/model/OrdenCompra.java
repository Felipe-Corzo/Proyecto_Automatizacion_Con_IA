package com.logitrack.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordenes_compra", schema = "proyecto")
@Getter
@Setter
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bodega_destino_id", nullable = false)
    private Bodega bodegaDestino;

    @NotNull
    @Min(value = 1, message = "La cantidad debe ser mayor a 0") // Validación TDD: no se admite cantidad <= 0
    private Integer cantidad;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    private BigDecimal total;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING) // Guarda el enum como texto en la base de datos (Ej: "BORRADOR")
    private EstadoOrden estado;

    @Column(name = "creado_por")
    private String creadoPor;

    @JsonIgnore // Evitar serialización en respuestas JSON
    @Lob // Define un objeto binario grande en la base de datos (Large Object) [1]
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "pdf_data", columnDefinition = "bytea") // Para almacenar el archivo PDF completo en bytes
    private byte[] pdfData;

    @JsonIgnore // Evitar serialización en respuestas JSON
    @Column(name = "pdf_fecha_generacion")
    private LocalDateTime pdfFechaGeneracion;
}