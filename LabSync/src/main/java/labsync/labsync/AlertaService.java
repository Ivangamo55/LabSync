package labsync.labsync;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Genera y administra las alertas automáticas del laboratorista. */
public final class AlertaService {
    public static final int DIAS_MANTENIMIENTO_PROXIMO = 7;
    public static final int FALLAS_PARA_SUGERIR_BAJA = 3;
    public static final int DIAS_SIN_MANTENIMIENTO = 180;

    private final AlertaDAO alertaDAO = new AlertaDAO();

    public void sincronizar(Connection conexion) throws SQLException {
        // El DDL se ejecuta antes de la transacción porque MySQL/MariaDB realizan
        // commit implícito al crear tablas. CREATE IF NOT EXISTS es idempotente.
        alertaDAO.crearTablaSiNoExiste(conexion);
        boolean autoCommitOriginal = conexion.getAutoCommit();
        try {
            conexion.setAutoCommit(false);
            generarMantenimientos(conexion);
            generarMantenimientosRequeridos(conexion);
            generarFallas(conexion);
            generarEquipos(conexion);
            // Los avisos cuyo origen ya no está activo se eliminan. Si el problema
            // reaparece más adelante podrá generarse de nuevo como una alerta Nueva.
            alertaDAO.eliminarAlertasSinOrigenActivo(conexion);
            conexion.commit();
        } catch (SQLException ex) {
            conexion.rollback();
            throw ex;
        } finally {
            conexion.setAutoCommit(autoCommitOriginal);
        }
    }

    public List<Alerta> listar(Connection conexion) throws SQLException {
        return listar(conexion, false);
    }

    public List<Alerta> listar(Connection conexion, boolean incluirAtendidas) throws SQLException {
        return alertaDAO.listar(conexion, incluirAtendidas);
    }

    public int contarNoLeidas(Connection conexion) throws SQLException {
        return alertaDAO.contarNoLeidas(conexion);
    }

    public void marcarLeida(Connection conexion, int idAlerta) throws SQLException {
        cambiarEstadoEnTransaccion(conexion, idAlerta, "Leida");
    }

    public void marcarAtendida(Connection conexion, int idAlerta) throws SQLException {
        cambiarEstadoEnTransaccion(conexion, idAlerta, "Atendida");
    }

    private void cambiarEstadoEnTransaccion(Connection conexion, int id, String estado)
            throws SQLException {
        boolean autoCommitOriginal = conexion.getAutoCommit();
        try {
            conexion.setAutoCommit(false);
            alertaDAO.cambiarEstado(conexion, id, estado);
            conexion.commit();
        } catch (SQLException ex) {
            conexion.rollback();
            throw ex;
        } finally {
            conexion.setAutoCommit(autoCommitOriginal);
        }
    }

