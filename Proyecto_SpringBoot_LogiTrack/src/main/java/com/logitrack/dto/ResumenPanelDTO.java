package com.logitrack.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ResumenPanelDTO {
    private String fecha;
    private String narrativa;
    private List<Alerta> alertas;
    private List<AccionSugerida> accionesSugeridas;

    @Getter
    @Setter
    public static class Alerta {
        private String severidad;
        private String titulo;
        private String detalle;
        private Long productoId;
        private Long ordenId;
        private Long bodegaId;
    }

    @Getter
    @Setter
    public static class AccionSugerida {
        private String tipo;
        private String descripcion;
        private Long ordenId;
        private Long productoId;
        private Long bodegaId;
    }
}