package labsync.labsync;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

/** Ejecuta las escrituras de mantenimiento y de inventario en una sola transacción. */
public final class MantenimientoService {

    private final DisponibilidadService disponibilidadService = new DisponibilidadService();

    public void guardar(Connection conexion, DatosMantenimiento datos, Integer idMantenimiento)
            throws SQLException, ConflictoMantenimientoException {
        validarConflictoReservas(conexion, datos, idMantenimiento);
        if (idMantenimiento == null) {
            insertar(conexion, datos);
        } else {
            actualizar(conexion, datos, idMantenimiento);
        }
        actualizarInventario(conexion, datos.codigoEquipo, datos.estado, false);
    }

    public void finalizar(Connection conexion, int idMantenimiento, String codigoEquipo)
            throws SQLException {
        actualizarEstado(conexion, idMantenimiento, "Realizado");
        actualizarInventario(conexion, codigoEquipo, "Realizado", true);
    }

    public void cancelar(Connection conexion, int idMantenimiento, String codigoEquipo)
            throws SQLException {
        actualizarEstado(conexion, idMantenimiento, "Cancelado");
        actualizarInventario(conexion, codigoEquipo, "Cancelado", false);
    }

    private void validarConflictoReservas(
            Connection conexion, DatosMantenimiento datos, Integer idMantenimiento)
            throws SQLException, ConflictoMantenimientoException {
        if (!esEstadoActivo(datos.estado)) {
            return;
        }
        if (disponibilidadService.existenReservasEnFecha(
                conexion, datos.laboratorio, datos.fechaProgramada, true)) {
            throw new ConflictoMantenimientoException(
                    "No se puede programar el mantenimiento porque existen reservas activas "
                    + "de alumnos o profesores para el laboratorio en esa fecha.");
        }
    }

    private void insertar(Connection conexion, DatosMantenimiento datos) throws SQLException {
        String sql = "INSERT INTO mantenimiento (codigo_equipo, nombre_equipo, laboratorio, "
                + "tipo_mantenimiento, fecha_programada, estado, responsable, observaciones) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            asignarDatos(ps, datos);
            ps.executeUpdate();
        }
    }

    private void actualizar(Connection conexion, DatosMantenimiento datos, int id) throws SQLException {
        String sql = "UPDATE mantenimiento SET codigo_equipo = ?, nombre_equipo = ?, laboratorio = ?, "
                + "tipo_mantenimiento = ?, fecha_programada = ?, estado = ?, responsable = ?, "
                + "observaciones = ? WHERE id_mantenimiento = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            asignarDatos(ps, datos);
            ps.setInt(9, id);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se encontró el mantenimiento seleccionado.");
            }
        }
    }

    private void asignarDatos(PreparedStatement ps, DatosMantenimiento datos) throws SQLException {
        ps.setString(1, datos.codigoEquipo);
        ps.setString(2, "N/A");
        ps.setString(3, datos.laboratorio);
        ps.setString(4, datos.tipoMantenimiento);
        ps.setDate(5, Date.valueOf(datos.fechaProgramada));
        ps.setString(6, datos.estado);
        ps.setString(7, datos.responsable);
        ps.setString(8, datos.observaciones);
    }

    private void actualizarEstado(Connection conexion, int id, String estado) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE mantenimiento SET estado = ? WHERE id_mantenimiento = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se encontró el mantenimiento seleccionado.");
            }
        }
    }

    private void actualizarInventario(
            Connection conexion, String codigoEquipo, String estadoMantenimiento, boolean registrarFecha)
            throws SQLException {
        String estadoInventario = esEstadoActivo(estadoMantenimiento)
                ? "En mantenimiento" : "Disponible";
        String sql = registrarFecha
                ? "UPDATE inventario SET estado = ?, ultimo_mantenimiento = CURDATE() WHERE codigo = ?"
                : "UPDATE inventario SET estado = ? WHERE codigo = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, estadoInventario);
            ps.setString(2, codigoEquipo);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se encontró el equipo en inventario.");
            }
        }
    }

    private boolean esEstadoActivo(String estado) {
        return "Pendiente".equals(estado) || "En proceso".equals(estado);
    }

    public static final class DatosMantenimiento {
        private final String codigoEquipo;
        private final String laboratorio;
        private final String tipoMantenimiento;
        private final LocalDate fechaProgramada;
        private final String estado;
        private final String responsable;
        private final String observaciones;

        public DatosMantenimiento(String codigoEquipo, String laboratorio, String tipoMantenimiento,
                LocalDate fechaProgramada, String estado, String responsable, String observaciones) {
            this.codigoEquipo = codigoEquipo;
            this.laboratorio = laboratorio;
            this.tipoMantenimiento = tipoMantenimiento;
            this.fechaProgramada = fechaProgramada;
            this.estado = estado;
            this.responsable = responsable;
            this.observaciones = observaciones;
        }
    }

    public static final class ConflictoMantenimientoException extends Exception {
        public ConflictoMantenimientoException(String mensaje) { super(mensaje); }
    }
}
