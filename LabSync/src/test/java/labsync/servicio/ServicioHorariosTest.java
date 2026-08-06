package labsync.servicio;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalTime;
import java.util.List;
import labsync.modelo.HorarioClase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServicioHorariosTest {

    private static final LocalTime INICIO = LocalTime.of(9, 10);
    private static final LocalTime FIN = LocalTime.of(10, 0);
    private static final String CARRERA = "TSU - DSM";

    private final ServicioHorarios servicio = new ServicioHorarios();
    private Connection conexion;

    @BeforeEach
    void configurarConexion() {
        conexion = mock(Connection.class);
    }

    @Test
    void guardar_inicioNulo_lanzaExcepcionSinConsultarPersistencia() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> guardar(null, FIN));

        assertTrue(error.getMessage().contains("hora final"));
        verifyNoInteractions(conexion);
    }

    @Test
    void guardar_finNoPosterior_lanzaExcepcionSinConsultarPersistencia() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> guardar(INICIO, INICIO));

        assertTrue(error.getMessage().contains("posterior"));
        verifyNoInteractions(conexion);
    }

    @Test
    void guardar_horaFueraDeModulos_lanzaExcepcionSinConsultarPersistencia() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> guardar(LocalTime.of(8, 0), LocalTime.of(9, 0)));

        assertTrue(error.getMessage().contains("módulos escolares"));
        verifyNoInteractions(conexion);
    }

    @Test
    void guardar_horarioFueraDelTurno_lanzaExcepcionSinConsultarPersistencia() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardar(conexion, null, 1, CARRERA, 3, "A", "Matutino",
                        "Programación", 28, 2, "Martes",
                        LocalTime.of(15, 0), LocalTime.of(15, 50)));

        assertTrue(error.getMessage().contains("turno seleccionado"));
        verifyNoInteractions(conexion);
    }

    @Test
    void guardar_conflictoPorProfesor_rechazaSinEscribir() throws Exception {
        verificarConflicto("h.id_profesor=?", 28, 2, CARRERA, 3);
    }

    @Test
    void guardar_conflictoPorLaboratorio_rechazaSinEscribir() throws Exception {
        verificarConflicto("h.id_laboratorio=?", 31, 2, CARRERA, 3);
    }

    @Test
    void guardar_conflictoPorGrupoAcademico_rechazaSinEscribir() throws Exception {
        verificarConflicto("h.carrera=? AND h.cuatrimestre=? AND h.grupo=? AND h.turno=?",
                31, 4, CARRERA, 3);
    }

    @Test
    void existeConflicto_mismaLetraEnCarreraYCuatrimestreDistintos_noConflictaPorGrupo()
            throws Exception {
        Consulta conflicto = consulta(false);
        when(conexion.prepareStatement(anyString())).thenReturn(conflicto.sentencia());

        boolean existe = servicio.existeConflicto(conexion, null, 1, "TSU - ENV", 5,
                "A", "Matutino", 31, 4, "Martes", INICIO, FIN);

        assertAll(
                () -> assertFalse(existe),
                () -> verify(conflicto.sentencia()).setString(7, "TSU - ENV"),
                () -> verify(conflicto.sentencia()).setInt(8, 5),
                () -> verify(conflicto.sentencia()).setString(9, "A"),
                () -> verify(conflicto.sentencia()).setString(10, "Matutino"),
                () -> verify(conflicto.sentencia()).setNull(11, Types.INTEGER));
    }

    @Test
    void guardar_nuevoHorario_devuelveClaveGeneradaYCierraRecursos() throws Exception {
        Consulta conflicto = consulta(false);
        PreparedStatement escritura = mock(PreparedStatement.class);
        ResultSet claves = mock(ResultSet.class);
        when(escritura.executeUpdate()).thenReturn(1);
        when(escritura.getGeneratedKeys()).thenReturn(claves);
        when(claves.next()).thenReturn(true);
        when(claves.getInt(1)).thenReturn(88);
        when(conexion.prepareStatement(anyString())).thenReturn(conflicto.sentencia());
        when(conexion.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(escritura);

        int id = guardar(INICIO, FIN);

        assertAll(
                () -> assertEquals(88, id),
                () -> verify(escritura).setInt(1, 1),
                () -> verify(escritura).setString(2, CARRERA),
                () -> verify(escritura).setInt(3, 3),
                () -> verify(escritura).setString(4, "A"),
                () -> verify(escritura).setString(5, "Matutino"),
                () -> verify(escritura).setString(6, "Programación"),
                () -> verify(escritura).setInt(8, 2),
                () -> verify(escritura).setString(9, "Martes"),
                () -> verify(escritura).executeUpdate(),
                () -> verify(claves).close(),
                () -> verify(escritura).close());
    }

    @Test
    void guardar_horarioExistente_actualizaYDevuelveMismoId() throws Exception {
        Consulta conflicto = consulta(false);
        PreparedStatement escritura = mock(PreparedStatement.class);
        when(escritura.executeUpdate()).thenReturn(1);
        when(conexion.prepareStatement(anyString())).thenReturn(conflicto.sentencia());
        when(conexion.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(escritura);

        int id = servicio.guardar(conexion, 51, 1, CARRERA, 3, "A", "Matutino",
                "Programación", 28, 2, "Martes", INICIO, FIN);

        assertAll(
                () -> assertEquals(51, id),
                () -> verify(escritura).setInt(12, 51),
                () -> verify(escritura, never()).getGeneratedKeys(),
                () -> verify(escritura).close());
    }

    @Test
    void desactivar_horarioExistente_actualizaUnaFila() throws Exception {
        PreparedStatement sentencia = mock(PreparedStatement.class);
        when(sentencia.executeUpdate()).thenReturn(1);
        when(conexion.prepareStatement(anyString())).thenReturn(sentencia);

        servicio.desactivar(conexion, 51);

        assertAll(
                () -> verify(sentencia).setInt(1, 51),
                () -> verify(sentencia).executeUpdate(),
                () -> verify(sentencia).close());
    }

    @Test
    void desactivar_horarioInexistente_lanzaSQLException() throws Exception {
        PreparedStatement sentencia = mock(PreparedStatement.class);
        when(sentencia.executeUpdate()).thenReturn(0);
        when(conexion.prepareStatement(anyString())).thenReturn(sentencia);

        SQLException error = assertThrows(SQLException.class,
                () -> servicio.desactivar(conexion, 999));

        assertTrue(error.getMessage().contains("No se encontró"));
        verify(sentencia).close();
    }

    @Test
    void consultarTodos_conUnaFila_mapeaHorarioYCierraRecursos() throws Exception {
        Consulta consulta = consulta(true);
        ResultSet rs = consulta.resultado();
        when(rs.getInt("id_horario")).thenReturn(7);
        when(rs.getString("ciclo")).thenReturn("MAYO - AGOSTO 2026");
        when(rs.getInt("id_profesor")).thenReturn(30);
        when(rs.getString("profesor")).thenReturn("Carla Méndez Ríos");
        when(rs.getString("carrera")).thenReturn(CARRERA);
        when(rs.getInt("cuatrimestre")).thenReturn(3);
        when(rs.getString("grupo")).thenReturn("A");
        when(rs.getString("turno")).thenReturn("Matutino");
        when(rs.getString("materia")).thenReturn("Programación orientada a objetos");
        when(rs.getString("dia_semana")).thenReturn("Martes");
        when(rs.getTime("hora_inicio")).thenReturn(java.sql.Time.valueOf(INICIO));
        when(rs.getTime("hora_fin")).thenReturn(java.sql.Time.valueOf(FIN));
        when(rs.getString("laboratorio")).thenReturn("M-14");
        when(conexion.prepareStatement(anyString())).thenReturn(consulta.sentencia());

        List<HorarioClase> horarios = servicio.consultarTodos(conexion);

        assertAll(
                () -> assertEquals(1, horarios.size()),
                () -> assertEquals(7, horarios.get(0).id()),
                () -> assertEquals(CARRERA, horarios.get(0).carrera()),
                () -> assertEquals(3, horarios.get(0).cuatrimestre()),
                () -> assertEquals("A", horarios.get(0).grupo()),
                () -> assertEquals("09:10 - 10:00", horarios.get(0).intervalo()),
                () -> assertEquals("M-14", horarios.get(0).laboratorio()),
                () -> verify(rs).close(),
                () -> verify(consulta.sentencia()).close());
    }

    private int guardar(LocalTime inicio, LocalTime fin) throws Exception {
        return servicio.guardar(conexion, null, 1, CARRERA, 3, "A", "Matutino",
                "Programación", 28, 2, "Martes", inicio, fin);
    }

    private void verificarConflicto(String fragmentoSql, int idProfesor, int idLaboratorio,
            String carrera, int cuatrimestre) throws Exception {
        Consulta conflicto = consulta(true);
        when(conexion.prepareStatement(anyString())).thenReturn(conflicto.sentencia());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.guardar(conexion, null, 1, carrera, cuatrimestre, "A",
                        "Matutino", "Programación", idProfesor, idLaboratorio,
                        "Martes", INICIO, FIN));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexion).prepareStatement(sql.capture());
        assertAll(
                () -> assertTrue(error.getMessage().contains("ya está ocupado")),
                () -> assertTrue(sql.getValue().contains(fragmentoSql)),
                () -> verify(conexion, never()).prepareStatement(anyString(), anyInt()));
    }

    private Consulta consulta(boolean hayFila) throws Exception {
        PreparedStatement sentencia = mock(PreparedStatement.class);
        ResultSet resultado = mock(ResultSet.class);
        when(sentencia.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(hayFila, false);
        return new Consulta(sentencia, resultado);
    }

    private record Consulta(PreparedStatement sentencia, ResultSet resultado) {
    }
}
