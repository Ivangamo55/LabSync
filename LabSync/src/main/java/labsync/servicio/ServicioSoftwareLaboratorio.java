package labsync.servicio;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import labsync.modelo.SoftwareLaboratorio;

/** Persistencia y validaciones del software instalado por laboratorio. */
public final class ServicioSoftwareLaboratorio {
    public static final List<String> ESTADOS = List.of("Actualizado", "Desactualizado",
            "Pendiente de instalación", "Pendiente de eliminación", "Eliminado");
    private static final Set<String> ESTADOS_VALIDOS = Set.copyOf(ESTADOS);

    public void validar(int idLaboratorio, String nombre, String versionInstalada,
            String versionObjetivo, String usoAcademico, String estado,
            String observaciones) {
        if (idLaboratorio <= 0) throw new IllegalArgumentException("Selecciona un laboratorio.");
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del software es obligatorio.");
        }
        if (nombre.trim().length() > 150) {
            throw new IllegalArgumentException("El nombre no puede exceder 150 caracteres.");
        }
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new IllegalArgumentException("El estado del software no es válido.");
        }
        if ("Desactualizado".equals(estado) && vacio(versionObjetivo)) {
            throw new IllegalArgumentException("El software desactualizado requiere versión objetivo.");
        }
        if ("Pendiente de eliminación".equals(estado) && vacio(observaciones)) {
            throw new IllegalArgumentException("La eliminación pendiente requiere observaciones.");
        }
        if (usoAcademico != null && usoAcademico.trim().length() > 200) {
            throw new IllegalArgumentException("El uso académico no puede exceder 200 caracteres.");
        }
    }

    public int guardar(Connection conexion, Integer idSoftware, int idLaboratorio,
            String nombre, String versionInstalada, String versionObjetivo,
            String usoAcademico, String estado, LocalDate fechaRevision,
            String observaciones) throws SQLException {
        validar(idLaboratorio, nombre, versionInstalada, versionObjetivo,
                usoAcademico, estado, observaciones);
        String sql = idSoftware == null
                ? "INSERT INTO software_laboratorio(id_laboratorio,nombre,version_instalada,version_objetivo,uso_academico,estado,fecha_revision,observaciones) VALUES(?,?,?,?,?,?,?,?)"
                : "UPDATE software_laboratorio SET id_laboratorio=?,nombre=?,version_instalada=?,version_objetivo=?,uso_academico=?,estado=?,fecha_revision=?,observaciones=? WHERE id_software=?";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idLaboratorio);
            ps.setString(2, nombre.trim());
            ps.setString(3, nuloSiVacio(versionInstalada));
            ps.setString(4, nuloSiVacio(versionObjetivo));
            ps.setString(5, vacio(usoAcademico) ? "General" : usoAcademico.trim());
            ps.setString(6, estado);
            if (fechaRevision == null) ps.setNull(7, java.sql.Types.DATE);
            else ps.setDate(7, Date.valueOf(fechaRevision));
            ps.setString(8, nuloSiVacio(observaciones));
            if (idSoftware != null) ps.setInt(9, idSoftware);
            try {
                if (ps.executeUpdate() == 0) throw new SQLException("No se encontró el software seleccionado.");
            } catch (SQLException ex) {
                if (esDuplicado(ex)) {
                    throw new IllegalArgumentException(
                            "Ya existe software con ese nombre en el laboratorio.", ex);
                }
                throw ex;
            }
            if (idSoftware != null) return idSoftware;
            try (ResultSet claves = ps.getGeneratedKeys()) {
                return claves.next() ? claves.getInt(1) : 0;
            }
        }
    }

    public void cambiarEstado(Connection conexion, int idSoftware, String estado,
            String versionObjetivo, String observaciones) throws SQLException {
        if (!ESTADOS_VALIDOS.contains(estado)) throw new IllegalArgumentException("Estado no válido.");
        if ("Desactualizado".equals(estado) && vacio(versionObjetivo)) {
            throw new IllegalArgumentException("El software desactualizado requiere versión objetivo.");
        }
        if ("Pendiente de eliminación".equals(estado) && vacio(observaciones)) {
            throw new IllegalArgumentException("La eliminación pendiente requiere observaciones.");
        }
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE software_laboratorio SET estado=? WHERE id_software=?")) {
            ps.setString(1, estado);
            ps.setInt(2, idSoftware);
            if (ps.executeUpdate() == 0) throw new SQLException("No se encontró el software seleccionado.");
        }
    }

    public List<SoftwareLaboratorio> consultar(Connection conexion, Integer idLaboratorio,
            String estado, String usoAcademico, String busqueda) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT s.id_software,s.id_laboratorio,l.nombre laboratorio,s.nombre,s.version_instalada,s.version_objetivo,s.uso_academico,s.estado,s.fecha_revision,s.observaciones FROM software_laboratorio s JOIN laboratorios l ON l.id_laboratorio=s.id_laboratorio WHERE 1=1");
        List<Object> parametros = new ArrayList<>();
        if (idLaboratorio != null) { sql.append(" AND s.id_laboratorio=?"); parametros.add(idLaboratorio); }
        if (!vacio(estado)) { sql.append(" AND s.estado=?"); parametros.add(estado); }
        if (!vacio(usoAcademico)) { sql.append(" AND s.uso_academico LIKE ?"); parametros.add("%" + usoAcademico.trim() + "%"); }
        if (!vacio(busqueda)) {
            sql.append(" AND (s.nombre LIKE ? OR s.version_instalada LIKE ? OR s.version_objetivo LIKE ?)");
            String patron = "%" + busqueda.trim() + "%";
            parametros.add(patron); parametros.add(patron); parametros.add(patron);
        }
        sql.append(" ORDER BY s.id_software DESC");
        try (PreparedStatement ps = conexion.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) ps.setObject(i + 1, parametros.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<SoftwareLaboratorio> resultado = new ArrayList<>();
                while (rs.next()) {
                    Date fecha = rs.getDate("fecha_revision");
                    resultado.add(new SoftwareLaboratorio(rs.getInt("id_software"),
                            rs.getInt("id_laboratorio"), rs.getString("laboratorio"),
                            rs.getString("nombre"), rs.getString("version_instalada"),
                            rs.getString("version_objetivo"), rs.getString("uso_academico"),
                            rs.getString("estado"), fecha == null ? null : fecha.toLocalDate(),
                            rs.getString("observaciones")));
                }
                return resultado;
            }
        }
    }

    private static boolean esDuplicado(SQLException ex) {
        return ex.getErrorCode() == 1062 || "23000".equals(ex.getSQLState());
    }

    private static boolean vacio(String valor) { return valor == null || valor.isBlank(); }
    private static String nuloSiVacio(String valor) { return vacio(valor) ? null : valor.trim(); }
}
