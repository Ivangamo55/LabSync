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

    /** Permite actualizar instalaciones existentes sin exigir reimportar toda la base. */
    public void crearTablaSiNoExiste(Connection conexion) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alertas ("
                + "id_alerta INT NOT NULL AUTO_INCREMENT, "
                + "tipo VARCHAR(40) NOT NULL, referencia VARCHAR(50) NOT NULL, "
                + "titulo VARCHAR(120) NOT NULL, detalle VARCHAR(500) NOT NULL, "
                + "prioridad VARCHAR(20) NOT NULL DEFAULT 'Media', "
                + "estado VARCHAR(20) NOT NULL DEFAULT 'Nueva', "
                + "fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "fecha_lectura TIMESTAMP NULL DEFAULT NULL, "
                + "fecha_atencion TIMESTAMP NULL DEFAULT NULL, "
                + "PRIMARY KEY (id_alerta), "
                + "UNIQUE KEY uk_alerta_origen (tipo, referencia), "
                + "KEY idx_alerta_estado_prioridad (estado, prioridad)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public List<Alerta> listar(Connection conexion, boolean incluirAtendidas) throws SQLException {
        String sql = "SELECT id_alerta, tipo, referencia, titulo, detalle, prioridad, estado, fecha_creacion "
                + "FROM alertas "
                + "WHERE tipo NOT IN ('RESERVA_APROBADA','RESERVA_RECHAZADA') "
                + "ORDER BY FIELD(estado, 'Nueva', 'Leida', 'Atendida'), "
                + "FIELD(prioridad, 'Critica', 'Alta', 'Media', 'Baja'), fecha_creacion DESC";
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
                "SELECT COUNT(*) FROM alertas")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Alerta> listarResolucionesReserva(Connection conexion, int idUsuario,
            String nombreCompleto, String rol) throws SQLException {
        String sql = "SELECT a.id_alerta, a.tipo, a.referencia, a.titulo, a.detalle, "
                + "a.prioridad, a.estado, a.fecha_creacion FROM alertas a "
                + "JOIN reservas r ON r.id_reserva=CAST(a.referencia AS UNSIGNED) "
                + "WHERE a.tipo IN ('RESERVA_APROBADA','RESERVA_RECHAZADA') "
                + "AND a.estado<>'Atendida' AND r.rol_solicitante=? "
                + "AND (r.id_usuario=? OR (r.id_usuario IS NULL AND r.nombre_solicitante=?)) "
                + "ORDER BY FIELD(a.estado,'Nueva','Leida'), a.fecha_creacion DESC";
        List<Alerta> alertas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, rol);
            ps.setInt(2, idUsuario);
            ps.setString(3, nombreCompleto);
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
        String sql = "UPDATE alertas SET estado = ?, fecha_lectura = CASE WHEN ? = 'Leida' "
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

    public void guardarGenerada(Connection conexion, String tipo, String referencia,
            String titulo, String detalle, String prioridad) throws SQLException {
        String sql = "INSERT INTO alertas (tipo, referencia, titulo, detalle, prioridad) "
                + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), "
                + "detalle = VALUES(detalle), prioridad = VALUES(prioridad)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.setString(2, referencia);
            ps.setString(3, titulo);
            ps.setString(4, detalle);
            ps.setString(5, prioridad);
            ps.executeUpdate();
        }
    }

    public void eliminarAlertasSinOrigenActivo(Connection conexion) throws SQLException {
        String[] consultas = {
            "DELETE a FROM alertas a WHERE a.tipo IN "
                + "('MANTENIMIENTO_PROXIMO','MANTENIMIENTO_VENCIDO','SOFTWARE_ACTUALIZACION') "
                + "AND NOT EXISTS (SELECT 1 FROM mantenimiento m "
                + "WHERE m.id_mantenimiento=CAST(a.referencia AS UNSIGNED) "
                + "AND m.estado IN ('Pendiente','En proceso') "
                + "AND ((a.tipo='MANTENIMIENTO_PROXIMO' AND m.fecha_programada>=CURDATE() "
                + "AND m.fecha_programada<=DATE_ADD(CURDATE(), INTERVAL 7 DAY)) "
                + "OR (a.tipo='MANTENIMIENTO_VENCIDO' AND m.fecha_programada<CURDATE()) "
                + "OR (a.tipo='SOFTWARE_ACTUALIZACION' AND m.fecha_programada "
                + "<=DATE_ADD(CURDATE(), INTERVAL 7 DAY))))",
            "DELETE a FROM alertas a WHERE a.tipo='MANTENIMIENTO_REQUERIDO' "
                + "AND NOT EXISTS (SELECT 1 FROM inventario i WHERE i.codigo=a.referencia "
                + "AND i.estado NOT IN ('Dado de baja','En mantenimiento') "
                + "AND (i.ultimo_mantenimiento IS NULL OR i.ultimo_mantenimiento "
                + "< DATE_SUB(CURDATE(), INTERVAL 180 DAY)) AND NOT EXISTS (SELECT 1 "
                + "FROM mantenimiento m WHERE m.codigo_equipo=i.codigo "
                + "AND m.estado IN ('Pendiente','En proceso')))",
            "DELETE a FROM alertas a WHERE a.tipo='FALLA_PENDIENTE' "
                + "AND NOT EXISTS (SELECT 1 FROM reporte_fallas f WHERE "
                + "f.id_falla=CAST(a.referencia AS UNSIGNED) "
                + "AND f.estado NOT IN ('Atendida','Cancelada'))",
            "DELETE a FROM alertas a WHERE a.tipo='RESERVA_PENDIENTE' "
                + "AND NOT EXISTS (SELECT 1 FROM reservas r WHERE "
                + "r.id_reserva=CAST(a.referencia AS UNSIGNED) "
                + "AND r.estado='Pendiente' AND r.fecha>=CURDATE())",
            "DELETE a FROM alertas a WHERE a.tipo='RESERVA_APROBADA' "
                + "AND NOT EXISTS (SELECT 1 FROM reservas r WHERE "
                + "r.id_reserva=CAST(a.referencia AS UNSIGNED) AND r.estado='Aprobada')",
            "DELETE a FROM alertas a WHERE a.tipo='RESERVA_RECHAZADA' "
                + "AND NOT EXISTS (SELECT 1 FROM reservas r WHERE "
                + "r.id_reserva=CAST(a.referencia AS UNSIGNED) AND r.estado='Rechazada')",
            "DELETE a FROM alertas a WHERE a.tipo IN ('EQUIPO_REVISION','EQUIPO_BAJA') "
                + "AND NOT EXISTS (SELECT 1 FROM inventario i WHERE i.codigo=a.referencia "
                + "AND i.estado='Con falla' AND ((a.tipo='EQUIPO_REVISION' AND "
                + "(SELECT COUNT(*) FROM reporte_fallas f WHERE "
                + "f.codigo_equipo COLLATE utf8mb4_unicode_ci=i.codigo "
                + "AND f.estado<>'Cancelada') < 3) OR (a.tipo='EQUIPO_BAJA' AND "
                + "(SELECT COUNT(*) FROM reporte_fallas f WHERE "
                + "f.codigo_equipo COLLATE utf8mb4_unicode_ci=i.codigo "
                + "AND f.estado<>'Cancelada') >= 3)))"
        };
        for (String sql : consultas) {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.executeUpdate();
            }
        }
    }
}
