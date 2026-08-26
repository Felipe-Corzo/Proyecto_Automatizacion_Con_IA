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
public class DistribucionStockDTO {

    private Long bodegaId;
    private String bodegaNombre;
    private Integer stockEnBodega;
    private BigDecimal porcentajeDelTotal;
    private BigDecimal valorEnBodega;
}
