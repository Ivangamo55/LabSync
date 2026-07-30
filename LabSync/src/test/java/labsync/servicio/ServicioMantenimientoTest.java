package labsync.servicio;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServicioMantenimientoTest {

    private final ServicioMantenimiento servicio = new ServicioMantenimiento();
    private Connection conexion;

    @BeforeEach
    void configurarConexion() {
        conexion = mock(Connection.class);
    }

    @Test
    void guardar_estadoActivoConReserva_lanzaConflictoSinEscribir() throws Exception {
        // Arrange
        Consulta reservas = consulta(true);
        preparar(reservas);

        // Act
        ServicioMantenimiento.ConflictoMantenimientoException error = assertThrows(
                ServicioMantenimiento.ConflictoMantenimientoException.class,
                () -> servicio.guardar(conexion, datos("Pendiente"), null));

        // Assert
        assertTrue(error.getMessage().contains("reservas activas"));
        verify(conexion, times(1)).prepareStatement(anyString());
    }

    @Test
    void guardar_estadoInactivo_insertaYDejaInventarioDisponible() throws Exception {
        // Arrange
        Consulta insercion = actualizacion(1);
        Consulta inventario = actualizacion(1);
        preparar(insercion, inventario);

        // Act
        servicio.guardar(conexion, datos("Realizado"), null);

        // Assert
        assertAll(
                () -> verify(insercion.sentencia()).setString(1, "Preventivo"),
                () -> verify(insercion.sentencia()).setString(3, "Realizado"),
                () -> verify(insercion.sentencia()).setString(6, "EQ-01"),
                () -> verify(inventario.sentencia()).setString(1, "Disponible"),
                () -> verify(inventario.sentencia()).setString(2, "EQ-01"),
                () -> verify(conexion, times(2)).prepareStatement(anyString()));
    }

    @Test
    void guardar_estadoActivoSinReservas_insertaYMarcaInventarioEnMantenimiento() throws Exception {
        // Arrange
        Consulta reservas = consulta(false);
        Consulta clases = consulta(false);
        Consulta insercion = actualizacion(1);
        Consulta inventario = actualizacion(1);
        preparar(reservas, clases, insercion, inventario);

        // Act
        servicio.guardar(conexion, datos("Pendiente"), null);

        // Assert
        assertAll(
                () -> verify(insercion.sentencia()).executeUpdate(),
                () -> verify(inventario.sentencia()).setString(1, "En mantenimiento"),
                () -> verify(conexion, times(4)).prepareStatement(anyString()));
    }

    @Test
    void guardar_actualizacionInexistente_lanzaSQLExceptionSinModificarInventario() throws Exception {
        // Arrange
        Consulta actualizacion = actualizacion(0);
        preparar(actualizacion);

        // Act
        SQLException error = assertThrows(SQLException.class,
                () -> servicio.guardar(conexion, datos("Cancelado"), 404));

        // Assert
        assertTrue(error.getMessage().contains("No se encontró el mantenimiento"));
        verify(conexion, times(1)).prepareStatement(anyString());
    }

    @Test
    void guardar_actualizacionExistente_actualizaIdYEstadoInventario() throws Exception {
        // Arrange
        Consulta actualizacion = actualizacion(1);
        Consulta inventario = actualizacion(1);
        preparar(actualizacion, inventario);

        // Act
        servicio.guardar(conexion, datos("Realizado"), 12);

        // Assert
        assertAll(
                () -> verify(actualizacion.sentencia()).setInt(7, 12),
                () -> verify(actualizacion.sentencia()).executeUpdate(),
                () -> verify(inventario.sentencia()).setString(1, "Disponible"));
    }

    @Test
    void finalizar_mantenimientoExistente_actualizaEstadoInventarioYFecha() throws Exception {
        // Arrange
        Consulta mantenimiento = actualizacion(1);
        Consulta inventario = actualizacion(1);
        preparar(mantenimiento, inventario);

        // Act
        servicio.finalizar(conexion, 12, "EQ-01");

        // Assert
        ArgumentCaptor<String> consultas = ArgumentCaptor.forClass(String.class);
        verify(conexion, times(2)).prepareStatement(consultas.capture());
        assertAll(
                () -> verify(mantenimiento.sentencia()).setString(1, "Realizado"),
                () -> verify(mantenimiento.sentencia()).setInt(2, 12),
                () -> verify(inventario.sentencia()).setString(1, "Disponible"),
                () -> assertTrue(consultas.getAllValues().get(1).contains("ultimo_mantenimiento")));
    }

    @Test
    void finalizar_mantenimientoInexistente_lanzaSQLExceptionSinModificarInventario() throws Exception {
        // Arrange
        Consulta mantenimiento = actualizacion(0);
        preparar(mantenimiento);

        // Act
        SQLException error = assertThrows(SQLException.class,
                () -> servicio.finalizar(conexion, 999, "EQ-01"));

        // Assert
        assertTrue(error.getMessage().contains("No se encontró el mantenimiento"));
        verify(conexion, times(1)).prepareStatement(anyString());
    }

    @Test
    void cancelar_mantenimientoExistente_dejaInventarioDisponibleSinRegistrarFecha() throws Exception {
        // Arrange
        Consulta mantenimiento = actualizacion(1);
        Consulta inventario = actualizacion(1);
        preparar(mantenimiento, inventario);

        // Act
        servicio.cancelar(conexion, 13, "EQ-01");

        // Assert
        ArgumentCaptor<String> consultas = ArgumentCaptor.forClass(String.class);
        verify(conexion, times(2)).prepareStatement(consultas.capture());
        assertAll(
                () -> verify(mantenimiento.sentencia()).setString(1, "Cancelado"),
                () -> verify(inventario.sentencia()).setString(1, "Disponible"),
                () -> assertTrue(!consultas.getAllValues().get(1).contains("ultimo_mantenimiento")));
    }

    @Test
    void cancelar_equipoInexistente_propagaSQLException() throws Exception {
        // Arrange
        Consulta mantenimiento = actualizacion(1);
        Consulta inventario = actualizacion(0);
        preparar(mantenimiento, inventario);

        // Act
        SQLException error = assertThrows(SQLException.class,
                () -> servicio.cancelar(conexion, 13, "NO-EXISTE"));

        // Assert
        assertTrue(error.getMessage().contains("equipo en inventario"));
        verify(inventario.sentencia()).close();
    }

    private ServicioMantenimiento.DatosMantenimiento datos(String estado) {
        return new ServicioMantenimiento.DatosMantenimiento(
                "EQ-01", "M-11", "Preventivo", LocalDate.of(2026, 7, 30),
                estado, "Responsable QA", "Observación controlada");
    }

    private Consulta consulta(boolean hayFila) throws Exception {
        PreparedStatement sentencia = mock(PreparedStatement.class);
        ResultSet resultado = mock(ResultSet.class);
        when(sentencia.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(hayFila, false);
        return new Consulta(sentencia, resultado);
    }

    private Consulta actualizacion(int filas) throws Exception {
        PreparedStatement sentencia = mock(PreparedStatement.class);
        when(sentencia.executeUpdate()).thenReturn(filas);
        return new Consulta(sentencia, null);
    }

    private void preparar(Consulta... consultas) throws Exception {
        AtomicInteger indice = new AtomicInteger();
        when(conexion.prepareStatement(anyString())).thenAnswer(invocacion -> {
            int posicion = indice.getAndIncrement();
            if (posicion >= consultas.length) {
                throw new AssertionError("Consulta JDBC no esperada: " + invocacion.getArgument(0));
            }
            return consultas[posicion].sentencia();
        });
    }

    private record Consulta(PreparedStatement sentencia, ResultSet resultado) {
    }
}
