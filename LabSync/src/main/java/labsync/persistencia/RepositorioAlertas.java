package labsync.persistencia;

import labsync.modelo.Alerta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos de alertas. Todas las consultas usan parámetros. */
public final class RepositorioAlertas {

    public static String condicionAntiguedadMantenimiento(String diasSql) {
        return "DATEDIFF(CURDATE(), COALESCE(i.ultimo_mantenimiento, "
                + "DATE(i.fecha_registro))) > " + diasSql;
    }

    /** Verifica que la fuente de verdad haya sido instalada antes de usar alertas. */
    public void crearTablaSiNoExiste(Connection conexion) throws SQLException {
        String sql = "SELECT 1 FROM alertas LIMIT 0";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet ignored = ps.executeQuery()) {
            // Preparar y ejecutar la consulta basta para verificar que la tabla existe.
        }
    }

    public List<Alerta> listar(Connection conexion, boolean incluirAtendidas) throws SQLException {
        String sql = "SELECT id_alerta, tipo, referencia, titulo, detalle, prioridad, estado, fecha_creacion "
                + "FROM alertas "
                + "WHERE tipo NOT IN ('RESERVA_APROBADA','RESERVA_RECHAZADA') "
                + (incluirAtendidas ? "" : "AND estado <> 'Atendida' ")
                + "ORDER BY FIELD(estado, 'Nueva', 'Leída', 'Atendida'), "
                + "FIELD(prioridad, 'Crítica', 'Alta', 'Media', 'Baja'), fecha_creacion DESC";
        List<Alerta> alertas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp fecha = rs.getTimestamp("fecha_creacion");
                alertas.add(new Alerta(rs.getInt("id_alerta"), rs.getString("tipo"),
                        rs.getString("referencia"),
                        rs.getString("titulo"), rs.getString("detalle"),
                        rs.getString("prioridad"), rs.getString("estado"),
                        fecha == null ? null : fecha.toLocalDateTime()));
            }
        }
        return alertas;
    }

    public int contarNoLeidas(Connection conexion) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT COUNT(*) FROM alertas WHERE estado = 'Nueva'")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Alerta> listarResolucionesReserva(Connection conexion, int idUsuario,
            String nombreCompleto, String rol) throws SQLException {
        String sql = "SELECT a.id_alerta, a.tipo, a.referencia, a.titulo, a.detalle, "
                + "a.prioridad, a.estado, a.fecha_creacion FROM alertas a "
                + "JOIN reservas r ON r.id_reserva=a.id_reserva "
                + "JOIN usuario u ON u.id=r.id_usuario "
                + "WHERE a.tipo IN ('RESERVA_APROBADA','RESERVA_RECHAZADA') "
                + "AND a.estado<>'Atendida' AND u.rol=? AND r.id_usuario=? "
                + "ORDER BY FIELD(a.estado,'Nueva','Leída'), a.fecha_creacion DESC";
        List<Alerta> alertas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, rol);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp fecha = rs.getTimestamp("fecha_creacion");
                    alertas.add(new Alerta(rs.getInt("id_alerta"), rs.getString("tipo"),
                            rs.getString("referencia"), rs.getString("titulo"),
                            rs.getString("detalle"), rs.getString("prioridad"),
                            rs.getString("estado"), fecha == null ? null : fecha.toLocalDateTime()));
                }
            }
        }
        return alertas;
    }

    public void cambiarEstado(Connection conexion, int idAlerta, String estado) throws SQLException {
        String sql = "UPDATE alertas SET estado = ?, fecha_lectura = CASE WHEN ? = 'Leída' "
                + "THEN COALESCE(fecha_lectura, CURRENT_TIMESTAMP) ELSE fecha_lectura END, "
                + "fecha_atencion = CASE WHEN ? = 'Atendida' THEN CURRENT_TIMESTAMP "
                + "ELSE fecha_atencion END WHERE id_alerta = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, estado);
            ps.setString(3, estado);
            ps.setInt(4, idAlerta);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se encontró la alerta seleccionada.");
            }
        }
    }

    public void guardarGenerada(Connection conexion, Integer idReserva, Integer idFalla,
            Integer idMantenimiento, Integer idInventario, String tipo, String referencia,
            String titulo, String detalle, String prioridad) throws SQLException {
        int origenes = (idReserva == null ? 0 : 1) + (idFalla == null ? 0 : 1)
                + (idMantenimiento == null ? 0 : 1) + (idInventario == null ? 0 : 1);
        if (origenes != 1) {
            throw new IllegalArgumentException("Cada alerta debe tener exactamente un origen.");
        }
        String sql = "INSERT INTO alertas (id_reserva, id_falla, id_mantenimiento, "
                + "id_inventario, tipo, referencia, titulo, detalle, prioridad) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), "
                + "detalle = VALUES(detalle), prioridad = VALUES(prioridad)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            asignarEnteroNullable(ps, 1, idReserva);
            asignarEnteroNullable(ps, 2, idFalla);
            asignarEnteroNullable(ps, 3, idMantenimiento);
            asignarEnteroNullable(ps, 4, idInventario);
            ps.setString(5, tipo);
            ps.setString(6, referencia);
            ps.setString(7, titulo);
            ps.setString(8, detalle);
            ps.setString(9, prioridad);
            ps.executeUpdate();
        }
    }

    private void asignarEnteroNullable(PreparedStatement ps, int indice, Integer valor)
            throws SQLException {
        if (valor == null) ps.setNull(indice, java.sql.Types.INTEGER);
        else ps.setInt(indice, valor);
    }

    public void eliminarAlertasSinOrigenActivo(Connection conexion) throws SQLException {
        String[] consultas = {
            "DELETE FROM alertas WHERE tipo IN "
                + "('MANTENIMIENTO_PROXIMO','MANTENIMIENTO_VENCIDO','SOFTWARE_ACTUALIZACION')",
            "DELETE a FROM alertas a WHERE a.tipo IN "
                + "('ACTUALIZACION_SOFTWARE','ACTUALIZACION_HARDWARE','DISPOSICION_PELIGROSA',"
                + "'RETIRO_EQUIPO_OBSOLETO','MANTENIMIENTO_PREVENTIVO',"
                + "'MANTENIMIENTO_CORRECTIVO','MANTENIMIENTO_GENERAL') "
                + "AND NOT EXISTS (SELECT 1 FROM mantenimiento m "
                + "WHERE m.id_mantenimiento=a.id_mantenimiento "
                + "AND m.estado IN ('Pendiente','En proceso') "
                + "AND m.fecha_programada<=DATE_ADD(CURDATE(), INTERVAL 7 DAY))",
            "DELETE a FROM alertas a WHERE a.tipo='MANTENIMIENTO_REQUERIDO' "
                + "AND NOT EXISTS (SELECT 1 FROM inventario i WHERE i.id_inventario=a.id_inventario "
                + "AND i.estado NOT IN ('Baja','En mantenimiento') "
                + "AND " + condicionAntiguedadMantenimiento("180")
                + " AND NOT EXISTS (SELECT 1 "
                + "FROM mantenimiento m WHERE m.id_inventario=i.id_inventario "
                + "AND m.estado IN ('Pendiente','En proceso')))",
            "DELETE a FROM alertas a WHERE a.tipo='FALLA_PENDIENTE' "
                + "AND NOT EXISTS (SELECT 1 FROM reporte_fallas f WHERE "
                + "f.id_falla=a.id_falla "
                + "AND f.estado NOT IN ('Atendida','Cancelada'))",
            "DELETE a FROM alertas a WHERE a.tipo='RESERVA_PENDIENTE' "
                + "AND NOT EXISTS (SELECT 1 FROM reservas r WHERE "
                + "r.id_reserva=a.id_reserva "
                + "AND r.estado='Pendiente' AND r.fecha>=CURDATE())",
            "DELETE a FROM alertas a WHERE a.tipo='RESERVA_APROBADA' "
                + "AND NOT EXISTS (SELECT 1 FROM reservas r WHERE "
                + "r.id_reserva=a.id_reserva AND r.estado='Aprobada')",
            "DELETE a FROM alertas a WHERE a.tipo='RESERVA_RECHAZADA' "
                + "AND NOT EXISTS (SELECT 1 FROM reservas r WHERE "
                + "r.id_reserva=a.id_reserva AND r.estado='Rechazada')",
            "DELETE a FROM alertas a WHERE a.tipo IN ('EQUIPO_REVISION','EQUIPO_BAJA') "
                + "AND NOT EXISTS (SELECT 1 FROM inventario i WHERE i.id_inventario=a.id_inventario "
                + "AND i.estado='Con falla' AND ((a.tipo='EQUIPO_REVISION' AND "
                + "(SELECT COUNT(*) FROM reporte_fallas f WHERE "
                + "f.id_inventario=i.id_inventario "
                + "AND f.estado<>'Cancelada') < 3) OR (a.tipo='EQUIPO_BAJA' AND "
                + "(SELECT COUNT(*) FROM reporte_fallas f WHERE "
                + "f.id_inventario=i.id_inventario "
                + "AND f.estado<>'Cancelada') >= 3)))"
        };
        for (String sql : consultas) {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.executeUpdate();
            }
        }
    }
}
