package labsync.modelo;

import java.time.LocalDateTime;

/** Datos de una alerta persistida. */
public record Alerta(
        int id, String tipo, String referencia, String titulo, String detalle, String prioridad,
        String estado, LocalDateTime fechaCreacion) {
}
