package labsync.persistencia;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import labsync.modelo.Alerta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RepositorioAlertasTest {

    private final RepositorioAlertas repositorio = new RepositorioAlertas();
    private Connection conexion;
    private PreparedStatement sentencia;
    private ResultSet resultado;

    @BeforeEach
    void configurarJDBC() throws Exception {
        conexion = mock(Connection.class);
        sentencia = mock(PreparedStatement.class);
        resultado = mock(ResultSet.class);
        when(conexion.prepareStatement(anyString())).thenReturn(sentencia);
        when(sentencia.executeQuery()).thenReturn(resultado);
    }

    @Test
    void listar_sinIncluirAtendidas_excluyeEstadoAtendida() throws Exception {
        // Act
        repositorio.listar(conexion, false);

        // Assert
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexion).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("estado <> 'Atendida'"),
                "La consulta debe excluir las alertas ya atendidas");
    }

    @Test
    void listar_incluyendoAtendidas_noFiltraEstadoAtendida() throws Exception {
        // Act
        repositorio.listar(conexion, true);

        // Assert
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexion).prepareStatement(sql.capture());
        assertFalse(sql.getValue().contains("estado <> 'Atendida'"));
    }

    @Test
    void contarNoLeidas_consultaSoloEstadoNueva() throws Exception {
        // Act
        repositorio.contarNoLeidas(conexion);

        // Assert
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexion).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("estado = 'Nueva'"),
                "El conteo no debe incluir alertas leídas ni atendidas");
    }

    @Test
    void listar_sinFilas_devuelveListaVaciaYCierraRecursos() throws Exception {
        // Arrange
        when(resultado.next()).thenReturn(false);

        // Act
        List<Alerta> alertas = repositorio.listar(conexion, false);

        // Assert
        assertTrue(alertas.isEmpty());
        verify(resultado).close();
        verify(sentencia).close();
    }

    @Test
    void listar_conDosFilas_mapeaValoresIncluidaFechaNula() throws Exception {
        // Arrange
        AtomicInteger fila = new AtomicInteger();
        when(resultado.next()).thenAnswer(invocacion -> fila.incrementAndGet() <= 2);
        when(resultado.getInt("id_alerta")).thenAnswer(invocacion -> fila.get());
        when(resultado.getString("tipo")).thenAnswer(invocacion -> "TIPO_" + fila.get());
        when(resultado.getString("referencia")).thenAnswer(invocacion -> "REF_" + fila.get());
        when(resultado.getString("titulo")).thenAnswer(invocacion -> "Título " + fila.get());
        when(resultado.getString("detalle")).thenAnswer(invocacion -> "Detalle " + fila.get());
        when(resultado.getString("prioridad")).thenReturn("Alta", "Media");
        when(resultado.getString("estado")).thenReturn("Nueva", "Leída");
        LocalDateTime fecha = LocalDateTime.of(2026, 7, 24, 9, 30);
        when(resultado.getTimestamp("fecha_creacion"))
                .thenReturn(Timestamp.valueOf(fecha), (Timestamp) null);

        // Act
        List<Alerta> alertas = repositorio.listar(conexion, true);

        // Assert
        assertAll(
                () -> assertEquals(2, alertas.size()),
                () -> assertEquals(1, alertas.get(0).id()),
                () -> assertEquals("REF_1", alertas.get(0).referencia()),
                () -> assertEquals(fecha, alertas.get(0).fechaCreacion()),
                () -> assertEquals(2, alertas.get(1).id()),
                () -> assertNull(alertas.get(1).fechaCreacion()));
        verify(resultado).close();
        verify(sentencia).close();
    }

    @Test
    void contarNoLeidas_conResultado_devuelveConteoYCierraRecursos() throws Exception {
        // Arrange
        when(resultado.next()).thenReturn(true);
        when(resultado.getInt(1)).thenReturn(4);

        // Act
        int total = repositorio.contarNoLeidas(conexion);

        // Assert
        assertEquals(4, total);
        verify(resultado).close();
        verify(sentencia).close();
    }

    @Test
    void contarNoLeidas_sinResultado_devuelveCero() throws Exception {
        // Arrange
        when(resultado.next()).thenReturn(false);

        // Act
        int total = repositorio.contarNoLeidas(conexion);

        // Assert
        assertEquals(0, total);
    }

    @Test
    void cambiarEstado_alertaExistente_actualizaParametrosYCierraSentencia() throws Exception {
        // Arrange
        when(sentencia.executeUpdate()).thenReturn(1);

        // Act
        repositorio.cambiarEstado(conexion, 17, "Leída");

        // Assert
        assertAll(
                () -> verify(sentencia).setString(1, "Leída"),
                () -> verify(sentencia).setString(2, "Leída"),
                () -> verify(sentencia).setString(3, "Leída"),
                () -> verify(sentencia).setInt(4, 17),
                () -> verify(sentencia).close());
    }

    @Test
    void cambiarEstado_alertaInexistente_lanzaSQLException() throws Exception {
        // Arrange
        when(sentencia.executeUpdate()).thenReturn(0);

        // Act
        SQLException error = assertThrows(SQLException.class,
                () -> repositorio.cambiarEstado(conexion, 999, "Atendida"));

        // Assert
        assertTrue(error.getMessage().contains("No se encontró"));
        verify(sentencia).close();
    }

    @Test
    void guardarGenerada_datosValidos_insertaYVinculaOrigen() throws Exception {
        // Arrange
        PreparedStatement insercion = mock(PreparedStatement.class);
        PreparedStatement vinculacion = mock(PreparedStatement.class);
        when(conexion.prepareStatement(anyString())).thenReturn(insercion, vinculacion);

        // Act
        repositorio.guardarGenerada(conexion, "FALLA_PENDIENTE", "8",
                "Falla pendiente", "Equipo sin conexión", "Alta");

        // Assert
        assertAll(
                () -> verify(insercion).setString(1, "FALLA_PENDIENTE"),
                () -> verify(insercion).setString(2, "8"),
                () -> verify(insercion).setString(3, "Falla pendiente"),
                () -> verify(insercion).setString(4, "Equipo sin conexión"),
                () -> verify(insercion).setString(5, "Alta"),
                () -> verify(insercion).executeUpdate(),
                () -> verify(vinculacion).setString(1, "FALLA_PENDIENTE"),
                () -> verify(vinculacion).setString(2, "8"),
                () -> verify(vinculacion).executeUpdate(),
                () -> verify(insercion).close(),
                () -> verify(vinculacion).close());
    }
}