    private void generarMantenimientos(Connection conexion) throws SQLException {
        String sql = "SELECT id_mantenimiento, codigo_equipo, nombre_equipo, laboratorio, "
                + "tipo_mantenimiento, fecha_programada, "
                + "DATEDIFF(fecha_programada, CURDATE()) dias "
                + "FROM mantenimiento WHERE estado IN ('Pendiente','En proceso') "
                + "AND fecha_programada <= DATE_ADD(CURDATE(), INTERVAL ? DAY)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, DIAS_MANTENIMIENTO_PROXIMO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int dias = rs.getInt("dias");
                    boolean vencido = dias < 0;
                    String equipo = nombreEquipo(rs.getString("codigo_equipo"), rs.getString("nombre_equipo"));
                    String tipoMantenimiento = rs.getString("tipo_mantenimiento");
                    boolean actualizacion = tipoMantenimiento != null
                            && tipoMantenimiento.toLowerCase().contains("actualiz");
                    alertaDAO.guardarGenerada(conexion,
                            actualizacion ? "SOFTWARE_ACTUALIZACION"
                                    : (vencido ? "MANTENIMIENTO_VENCIDO" : "MANTENIMIENTO_PROXIMO"),
                            String.valueOf(rs.getInt("id_mantenimiento")),
                            actualizacion ? (vencido ? "Actualización de software vencida"
                                    : "Actualización de software próxima")
                                    : (vencido ? "Mantenimiento vencido" : "Mantenimiento próximo"),
                            equipo + " en " + rs.getString("laboratorio") + " · "
                                    + rs.getDate("fecha_programada"),
                            vencido ? "Critica" : (dias <= 2 ? "Alta" : "Media"));
                }
            }
        }
    }

    private void generarMantenimientosRequeridos(Connection conexion) throws SQLException {
        String sql = "SELECT i.codigo, i.nombre_equipo, i.laboratorio, i.ultimo_mantenimiento "
                + "FROM inventario i WHERE i.estado NOT IN ('Dado de baja','En mantenimiento') "
                + "AND (i.ultimo_mantenimiento IS NULL OR i.ultimo_mantenimiento "
                + "< DATE_SUB(CURDATE(), INTERVAL ? DAY)) AND NOT EXISTS (SELECT 1 "
                + "FROM mantenimiento m WHERE m.codigo_equipo=i.codigo "
                + "AND m.estado IN ('Pendiente','En proceso'))";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, DIAS_SIN_MANTENIMIENTO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date ultimo = rs.getDate("ultimo_mantenimiento");
                    alertaDAO.guardarGenerada(conexion, "MANTENIMIENTO_REQUERIDO",
                            rs.getString("codigo"), "Programar mantenimiento preventivo",
                            nombreEquipo(rs.getString("codigo"), rs.getString("nombre_equipo"))
                                    + " en " + rs.getString("laboratorio") + " · "
                                    + (ultimo == null ? "sin mantenimiento registrado"
                                            : "último mantenimiento: " + ultimo),
                            ultimo == null ? "Alta" : "Media");
                }
            }
        }
    }

    private void generarFallas(Connection conexion) throws SQLException {
        String sql = "SELECT id_falla, codigo_equipo, laboratorio, prioridad, descripcion_falla "
                + "FROM reporte_fallas WHERE estado NOT IN ('Atendida','Cancelada')";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String codigo = rs.getString("codigo_equipo");
                alertaDAO.guardarGenerada(conexion, "FALLA_PENDIENTE",
                        String.valueOf(rs.getInt("id_falla")), "Falla pendiente",
                        nombreEquipo(codigo, null) + " en " + rs.getString("laboratorio")
                                + " · " + resumir(rs.getString("descripcion_falla")),
                        normalizarPrioridad(rs.getString("prioridad")));
            }
        }
    }

    private void generarEquipos(Connection conexion) throws SQLException {
        String sql = "SELECT i.codigo, i.nombre_equipo, i.laboratorio, COUNT(f.id_falla) total_fallas "
                + "FROM inventario i LEFT JOIN reporte_fallas f "
                + "ON f.codigo_equipo COLLATE utf8mb4_unicode_ci=i.codigo "
                + "AND f.estado<>'Cancelada' WHERE i.estado='Con falla' "
                + "GROUP BY i.codigo, i.nombre_equipo, i.laboratorio";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int fallas = rs.getInt("total_fallas");
                boolean sugerirBaja = fallas >= FALLAS_PARA_SUGERIR_BAJA;
                alertaDAO.guardarGenerada(conexion,
                        sugerirBaja ? "EQUIPO_BAJA" : "EQUIPO_REVISION", rs.getString("codigo"),
                        sugerirBaja ? "Equipo requiere valorar baja" : "Equipo requiere revisión",
                        nombreEquipo(rs.getString("codigo"), rs.getString("nombre_equipo"))
                                + " en " + rs.getString("laboratorio") + " · " + fallas
                                + " falla(s) registrada(s)", sugerirBaja ? "Critica" : "Alta");
            }
        }
    }

    private String normalizarPrioridad(String prioridad) {
        return switch (prioridad == null ? "" : prioridad.trim().toLowerCase()) {
            case "crítica", "critica" -> "Critica";
            case "alta" -> "Alta";
            case "baja" -> "Baja";
            default -> "Media";
        };
    }

    private String nombreEquipo(String codigo, String nombre) {
        if (codigo != null && !codigo.isBlank()) return codigo;
        if (nombre != null && !nombre.isBlank()) return nombre;
        return "Equipo sin código";
    }

    private String resumir(String texto) {
        if (texto == null) return "Sin descripción";
        String limpio = texto.replaceAll("\\s+", " ").trim();
        return limpio.length() <= 100 ? limpio : limpio.substring(0, 97) + "...";
    }
}
