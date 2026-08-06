package labsync.servicio;

import labsync.modelo.HorarioClase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Consultas y reglas de las asignaciones regulares de laboratorios. */
public final class ServicioHorarios {
    private static final Set<LocalTime> INICIOS_VALIDOS = Set.of(
            LocalTime.of(7,0), LocalTime.of(7,50), LocalTime.of(9,10),
            LocalTime.of(10,0), LocalTime.of(10,50),
            LocalTime.of(11,40), LocalTime.of(12,30), LocalTime.of(13,20),
            LocalTime.of(15,0), LocalTime.of(15,50), LocalTime.of(17,10), LocalTime.of(18,0),
            LocalTime.of(18,50), LocalTime.of(19,40), LocalTime.of(20,30));
    private static final Set<LocalTime> FINES_VALIDOS = Set.of(
            LocalTime.of(7,50), LocalTime.of(8,40), LocalTime.of(10,0),
            LocalTime.of(10,50), LocalTime.of(11,40), LocalTime.of(12,30),
            LocalTime.of(13,20), LocalTime.of(14,10), LocalTime.of(15,50),
            LocalTime.of(16,40), LocalTime.of(18,0), LocalTime.of(18,50),
            LocalTime.of(19,40), LocalTime.of(20,30), LocalTime.of(21,20));

    private static final String SELECT_BASE = "SELECT h.id_horario, c.nombre ciclo, h.id_profesor, "
            + "CONCAT_WS(' ',u.nombre,u.apellido_p,u.apellido_m) profesor, h.carrera, "
            + "h.cuatrimestre, h.grupo, h.turno, h.materia, "
            + "h.dia_semana, h.hora_inicio, h.hora_fin, l.nombre laboratorio,h.activo "
            + "FROM horarios_clase h JOIN ciclos_escolares c ON c.id_ciclo=h.id_ciclo "
            + "JOIN usuario u ON u.id=h.id_profesor "
            + "JOIN laboratorios l ON l.id_laboratorio=h.id_laboratorio ";

