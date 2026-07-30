package labsync.servicio;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

class ServicioHorariosTest {

    private static final LocalTime INICIO = LocalTime.of(9, 10);
    private static final LocalTime FIN = LocalTime.of(10, 0);

    private final ServicioHorarios servicio = new ServicioHorarios();
    private Connection conexion;

    @BeforeEach
    void configurarConexion() {
        conexion = mock(Connection.class);
    }

    @Test
    void guardar_inicioNulo_lanzaExcepcionSinConsultarPersistencia() {
        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> guardar(null, FIN));

        // Assert
        assertTrue(error.getMessage().contains("hora final"));
        verifyNoInteractions(conexion);
    }

    @Test
    void guardar_finNoPosterior_lanzaExcepcionSinConsultarPersistencia() {
        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> guardar(INICIO, INICIO));

        // Assert
        assertTrue(error.getMessage().contains("posterior"));
        verifyNoInteractions(conexion);
    }

    @Test
    void guardar_horaFueraDeModulos_lanzaExcepcionSinConsultarPersistencia() {
        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> guardar(LocalTime.of(8, 0), LocalTime.of(9, 0)));

        // Assert
        assertTrue(error.getMessage().contains("módulos escolares"));
        verifyNoInteractions(conexion);
    }

    @Test
    void guardar_grupoInexistente_lanzaExcepcionAntesDeValidarMateria() throws Exception {
        // Arrange
        Consulta grupo = consulta(false);
        when(conexion.prepareStatement(anyString())).thenReturn(grupo.sentencia());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> guardar(INICIO, FIN));

        // Assert
        assertTrue(error.getMessage().contains("grupo seleccionado no existe"));
        verify(grupo.sentencia()).setInt(1, 3);
        verify(grupo.resultado()).close();
        verify(grupo.sentencia()).close();
    }

    @Test
    void guardar_horarioFueraDelTurno_lanzaExcepcionAntesDeValidarMateria() throws Exception {
        // Arrange
        Consulta grupo = consulta(true);
        when(grupo.resultado().getString(1)).thenReturn("Matutino");
        when(conexion.prepareStatement(anyString())).thenReturn(grupo.sentencia());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> guardar(LocalTime.of(15, 0), LocalTime.of(15, 50)));

        // Assert
        assertTrue(error.getMessage().contains("turno del grupo"));
        verify(conexion).prepareStatement(anyString());
    }

    @Test
    void guardar_materiaFueraDelPlan_lanzaExcepcionAntesDeBuscarConflicto() throws Exception {
        // Arrange
        Consulta grupo = grupoMatutino();
        Consulta materia = consulta(false);
        when(conexion.prepareStatement(anyString()))
                .thenReturn(grupo.sentencia(), materia.sentencia());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> guardar(INICIO, FIN));

        // Assert
        assertTrue(error.getMessage().contains("materia no pertenece"));
        assertAll(
                () -> verify(materia.sentencia()).setInt(1, 3),
                () -> verify(materia.sentencia()).setInt(2, 14),
                () -> verify(conexion, never()).prepareStatement(anyString(), anyInt()));
    }

    @Test
    void guardar_conConflicto_lanzaExcepcionSinEscribirHorario() throws Exception {
        // Arrange
        Consulta grupo = grupoMatutino();
        Consulta materia = consulta(true);
        Consulta conflicto = consulta(true);
        when(conexion.prepareStatement(anyString())).thenReturn(
                grupo.sentencia(), materia.sentencia(), conflicto.sentencia());

        // Act
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> guardar(INICIO, FIN));

        // Assert
        assertTrue(error.getMessage().contains("ya está ocupado"));
        verify(conexion, never()).prepareStatement(anyString(), anyInt());
    }

    @Test
    void guardar_nuevoHorario_devuelveClaveGeneradaYCierraRecursos() throws Exception {
        // Arrange
        Consulta grupo = grupoMatutino();
        Consulta materia = consulta(true);
        Consulta conflicto = consulta(false);
        PreparedStatement escritura = mock(PreparedStatement.class);
        ResultSet claves = mock(ResultSet.class);
        when(escritura.executeUpdate()).thenReturn(1);
        when(escritura.getGeneratedKeys()).thenReturn(claves);
        when(claves.next()).thenReturn(true);
        when(claves.getInt(1)).thenReturn(88);
        when(conexion.prepareStatement(anyString())).thenReturn(
                grupo.sentencia(), materia.sentencia(), conflicto.sentencia());
        when(conexion.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(escritura);

        // Act
        int id = servicio.guardar(conexion, null, 1, 3, 14, 28, 2,
                "Martes", INICIO, FIN);

        // Assert
        assertAll(
                () -> assertEquals(88, id),
                () -> verify(escritura).setInt(1, 1),
                () -> verify(escritura).setInt(5, 2),
                () -> verify(escritura).setString(6, "Martes"),
                () -> verify(escritura).executeUpdate(),
                () -> verify(claves).close(),
                () -> verify(escritura).close());
    }

    @Test
    void guardar_horarioExistente_actualizaYDevuelveMismoId() throws Exception {
        // Arrange
        Consulta grupo = grupoMatutino();
        Consulta materia = consulta(true);
        Consulta conflicto = consulta(false);
        PreparedStatement escritura = mock(PreparedStatement.class);
        when(escritura.executeUpdate()).thenReturn(1);
        when(conexion.prepareStatement(anyString())).thenReturn(
                grupo.sentencia(), materia.sentencia(), conflicto.sentencia());
        when(conexion.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(escritura);

        // Act
        int id = servicio.guardar(conexion, 51, 1, 3, 14, 28, 2,
                "Martes", INICIO, FIN);

        // Assert
        assertAll(
                () -> assertEquals(51, id),
                () -> verify(escritura).setInt(9, 51),
                () -> verify(escritura, never()).getGeneratedKeys(),
                () -> verify(escritura).close());
    }

    @Test
    void existeConflicto_sinIdExcluido_asignaNullYDevuelveFalso() throws Exception {
        // Arrange
        Consulta conflicto = consulta(false);
        when(conexion.prepareStatement(anyString())).thenReturn(conflicto.sentencia());

        // Act
        boolean existe = servicio.existeConflicto(conexion, null, 1, 3, 28, 2,
                "Martes", INICIO, FIN);

        // Assert
        assertAll(
                () -> assertEquals(false, existe),
                () -> verify(conflicto.sentencia()).setNull(8, Types.INTEGER),
                () -> verify(conflicto.resultado()).close(),
                () -> verify(conflicto.sentencia()).close());
    }

    @Test
    void desactivar_horarioInexistente_lanzaSQLException() throws Exception {
        // Arrange
        PreparedStatement sentencia = mock(PreparedStatement.class);
        when(sentencia.executeUpdate()).thenReturn(0);
        when(conexion.prepareStatement(anyString())).thenReturn(sentencia);

        // Act
        SQLException error = assertThrows(SQLException.class,
                () -> servicio.desactivar(conexion, 999));

        // Assert
        assertTrue(error.getMessage().contains("No se encontró"));
        verify(sentencia).close();
    }

    @Test
    void consultarTodos_conUnaFila_mapeaHorarioYCierraRecursos() throws Exception {
        // Arrange
        Consulta consulta = consulta(true);
        ResultSet rs = consulta.resultado();
        when(rs.getInt("id_horario")).thenReturn(7);
        when(rs.getString("ciclo")).thenReturn("MAYO - AGOSTO 2026");
        when(rs.getInt("id_profesor")).thenReturn(30);
        when(rs.getString("profesor")).thenReturn("Carla Méndez Ríos");
        when(rs.getString("trayectoria")).thenReturn("DSM");
        when(rs.getInt("cuatrimestre")).thenReturn(3);
        when(rs.getString("grupo")).thenReturn("3°A");
        when(rs.getString("turno")).thenReturn("Matutino");
        when(rs.getString("materia")).thenReturn("Programación orientada a objetos");
        when(rs.getString("dia_semana")).thenReturn("Martes");
        when(rs.getTime("hora_inicio")).thenReturn(java.sql.Time.valueOf(INICIO));
        when(rs.getTime("hora_fin")).thenReturn(java.sql.Time.valueOf(FIN));
        when(rs.getString("laboratorio")).thenReturn("M-14");
        when(conexion.prepareStatement(anyString())).thenReturn(consulta.sentencia());

        // Act
        List<HorarioClase> horarios = servicio.consultarTodos(conexion);

        // Assert
        assertAll(
                () -> assertEquals(1, horarios.size()),
                () -> assertEquals(7, horarios.get(0).id()),
                () -> assertEquals("09:10 - 10:00", horarios.get(0).intervalo()),
                () -> assertEquals("M-14", horarios.get(0).laboratorio()),
                () -> verify(rs).close(),
                () -> verify(consulta.sentencia()).close());
    }

    private void guardar(LocalTime inicio, LocalTime fin) throws Exception {
        servicio.guardar(conexion, null, 1, 3, 14, 28, 2,
                "Martes", inicio, fin);
    }

    private Consulta grupoMatutino() throws Exception {
        Consulta grupo = consulta(true);
        when(grupo.resultado().getString(1)).thenReturn("Matutino");
        return grupo;
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
