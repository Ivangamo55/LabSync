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
            + "CONCAT_WS(' ',u.nombre,u.apellido_p,u.apellido_m) profesor, t.nombre trayectoria, "
            + "g.cuatrimestre, CONCAT(g.cuatrimestre,'°',g.letra) grupo, g.turno, m.nombre materia, "
            + "h.dia_semana, h.hora_inicio, h.hora_fin, l.nombre laboratorio "
            + "FROM horarios_clase h JOIN ciclos_escolares c ON c.id_ciclo=h.id_ciclo "
            + "JOIN grupos g ON g.id_grupo=h.id_grupo JOIN trayectorias t ON t.id_trayectoria=g.id_trayectoria "
            + "JOIN plan_materias pm ON pm.id_plan_materia=h.id_plan_materia "
            + "JOIN materias m ON m.id_materia=pm.id_materia JOIN usuario u ON u.id=h.id_profesor "
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
                + "WHERE h.activo=1 ORDER BY c.fecha_inicio DESC,t.codigo,g.cuatrimestre,g.letra,h.dia_semana,h.hora_inicio"));
    }

    public boolean existeConflicto(Connection conexion, Integer idExcluido, int idCiclo,
            int idGrupo, int idProfesor, int idLaboratorio, String dia,
            LocalTime inicio, LocalTime fin) throws SQLException {
        String sql = "SELECT h.id_horario FROM horarios_clase h WHERE h.activo=1 AND h.id_ciclo=? "
                + "AND h.dia_semana=? AND h.hora_inicio < ? AND ? < h.hora_fin "
                + "AND (h.id_grupo=? OR h.id_profesor=? OR h.id_laboratorio=?) "
                + "AND h.id_horario<>COALESCE(?, -1) LIMIT 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            int i=1;
            ps.setInt(i++, idCiclo); ps.setString(i++, dia); ps.setTime(i++, Time.valueOf(fin));
            ps.setTime(i++, Time.valueOf(inicio)); ps.setInt(i++, idGrupo);
            ps.setInt(i++, idProfesor); ps.setInt(i++, idLaboratorio);
            if (idExcluido == null) ps.setNull(i, java.sql.Types.INTEGER); else ps.setInt(i, idExcluido);
            try (ResultSet rs=ps.executeQuery()) { return rs.next(); }
        }
    }

    public int guardar(Connection conexion, Integer idHorario, int idCiclo, int idGrupo,
            int idPlanMateria, int idProfesor, int idLaboratorio, String dia,
            LocalTime inicio, LocalTime fin) throws SQLException {
        validarIntervalo(conexion,idGrupo,inicio,fin);
        validarMateriaDelGrupo(conexion, idGrupo, idPlanMateria);
        if (existeConflicto(conexion,idHorario,idCiclo,idGrupo,idProfesor,idLaboratorio,dia,inicio,fin))
            throw new IllegalStateException("El profesor, grupo o laboratorio ya está ocupado en ese horario.");
        String sql = idHorario == null
                ? "INSERT INTO horarios_clase(id_ciclo,id_grupo,id_plan_materia,id_profesor,id_laboratorio,dia_semana,hora_inicio,hora_fin,activo) VALUES(?,?,?,?,?,?,?,?,1)"
                : "UPDATE horarios_clase SET id_ciclo=?,id_grupo=?,id_plan_materia=?,id_profesor=?,id_laboratorio=?,dia_semana=?,hora_inicio=?,hora_fin=? WHERE id_horario=?";
        try (PreparedStatement ps=conexion.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,idCiclo); ps.setInt(2,idGrupo); ps.setInt(3,idPlanMateria); ps.setInt(4,idProfesor);
            ps.setInt(5,idLaboratorio); ps.setString(6,dia); ps.setTime(7,Time.valueOf(inicio)); ps.setTime(8,Time.valueOf(fin));
            if (idHorario != null) ps.setInt(9,idHorario);
            ps.executeUpdate();
            if (idHorario != null) return idHorario;
            try (ResultSet rs=ps.getGeneratedKeys()) { return rs.next()?rs.getInt(1):0; }
        }
    }

    private void validarIntervalo(Connection conexion,int idGrupo,LocalTime inicio,LocalTime fin)
            throws SQLException {
        if(inicio==null||fin==null||!fin.isAfter(inicio))
            throw new IllegalArgumentException("La hora final debe ser posterior a la inicial.");
        if(!INICIOS_VALIDOS.contains(inicio)||!FINES_VALIDOS.contains(fin))
            throw new IllegalArgumentException("Las horas deben coincidir con los módulos escolares.");
        String turno;
        try(PreparedStatement ps=conexion.prepareStatement("SELECT turno FROM grupos WHERE id_grupo=?")) {
            ps.setInt(1,idGrupo);
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) throw new IllegalArgumentException("El grupo seleccionado no existe.");
                turno=rs.getString(1);
            }
        }
        boolean valido="Matutino".equals(turno)
                ? estaDentro(inicio,fin,LocalTime.of(7,0),LocalTime.of(14,10))
                : estaDentro(inicio,fin,LocalTime.of(15,0),LocalTime.of(21,20));
        if(!valido)
            throw new IllegalArgumentException("El horario debe pertenecer al turno del grupo.");
    }

    private boolean estaDentro(LocalTime inicio,LocalTime fin,LocalTime limiteInicio,LocalTime limiteFin) {
        return !inicio.isBefore(limiteInicio)&&!fin.isAfter(limiteFin);
    }

    public void desactivar(Connection conexion, int idHorario) throws SQLException {
        try (PreparedStatement ps=conexion.prepareStatement("UPDATE horarios_clase SET activo=0 WHERE id_horario=?")) {
            ps.setInt(1,idHorario);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se encontró el horario seleccionado.");
            }
        }
    }

    private void validarMateriaDelGrupo(Connection conexion,int idGrupo,int idPlan) throws SQLException {
        String sql="SELECT 1 FROM grupos g JOIN plan_materias pm ON pm.id_trayectoria=g.id_trayectoria "
                + "AND pm.cuatrimestre=g.cuatrimestre WHERE g.id_grupo=? AND pm.id_plan_materia=?";
        try (PreparedStatement ps=conexion.prepareStatement(sql)) {
            ps.setInt(1,idGrupo); ps.setInt(2,idPlan);
            try(ResultSet rs=ps.executeQuery()) { if(!rs.next()) throw new IllegalArgumentException("La materia no pertenece al plan del grupo."); }
        }
    }

    private List<HorarioClase> leer(PreparedStatement ps) throws SQLException {
        List<HorarioClase> resultado=new ArrayList<>();
        try (ps; ResultSet rs=ps.executeQuery()) {
            while(rs.next()) resultado.add(new HorarioClase(rs.getInt("id_horario"),rs.getString("ciclo"),
                    rs.getInt("id_profesor"),rs.getString("profesor"),rs.getString("trayectoria"),
                    rs.getInt("cuatrimestre"),rs.getString("grupo"),rs.getString("turno"),rs.getString("materia"),
                    rs.getString("dia_semana"),rs.getTime("hora_inicio").toLocalTime(),
                    rs.getTime("hora_fin").toLocalTime(),rs.getString("laboratorio")));
        }
        return resultado;
    }
}