    public List<HorarioClase> consultarClasesDelProfesor(
            Connection conexion, int idProfesor, LocalDate fecha) throws SQLException {
        String sql = SELECT_BASE + "WHERE h.id_profesor=? AND h.activo=1 AND c.activo=1 "
                + "AND ? BETWEEN c.fecha_inicio AND c.fecha_fin "
                + "AND h.dia_semana=ELT(WEEKDAY(?)+1,'Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo') "
                + "ORDER BY h.hora_inicio";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idProfesor);
            ps.setDate(2, java.sql.Date.valueOf(fecha));
            ps.setDate(3, java.sql.Date.valueOf(fecha));
            return leer(ps);
        }
    }

    public List<HorarioClase> consultarTodos(Connection conexion) throws SQLException {
        return leer(conexion.prepareStatement(SELECT_BASE
                + "ORDER BY c.fecha_inicio DESC,h.carrera,h.cuatrimestre,"
                + "h.grupo,h.dia_semana,h.hora_inicio"));
    }

    public boolean existeConflicto(Connection conexion, Integer idExcluido, int idCiclo,
            String carrera, int cuatrimestre, String grupo, String turno,
            int idProfesor, int idLaboratorio, String dia,
            LocalTime inicio, LocalTime fin) throws SQLException {
        String sql = "SELECT h.id_horario FROM horarios_clase h WHERE h.activo=1 AND h.id_ciclo=? "
                + "AND h.dia_semana=? AND h.hora_inicio < ? AND ? < h.hora_fin "
                + "AND (h.id_laboratorio=? OR h.id_profesor=? "
                + "OR (h.carrera=? AND h.cuatrimestre=? AND h.grupo=? AND h.turno=?)) "
                + "AND h.id_horario<>COALESCE(?, -1) LIMIT 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            int i=1;
            ps.setInt(i++, idCiclo);
            ps.setString(i++, dia);
            ps.setTime(i++, Time.valueOf(fin));
            ps.setTime(i++, Time.valueOf(inicio));
            ps.setInt(i++, idLaboratorio);
            ps.setInt(i++, idProfesor);
            ps.setString(i++, carrera.trim());
            ps.setInt(i++, cuatrimestre);
            ps.setString(i++, grupo.trim());
            ps.setString(i++, turno);
            if (idExcluido == null) ps.setNull(i, java.sql.Types.INTEGER);
            else ps.setInt(i, idExcluido);
            try (ResultSet rs=ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int guardar(Connection conexion, Integer idHorario, int idCiclo,
            String carrera, int cuatrimestre, String grupo, String turno, String materia,
            int idProfesor, int idLaboratorio, String dia,
            LocalTime inicio, LocalTime fin) throws SQLException {
        validarDatosAcademicos(carrera, cuatrimestre, grupo, turno, materia);
        validarIntervalo(turno, inicio, fin);
        if (existeConflicto(conexion, idHorario, idCiclo, carrera, cuatrimestre, grupo,
                turno, idProfesor, idLaboratorio, dia, inicio, fin)) {
            throw new IllegalStateException(
                    "El profesor, grupo académico o laboratorio ya está ocupado en ese horario.");
        }
        String sql = idHorario == null
                ? "INSERT INTO horarios_clase(id_ciclo,carrera,cuatrimestre,grupo,turno,materia,id_profesor,id_laboratorio,dia_semana,hora_inicio,hora_fin,activo) VALUES(?,?,?,?,?,?,?,?,?,?,?,1)"
                : "UPDATE horarios_clase SET id_ciclo=?,carrera=?,cuatrimestre=?,grupo=?,turno=?,materia=?,id_profesor=?,id_laboratorio=?,dia_semana=?,hora_inicio=?,hora_fin=? WHERE id_horario=?";
        try (PreparedStatement ps=conexion.prepareStatement(
                sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,idCiclo);
            ps.setString(2,carrera.trim());
            ps.setInt(3,cuatrimestre);
            ps.setString(4,grupo.trim());
            ps.setString(5,turno);
            ps.setString(6,materia.trim());
            ps.setInt(7,idProfesor);
            ps.setInt(8,idLaboratorio);
            ps.setString(9,dia);
            ps.setTime(10,Time.valueOf(inicio));
            ps.setTime(11,Time.valueOf(fin));
            if (idHorario != null) ps.setInt(12,idHorario);
            ps.executeUpdate();
            if (idHorario != null) return idHorario;
            try (ResultSet rs=ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void validarDatosAcademicos(String carrera, int cuatrimestre, String grupo,
            String turno, String materia) {
        if (carrera == null || carrera.isBlank() || grupo == null || grupo.isBlank()
                || materia == null || materia.isBlank()) {
            throw new IllegalArgumentException(
                    "Carrera, grupo y materia son obligatorios para el horario.");
        }
        if (cuatrimestre < 1 || cuatrimestre > 11) {
            throw new IllegalArgumentException("El cuatrimestre debe estar entre 1 y 11.");
        }
        if (!"Matutino".equals(turno) && !"Vespertino".equals(turno)) {
            throw new IllegalArgumentException("El turno del horario no es válido.");
        }
    }

    private void validarIntervalo(String turno, LocalTime inicio, LocalTime fin) {
        if(inicio==null || fin==null || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La hora final debe ser posterior a la inicial.");
        }
        if(!INICIOS_VALIDOS.contains(inicio) || !FINES_VALIDOS.contains(fin)) {
            throw new IllegalArgumentException("Las horas deben coincidir con los módulos escolares.");
        }
        boolean valido="Matutino".equals(turno)
                ? estaDentro(inicio,fin,LocalTime.of(7,0),LocalTime.of(14,10))
                : estaDentro(inicio,fin,LocalTime.of(15,0),LocalTime.of(21,20));
        if(!valido) {
            throw new IllegalArgumentException("El horario debe pertenecer al turno seleccionado.");
        }
    }

    private boolean estaDentro(LocalTime inicio, LocalTime fin,
            LocalTime limiteInicio, LocalTime limiteFin) {
        return !inicio.isBefore(limiteInicio) && !fin.isAfter(limiteFin);
    }

    public void desactivar(Connection conexion, int idHorario) throws SQLException {
        try (PreparedStatement ps=conexion.prepareStatement(
                "UPDATE horarios_clase SET activo=0 WHERE id_horario=?")) {
            ps.setInt(1,idHorario);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se encontró el horario seleccionado.");
            }
        }
    }

    public void cambiarActivo(Connection conexion, int idHorario, boolean activo) throws SQLException {
        try (PreparedStatement ps=conexion.prepareStatement(
                "UPDATE horarios_clase SET activo=? WHERE id_horario=?")) {
            ps.setBoolean(1, activo);
            ps.setInt(2,idHorario);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se encontró el horario seleccionado.");
            }
        }
    }

    private List<HorarioClase> leer(PreparedStatement ps) throws SQLException {
        List<HorarioClase> resultado=new ArrayList<>();
        try (ps; ResultSet rs=ps.executeQuery()) {
            while(rs.next()) {
                resultado.add(new HorarioClase(rs.getInt("id_horario"),rs.getString("ciclo"),
                        rs.getInt("id_profesor"),rs.getString("profesor"),rs.getString("carrera"),
                        rs.getInt("cuatrimestre"),rs.getString("grupo"),rs.getString("turno"),
                        rs.getString("materia"),rs.getString("dia_semana"),
                        rs.getTime("hora_inicio").toLocalTime(),rs.getTime("hora_fin").toLocalTime(),
                        rs.getString("laboratorio"),rs.getBoolean("activo")));
            }
        }
        return resultado;
    }
}
