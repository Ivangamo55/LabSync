package labsync.servicio;

import labsync.modelo.TiposMantenimiento;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

/** Ejecuta las escrituras de mantenimiento y de inventario en una sola transacción. */
public final class ServicioMantenimiento {

    private final ServicioDisponibilidad disponibilidadService = new ServicioDisponibilidad();

    public void guardar(Connection conexion, DatosMantenimiento datos, Integer idMantenimiento)
            throws SQLException, ConflictoMantenimientoException {
        validarDatos(datos);
        validarConflictoReservas(conexion, datos, idMantenimiento);
        if (idMantenimiento == null) {
            insertar(conexion, datos);
        } else {
            actualizar(conexion, datos, idMantenimiento);
        }
        actualizarInventario(conexion, datos.codigoEquipo, datos.estado,
                datos.tipoMantenimiento, "Realizado".equals(datos.estado));
    }

    public boolean finalizar(Connection conexion, int idMantenimiento, String codigoEquipo)
            throws SQLException {
        boolean autoCommitOriginal = conexion.getAutoCommit();
        try {
            if (autoCommitOriginal) {
                conexion.setAutoCommit(false);
            }
            String tipo = obtenerTipoParaFinalizar(conexion, idMantenimiento, codigoEquipo);
            actualizarEstado(conexion, idMantenimiento, "Realizado");
            actualizarInventario(conexion, codigoEquipo, "Realizado", tipo, true);
            if (autoCommitOriginal) {
                conexion.commit();
            }
            return true;
        } catch (SQLException ex) {
            conexion.rollback();
            throw ex;
        } finally {
            if (autoCommitOriginal) {
                conexion.setAutoCommit(true);
            }
        }
    }

    public void cancelar(Connection conexion, int idMantenimiento, String codigoEquipo)
            throws SQLException {
        actualizarEstado(conexion, idMantenimiento, "Cancelado");
        actualizarInventario(conexion, codigoEquipo, "Cancelado", null, false);
    }

    private void validarDatos(DatosMantenimiento datos) {
        if (TiposMantenimiento.requiereObservaciones(datos.tipoMantenimiento)
                && (datos.observaciones == null || datos.observaciones.isBlank())) {
            throw new IllegalArgumentException("Las observaciones son obligatorias para "
                    + datos.tipoMantenimiento + ".");
        }
    }

    private String obtenerTipoParaFinalizar(
            Connection conexion, int idMantenimiento, String codigoEquipo) throws SQLException {
        String sql = "SELECT m.tipo_mantenimiento FROM mantenimiento m "
                + "JOIN inventario i ON i.id_inventario=m.id_inventario "
                + "WHERE m.id_mantenimiento=? AND i.codigo=? FOR UPDATE";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idMantenimiento);
            ps.setString(2, codigoEquipo);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No se encontró el mantenimiento seleccionado.");
                }
                return rs.getString("tipo_mantenimiento");
            }
        }
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
        String sql = "INSERT INTO mantenimiento (id_inventario, tipo_mantenimiento, "
                + "fecha_programada, estado, responsable, observaciones) "
                + "SELECT id_inventario, ?, ?, ?, ?, ? FROM inventario WHERE codigo = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            asignarDatos(ps, datos);
            ps.executeUpdate();
        }
    }

    private void actualizar(Connection conexion, DatosMantenimiento datos, int id) throws SQLException {
        String sql = "UPDATE mantenimiento m JOIN inventario i ON i.codigo=? "
                + "SET m.id_inventario=i.id_inventario, m.tipo_mantenimiento=?, m.fecha_programada=?, "
                + "m.estado=?, m.responsable=?, m.observaciones=? WHERE m.id_mantenimiento=?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            asignarDatos(ps, datos);
            ps.setInt(7, id);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se encontró el mantenimiento seleccionado.");
            }
        }
    }

    private void asignarDatos(PreparedStatement ps, DatosMantenimiento datos) throws SQLException {
        ps.setString(1, datos.tipoMantenimiento);
        ps.setDate(2, Date.valueOf(datos.fechaProgramada));
        ps.setString(3, datos.estado);
        ps.setString(4, datos.responsable);
        ps.setString(5, datos.observaciones);
        ps.setString(6, datos.codigoEquipo);
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
            Connection conexion, String codigoEquipo, String estadoMantenimiento,
            String tipoMantenimiento, boolean registrarFecha)
            throws SQLException {
        String estadoInventario;
        if (esEstadoActivo(estadoMantenimiento)) {
            estadoInventario = "En mantenimiento";
        } else if ("Realizado".equals(estadoMantenimiento)
                && TiposMantenimiento.causaBaja(tipoMantenimiento)) {
            estadoInventario = "Baja";
        } else {
            estadoInventario = "Disponible";
        }
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
