package com.logitrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValorInventarioBodegaDTO {

    private Long bodegaId;
    private String bodegaNombre;
    private Long totalProductos;
    private Long totalUnidades;
    private BigDecimal valorTotalInventario;
    private BigDecimal capacidadOcupacion;
}
