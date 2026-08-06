package labsync.servicio;

import labsync.modelo.Alerta;
import labsync.persistencia.RepositorioAlertas;
import labsync.modelo.TiposMantenimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Genera y administra las alertas automáticas del laboratorista. */
public final class ServicioAlertas {
    public static final int DIAS_MANTENIMIENTO_PROXIMO = 7;
    public static final int FALLAS_PARA_SUGERIR_BAJA = 3;
    public static final int DIAS_SIN_MANTENIMIENTO = 180;

    private final RepositorioAlertas alertaDAO = new RepositorioAlertas();

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
            generarReservasPendientes(conexion);
            generarResolucionesReserva(conexion);
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
        cambiarEstadoEnTransaccion(conexion, idAlerta, "Leída");
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
        String sql = "SELECT m.id_mantenimiento, i.codigo codigo_equipo, i.nombre_equipo, l.nombre laboratorio, "
                + "m.tipo_mantenimiento, m.fecha_programada, "
                + "DATEDIFF(fecha_programada, CURDATE()) dias "
                + "FROM mantenimiento m JOIN inventario i ON i.id_inventario=m.id_inventario "
                + "JOIN laboratorios l ON l.id_laboratorio=i.id_laboratorio WHERE m.estado IN ('Pendiente','En proceso') "
                + "AND m.fecha_programada <= DATE_ADD(CURDATE(), INTERVAL ? DAY)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, DIAS_MANTENIMIENTO_PROXIMO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int dias = rs.getInt("dias");
                    boolean vencido = dias < 0;
                    String equipo = nombreEquipo(rs.getString("codigo_equipo"), rs.getString("nombre_equipo"));
                    String tipoMantenimiento = rs.getString("tipo_mantenimiento");
                    DatosAlertaMantenimiento alerta = describirMantenimiento(
                            tipoMantenimiento, vencido);
                    alertaDAO.guardarGenerada(conexion, null, null,
                            rs.getInt("id_mantenimiento"), null,
                            alerta.tipoAlerta(),
                            String.valueOf(rs.getInt("id_mantenimiento")),
                            alerta.titulo(),
                            alerta.detalle() + ": " + equipo + " en "
                                    + rs.getString("laboratorio") + " · "
                                    + rs.getDate("fecha_programada"),
                            vencido ? "Crítica" : (dias <= 2 ? "Alta" : "Media"));
                }
            }
        }
    }

    static DatosAlertaMantenimiento describirMantenimiento(String tipo, boolean vencido) {
        String estado = vencido ? "vencido" : "próximo";
        String estadoFemenino = vencido ? "vencida" : "próxima";
        if (TiposMantenimiento.ACTUALIZACION_SOFTWARE.equals(tipo)) {
            return new DatosAlertaMantenimiento("ACTUALIZACION_SOFTWARE",
                    "Actualización de software " + estadoFemenino,
                    "Actualización de software " + estadoFemenino);
        }
        if (TiposMantenimiento.ACTUALIZACION_HARDWARE.equals(tipo)) {
            return new DatosAlertaMantenimiento("ACTUALIZACION_HARDWARE",
                    "Actualización de hardware " + estadoFemenino,
                    "Actualización de hardware " + estadoFemenino);
        }
        if (TiposMantenimiento.DISPOSICION_MATERIAL_PELIGROSO.equals(tipo)) {
            return new DatosAlertaMantenimiento("DISPOSICION_PELIGROSA",
                    "Disposición de material peligroso " + estadoFemenino,
                    "Disposición de material peligroso " + estadoFemenino);
        }
        if (TiposMantenimiento.RETIRO_EQUIPO_OBSOLETO.equals(tipo)) {
            return new DatosAlertaMantenimiento("RETIRO_EQUIPO_OBSOLETO",
                    "Retiro de equipo obsoleto " + estado,
                    "Retiro de equipo obsoleto " + estado);
        }
        if (TiposMantenimiento.PREVENTIVO.equals(tipo)) {
            return new DatosAlertaMantenimiento("MANTENIMIENTO_PREVENTIVO",
                    "Mantenimiento preventivo " + estado,
                    "Mantenimiento preventivo " + estado);
        }
        if (TiposMantenimiento.CORRECTIVO.equals(tipo)) {
            return new DatosAlertaMantenimiento("MANTENIMIENTO_CORRECTIVO",
                    "Mantenimiento correctivo " + estado,
                    "Mantenimiento correctivo " + estado);
        }
        return new DatosAlertaMantenimiento("MANTENIMIENTO_GENERAL",
                "Mantenimiento " + estado, (tipo == null || tipo.isBlank()
                        ? "Mantenimiento" : tipo) + " " + estado);
    }

    record DatosAlertaMantenimiento(String tipoAlerta, String titulo, String detalle) {
    }

    private void generarMantenimientosRequeridos(Connection conexion) throws SQLException {
        String sql = consultaMantenimientosRequeridos();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, DIAS_SIN_MANTENIMIENTO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date ultimo = rs.getDate("ultimo_mantenimiento");
                    if (!requiereMantenimientoPreventivo(
                            ultimo == null ? null : ultimo.toLocalDate(),
                            rs.getDate("fecha_registro").toLocalDate(),
                            rs.getDate("fecha_actual").toLocalDate(),
                            rs.getString("estado_inventario"), false)) {
                        continue;
                    }
                    alertaDAO.guardarGenerada(conexion, null, null, null,
                            rs.getInt("id_inventario"), "MANTENIMIENTO_REQUERIDO",
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

    static String consultaMantenimientosRequeridos() {
        return "SELECT i.id_inventario, i.codigo, i.nombre_equipo, l.nombre laboratorio, "
                + "i.ultimo_mantenimiento, DATE(i.fecha_registro) fecha_registro, "
                + "CURDATE() fecha_actual, i.estado estado_inventario "
                + "FROM inventario i JOIN laboratorios l ON l.id_laboratorio=i.id_laboratorio "
                + "WHERE i.estado NOT IN ('Baja','En mantenimiento') "
                + "AND " + RepositorioAlertas.condicionAntiguedadMantenimiento("?")
                + " AND NOT EXISTS (SELECT 1 "
                + "FROM mantenimiento m WHERE m.id_inventario=i.id_inventario "
                + "AND m.estado IN ('Pendiente','En proceso'))";
    }

    static boolean requiereMantenimientoPreventivo(LocalDate ultimoMantenimiento,
            LocalDate fechaRegistro, LocalDate fechaActual, String estadoInventario,
            boolean tieneMantenimientoActivo) {
        if ("Baja".equals(estadoInventario) || "En mantenimiento".equals(estadoInventario)
                || tieneMantenimientoActivo) {
            return false;
        }
        LocalDate fechaBase = ultimoMantenimiento == null ? fechaRegistro : ultimoMantenimiento;
        return ChronoUnit.DAYS.between(fechaBase, fechaActual) > DIAS_SIN_MANTENIMIENTO;
    }

    private void generarFallas(Connection conexion) throws SQLException {
        String sql = "SELECT f.id_falla, i.codigo codigo_equipo, l.nombre laboratorio, f.prioridad, f.descripcion_falla "
                + "FROM reporte_fallas f LEFT JOIN inventario i ON i.id_inventario=f.id_inventario "
                + "JOIN laboratorios l ON l.id_laboratorio=f.id_laboratorio WHERE f.estado NOT IN ('Atendida','Cancelada')";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String codigo = rs.getString("codigo_equipo");
                alertaDAO.guardarGenerada(conexion, null, rs.getInt("id_falla"), null, null,
                        "FALLA_PENDIENTE",
                        String.valueOf(rs.getInt("id_falla")), "Falla pendiente",
                        nombreEquipo(codigo, null) + " en " + rs.getString("laboratorio")
                                + " · " + resumir(rs.getString("descripcion_falla")),
                        normalizarPrioridad(rs.getString("prioridad")));
            }
        }
    }

    private void generarEquipos(Connection conexion) throws SQLException {
        String sql = "SELECT i.id_inventario, i.codigo, i.nombre_equipo, l.nombre laboratorio, COUNT(f.id_falla) total_fallas "
                + "FROM inventario i JOIN laboratorios l ON l.id_laboratorio=i.id_laboratorio "
                + "LEFT JOIN reporte_fallas f "
                + "ON f.id_inventario=i.id_inventario "
                + "AND f.estado<>'Cancelada' WHERE i.estado='Con falla' "
                + "GROUP BY i.id_inventario, i.codigo, i.nombre_equipo, l.nombre";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int fallas = rs.getInt("total_fallas");
                boolean sugerirBaja = fallas >= FALLAS_PARA_SUGERIR_BAJA;
                alertaDAO.guardarGenerada(conexion, null, null, null,
                        rs.getInt("id_inventario"),
                        sugerirBaja ? "EQUIPO_BAJA" : "EQUIPO_REVISION", rs.getString("codigo"),
                        sugerirBaja ? "Equipo requiere valorar baja" : "Equipo requiere revisión",
                        nombreEquipo(rs.getString("codigo"), rs.getString("nombre_equipo"))
                                + " en " + rs.getString("laboratorio") + " · " + fallas
                                + " falla(s) registrada(s)", sugerirBaja ? "Crítica" : "Alta");
            }
        }
    }

    private void generarReservasPendientes(Connection conexion) throws SQLException {
        String sql = "SELECT r.id_reserva, CONCAT_WS(' ',u.nombre,u.apellido_p,u.apellido_m) nombre_solicitante, u.rol rol_solicitante, l.nombre AS laboratorio, "
                + "r.actividad,r.fecha,CONCAT(TIME_FORMAT(r.hora_inicio,'%H:%i'),' - ',TIME_FORMAT(r.hora_fin,'%H:%i')) horario FROM reservas r JOIN usuario u ON u.id=r.id_usuario JOIN laboratorios l ON l.id_laboratorio=r.id_laboratorio "
                + "WHERE r.estado='Pendiente' AND r.fecha >= CURDATE()";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                boolean esAlumno = "Estudiante".equalsIgnoreCase(rs.getString("rol_solicitante"))
                        || "Alumno".equalsIgnoreCase(rs.getString("rol_solicitante"));
                String solicitante = rs.getString("nombre_solicitante");
                String recurso = rs.getString("laboratorio");
                String titulo = esAlumno ? "Reserva de equipo pendiente"
                        : "Reserva de aula pendiente";
                String detalle = solicitante + " solicitó "
                        + (esAlumno ? "un equipo en " : "el aula ") + recurso + " · "
                        + rs.getDate("fecha") + " · " + rs.getString("horario") + " · "
                        + resumir(rs.getString("actividad"));
                alertaDAO.guardarGenerada(conexion, rs.getInt("id_reserva"), null, null, null,
                        "RESERVA_PENDIENTE",
                        String.valueOf(rs.getInt("id_reserva")), titulo, detalle,
                        rs.getDate("fecha").toLocalDate().equals(java.time.LocalDate.now())
                                ? "Alta" : "Media");
            }
        }
    }

    private void generarResolucionesReserva(Connection conexion) throws SQLException {
        String sql = "SELECT r.id_reserva,l.nombre AS laboratorio,r.fecha,CONCAT(TIME_FORMAT(r.hora_inicio,'%H:%i'),' - ',TIME_FORMAT(r.hora_fin,'%H:%i')) horario,r.estado,r.observaciones "
                + "FROM reservas r JOIN laboratorios l ON l.id_laboratorio=r.id_laboratorio "
                + "WHERE r.estado IN ('Aprobada','Rechazada')";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                boolean aprobada = "Aprobada".equals(rs.getString("estado"));
                String detalle = "Tu reserva de " + rs.getString("laboratorio") + " para el "
                        + rs.getDate("fecha") + " de " + rs.getString("horario") + " fue "
                        + (aprobada ? "autorizada." : "rechazada.");
                String observaciones = rs.getString("observaciones");
                if (observaciones != null && !observaciones.isBlank()) {
                    detalle += " Observaciones: " + resumir(observaciones);
                }
                alertaDAO.guardarGenerada(conexion, rs.getInt("id_reserva"), null, null, null,
                        aprobada ? "RESERVA_APROBADA" : "RESERVA_RECHAZADA",
                        String.valueOf(rs.getInt("id_reserva")),
                        aprobada ? "Reserva autorizada" : "Reserva rechazada",
                        detalle, aprobada ? "Media" : "Alta");
            }
        }
    }

    public List<Alerta> listarResolucionesReserva(Connection conexion, int idUsuario,
            String nombreCompleto, String rol) throws SQLException {
        return alertaDAO.listarResolucionesReserva(conexion, idUsuario, nombreCompleto, rol);
    }

    private String normalizarPrioridad(String prioridad) {
        return switch (prioridad == null ? "" : prioridad.trim().toLowerCase()) {
            case "crítica" -> "Crítica";
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
