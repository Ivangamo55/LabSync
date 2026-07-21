package labsync.labsync;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Centraliza las reglas de disponibilidad compartidas por reservas y mantenimiento. */
public final class DisponibilidadService {

    private static final String[] ESTADOS_RESERVA_ACTIVA = {"Pendiente", "Aprobada"};
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm");

    public ResultadoDisponibilidad consultarParaAlumno(
            Connection conexion, String laboratorio, LocalDate fecha, String horario, boolean bloquear)
            throws SQLException {
        return consultar(conexion, laboratorio, fecha, horario, false, bloquear, null);
    }

    public ResultadoDisponibilidad consultarParaProfesor(
            Connection conexion, String laboratorio, LocalDate fecha, String horario, boolean bloquear)
            throws SQLException {
        return consultar(conexion, laboratorio, fecha, horario, true, bloquear, null);
    }

    public ResultadoDisponibilidad validarAprobacion(
            Connection conexion, int idReserva, boolean bloquear) throws SQLException {
        String sql = "SELECT laboratorio, fecha, horario, rol_solicitante FROM reservas "
                + "WHERE id_reserva = ?" + clausulaBloqueo(bloquear);
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return ResultadoDisponibilidad.noDisponible(
                            "No se encontró la reserva seleccionada.", 0);
                }
                return consultar(
                        conexion,
                        rs.getString("laboratorio"),
                        rs.getDate("fecha").toLocalDate(),
                        rs.getString("horario"),
                        "Profesor".equalsIgnoreCase(rs.getString("rol_solicitante")),
                        bloquear,
                        idReserva);
            }
        }
    }

    public boolean existenReservasEnFecha(
            Connection conexion, String laboratorio, LocalDate fecha, boolean bloquear)
            throws SQLException {
        String sql = "SELECT id_reserva FROM reservas "
                + "WHERE laboratorio = ? AND fecha = ? "
                + "AND estado IN (?, ?) LIMIT 1" + clausulaBloqueo(bloquear);
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, laboratorio);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setString(3, ESTADOS_RESERVA_ACTIVA[0]);
            ps.setString(4, ESTADOS_RESERVA_ACTIVA[1]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private ResultadoDisponibilidad consultar(
            Connection conexion, String laboratorio, LocalDate fecha, String horario,
            boolean reservaProfesor, boolean bloquear, Integer idReservaExcluida) throws SQLException {
        int capacidad = consultarCapacidad(conexion, laboratorio, bloquear);
        if (capacidad <= 0) {
            return ResultadoDisponibilidad.noDisponible("El laboratorio no está disponible.", 0);
        }
        if (tieneMantenimientoActivo(conexion, laboratorio, fecha, bloquear)) {
            return ResultadoDisponibilidad.noDisponible(
                    "El laboratorio está bloqueado por mantenimiento para esa fecha.", 0);
        }

        Ocupacion ocupacion = consultarOcupacion(
                conexion, laboratorio, fecha, horario, bloquear, idReservaExcluida);
        if (ocupacion.hayReservaProfesor) {
            return ResultadoDisponibilidad.noDisponible(
                    "El laboratorio ya tiene una reserva de profesor en ese horario.", 0);
        }
        if (reservaProfesor && ocupacion.reservasAlumno > 0) {
            return ResultadoDisponibilidad.noDisponible(
                    "Existen reservas activas de alumnos en ese horario.", 0);
        }

        int equiposDisponibles = Math.max(0, capacidad - ocupacion.reservasAlumno);
        if (!reservaProfesor && equiposDisponibles == 0) {
            return ResultadoDisponibilidad.noDisponible(
                    "No hay computadoras disponibles en ese horario.", 0);
        }
        return ResultadoDisponibilidad.disponible(equiposDisponibles, capacidad);
    }

    private int consultarCapacidad(Connection conexion, String laboratorio, boolean bloquear)
            throws SQLException {
        String sql = "SELECT total_equipos FROM laboratorios "
                + "WHERE nombre = ? AND estado = 'Disponible'" + clausulaBloqueo(bloquear);
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, laboratorio);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total_equipos") : 0;
            }
        }
    }

    private boolean tieneMantenimientoActivo(
            Connection conexion, String laboratorio, LocalDate fecha, boolean bloquear)
            throws SQLException {
        String sql = "SELECT id_mantenimiento FROM mantenimiento "
                + "WHERE laboratorio = ? AND estado IN ('Pendiente', 'En proceso') "
                + "AND fecha_programada <= ? "
                + "LIMIT 1" + clausulaBloqueo(bloquear);
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, laboratorio);
            ps.setDate(2, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Ocupacion consultarOcupacion(
            Connection conexion, String laboratorio, LocalDate fecha, String horario, boolean bloquear,
            Integer idReservaExcluida)
            throws SQLException {
        String sql = "SELECT rol_solicitante, horario FROM reservas "
                + "WHERE laboratorio = ? AND fecha = ? AND estado IN (?, ?) "
                + (idReservaExcluida == null ? "" : "AND id_reserva <> ? ")
                + clausulaBloqueo(bloquear);
        Ocupacion ocupacion = new Ocupacion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, laboratorio);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setString(3, ESTADOS_RESERVA_ACTIVA[0]);
            ps.setString(4, ESTADOS_RESERVA_ACTIVA[1]);
            if (idReservaExcluida != null) {
                ps.setInt(5, idReservaExcluida);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!horariosSeTraslapan(horario, rs.getString("horario"))) {
                        continue;
                    }
                    if ("Profesor".equalsIgnoreCase(rs.getString("rol_solicitante"))) {
                        ocupacion.hayReservaProfesor = true;
                    } else {
                        ocupacion.reservasAlumno++;
                    }
                }
            }
        }
        return ocupacion;
    }

    private boolean horariosSeTraslapan(String primero, String segundo) {
        try {
            LocalTime[] intervaloPrimero = obtenerIntervalo(primero);
            LocalTime[] intervaloSegundo = obtenerIntervalo(segundo);
            return intervaloPrimero[0].isBefore(intervaloSegundo[1])
                    && intervaloSegundo[0].isBefore(intervaloPrimero[1]);
        } catch (IllegalArgumentException ex) {
            return primero != null && primero.equalsIgnoreCase(segundo);
        }
    }

    private LocalTime[] obtenerIntervalo(String horario) {
        if (horario == null) {
            throw new IllegalArgumentException("Horario vacío");
        }
        String[] partes = horario.trim().split("\\s*-\\s*");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Horario inválido");
        }
        try {
            return new LocalTime[]{
                LocalTime.parse(partes[0], FORMATO_HORA),
                LocalTime.parse(partes[1], FORMATO_HORA)
            };
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Horario inválido", ex);
        }
    }

    private String clausulaBloqueo(boolean bloquear) {
        return bloquear ? " FOR UPDATE" : "";
    }

    private static final class Ocupacion {
        private boolean hayReservaProfesor;
        private int reservasAlumno;
    }

    public static final class ResultadoDisponibilidad {
        private final boolean disponible;
        private final int equiposDisponibles;
        private final int capacidad;
        private final String mensaje;

        private ResultadoDisponibilidad(
                boolean disponible, int equiposDisponibles, int capacidad, String mensaje) {
            this.disponible = disponible;
            this.equiposDisponibles = equiposDisponibles;
            this.capacidad = capacidad;
            this.mensaje = mensaje;
        }

        public static ResultadoDisponibilidad disponible(int equiposDisponibles, int capacidad) {
            return new ResultadoDisponibilidad(true, equiposDisponibles, capacidad, "Disponible");
        }

        public static ResultadoDisponibilidad noDisponible(String mensaje, int capacidad) {
            return new ResultadoDisponibilidad(false, 0, capacidad, mensaje);
        }

        public boolean estaDisponible() { return disponible; }
        public int getEquiposDisponibles() { return equiposDisponibles; }
        public int getCapacidad() { return capacidad; }
        public String getMensaje() { return mensaje; }
    }
}
