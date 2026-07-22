package labsync.modelo;

import java.time.LocalTime;

/** Vista inmutable de una clase regular asignada a un laboratorio. */
public record HorarioClase(
        int id, String ciclo, int idProfesor, String profesor, String trayectoria,
        int cuatrimestre, String grupo, String turno, String materia,
        String dia, LocalTime horaInicio, LocalTime horaFin, String laboratorio) {

    public String intervalo() {
        return String.format("%02d:%02d - %02d:%02d", horaInicio.getHour(), horaInicio.getMinute(),
                horaFin.getHour(), horaFin.getMinute());
    }

    public String descripcion() {
        return materia + " | " + intervalo() + " | " + laboratorio;
    }
}
