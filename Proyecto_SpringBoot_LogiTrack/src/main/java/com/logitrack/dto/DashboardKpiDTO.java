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
public class DashboardKpiDTO {
    private long totalProductos;
    private long productosEnRiesgo;
    private long productosEnQuiebre;
    private BigDecimal valorTotalInventario;
    private BigDecimal ocupacionPromedioBodegas;
}
