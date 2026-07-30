package labsync.servicio;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServicioDisponibilidadTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 7, 27);
    private static final String HORARIO = "09:10 - 10:00";

    private final ServicioDisponibilidad servicio = new ServicioDisponibilidad();
    private Connection conexion;

    @BeforeEach
    void configurarConexion() {
        conexion = mock(Connection.class);
    }

    @Test
    void consultarParaAlumno_laboratorioInexistente_devuelveNoDisponible() throws Exception {
        // Arrange
        Consulta capacidad = consulta(false);
        preparar(capacidad);

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.consultarParaAlumno(conexion, "NO-EXISTE", FECHA, HORARIO, false);

        // Assert
        assertAll(
                () -> assertFalse(resultado.estaDisponible()),
                () -> assertEquals(0, resultado.getCapacidad()),
                () -> assertEquals("El laboratorio no está disponible.", resultado.getMensaje()),
                () -> assertFalse(resultado.estaBloqueadoPorMantenimiento()),
                () -> verify(conexion, times(1)).prepareStatement(anyString()));
    }

    @Test
    void consultarParaAlumno_mantenimientoActivo_devuelveBloqueoMantenimiento() throws Exception {
        // Arrange
        Consulta capacidad = capacidadAlumno(20);
        Consulta mantenimiento = consulta(true);
        preparar(capacidad, mantenimiento);

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.consultarParaAlumno(conexion, "M-11", FECHA, HORARIO, true);

        // Assert
        assertAll(
                () -> assertFalse(resultado.estaDisponible()),
                () -> assertTrue(resultado.estaBloqueadoPorMantenimiento()),
                () -> assertTrue(resultado.getMensaje().contains("bloqueado por mantenimiento")),
                () -> verify(conexion, times(2)).prepareStatement(anyString()));
    }

    @Test
    void consultarParaAlumno_fallaEnRevision_devuelveBloqueoMantenimiento() throws Exception {
        // Arrange
        Consulta capacidad = capacidadAlumno(20);
        Consulta mantenimiento = consulta(false);
        Consulta falla = consulta(true);
        preparar(capacidad, mantenimiento, falla);

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.consultarParaAlumno(conexion, "M-11", FECHA, HORARIO, false);

        // Assert
        assertAll(
                () -> assertFalse(resultado.estaDisponible()),
                () -> assertTrue(resultado.estaBloqueadoPorMantenimiento()),
                () -> assertTrue(resultado.getMensaje().contains("reporte de falla en revisión")),
                () -> verify(conexion, times(3)).prepareStatement(anyString()));
    }

    @Test
    void consultarParaAlumno_claseRegularTraslapada_devuelveNoDisponible() throws Exception {
        // Arrange
        Consulta capacidad = capacidadAlumno(20);
        Consulta mantenimiento = consulta(false);
        Consulta falla = consulta(false);
        Consulta clase = consulta(true);
        preparar(capacidad, mantenimiento, falla, clase);

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.consultarParaAlumno(conexion, "M-11", FECHA, HORARIO, false);

        // Assert
        assertAll(
                () -> assertFalse(resultado.estaDisponible()),
                () -> assertTrue(resultado.getMensaje().contains("clase regular")),
                () -> assertEquals(0, resultado.getEquiposDisponibles()),
                () -> verify(conexion, times(4)).prepareStatement(anyString()));
    }

    @Test
    void consultarParaAlumno_reservaProfesorTraslapada_devuelveNoDisponible() throws Exception {
        // Arrange
        Consulta ocupacion = flujoHastaOcupacionAlumno(20, true);
        when(ocupacion.resultado().getString("rol_solicitante")).thenReturn("Profesor");

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.consultarParaAlumno(conexion, "M-11", FECHA, HORARIO, false);

        // Assert
        assertAll(
                () -> assertFalse(resultado.estaDisponible()),
                () -> assertEquals(20, resultado.getCapacidad()),
                () -> assertTrue(resultado.getMensaje().contains("reservado por un profesor")));
    }

    @Test
    void consultarParaAlumno_capacidadOcupada_devuelveSinComputadoras() throws Exception {
        // Arrange
        Consulta ocupacion = flujoHastaOcupacionAlumno(10, true);
        when(ocupacion.resultado().getString("rol_solicitante")).thenReturn("Estudiante");
        when(ocupacion.resultado().getInt("cantidad_alumnos")).thenReturn(10);

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.consultarParaAlumno(conexion, "M-11", FECHA, HORARIO, false);

        // Assert
        assertAll(
                () -> assertFalse(resultado.estaDisponible()),
                () -> assertEquals(10, resultado.getCapacidad()),
                () -> assertEquals(0, resultado.getEquiposDisponibles()),
                () -> assertTrue(resultado.getMensaje().contains("No hay computadoras")));
    }

    @Test
    void consultarParaAlumno_ocupacionParcial_devuelveEquiposRestantes() throws Exception {
        // Arrange
        Consulta ocupacion = flujoHastaOcupacionAlumno(10, true, true);
        when(ocupacion.resultado().getString("rol_solicitante"))
                .thenReturn("Estudiante", "Estudiante");
        when(ocupacion.resultado().getInt("cantidad_alumnos")).thenReturn(2, 3);

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.consultarParaAlumno(conexion, "M-11", FECHA, HORARIO, false);

        // Assert
        assertAll(
                () -> assertTrue(resultado.estaDisponible()),
                () -> assertEquals(5, resultado.getEquiposDisponibles()),
                () -> assertEquals(10, resultado.getCapacidad()),
                () -> assertEquals("Disponible", resultado.getMensaje()));
    }

    @Test
    void consultarParaProfesor_conReservasAlumno_devuelveNoDisponible() throws Exception {
        // Arrange
        Consulta capacidad = consulta(true);
        when(capacidad.resultado().getInt("capacidad_personas")).thenReturn(30);
        Consulta mantenimiento = consulta(false);
        Consulta falla = consulta(false);
        Consulta clase = consulta(false);
        Consulta ocupacion = consulta(true);
        when(ocupacion.resultado().getString("rol_solicitante")).thenReturn("Estudiante");
        when(ocupacion.resultado().getInt("cantidad_alumnos")).thenReturn(1);
        preparar(capacidad, mantenimiento, falla, clase, ocupacion);

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.consultarParaProfesor(conexion, "M-11", FECHA, HORARIO, false);

        // Assert
        assertAll(
                () -> assertFalse(resultado.estaDisponible()),
                () -> assertEquals(30, resultado.getCapacidad()),
                () -> assertTrue(resultado.getMensaje().contains("reservas individuales")));
    }

    @Test
    void validarAprobacion_reservaInexistente_devuelveNoDisponible() throws Exception {
        // Arrange
        Consulta reserva = consulta(false);
        preparar(reserva);

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.validarAprobacion(conexion, 404, false);

        // Assert
        assertAll(
                () -> assertFalse(resultado.estaDisponible()),
                () -> assertTrue(resultado.getMensaje().contains("No se encontró")),
                () -> verify(reserva.sentencia()).setInt(1, 404),
                () -> verify(reserva.resultado()).close(),
                () -> verify(reserva.sentencia()).close());
    }

    @Test
    void validarAprobacion_cantidadMayorQueCapacidad_rechazaSinMasConsultas() throws Exception {
        // Arrange
        Consulta reserva = consulta(true);
        when(reserva.resultado().getString("rol_solicitante")).thenReturn("Profesor");
        when(reserva.resultado().getInt("capacidad_personas")).thenReturn(25);
        when(reserva.resultado().getInt("cantidad_alumnos")).thenReturn(26);
        preparar(reserva);

        // Act
        ServicioDisponibilidad.ResultadoDisponibilidad resultado =
                servicio.validarAprobacion(conexion, 18, true);

        // Assert
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexion).prepareStatement(sql.capture());
        assertAll(
                () -> assertFalse(resultado.estaDisponible()),
                () -> assertEquals(25, resultado.getCapacidad()),
                () -> assertTrue(resultado.getMensaje().contains("supera la capacidad")),
                () -> assertTrue(sql.getValue().endsWith(" FOR UPDATE")));
    }

    private Consulta capacidadAlumno(int capacidad) throws Exception {
        Consulta consulta = consulta(true);
        when(consulta.resultado().getInt("total_equipos")).thenReturn(capacidad);
        return consulta;
    }

    private Consulta flujoHastaOcupacionAlumno(int capacidad, boolean... filasOcupacion)
            throws Exception {
        Consulta consultaCapacidad = capacidadAlumno(capacidad);
        Consulta mantenimiento = consulta(false);
        Consulta falla = consulta(false);
        Consulta clase = consulta(false);
        Consulta ocupacion = consulta(filasOcupacion);
        preparar(consultaCapacidad, mantenimiento, falla, clase, ocupacion);
        return ocupacion;
    }

    private Consulta consulta(boolean... filas) throws Exception {
        PreparedStatement sentencia = mock(PreparedStatement.class);
        ResultSet resultado = mock(ResultSet.class);
        AtomicInteger indice = new AtomicInteger();
        when(resultado.next()).thenAnswer(invocacion -> {
            int posicion = indice.getAndIncrement();
            return posicion < filas.length && filas[posicion];
        });
        when(sentencia.executeQuery()).thenReturn(resultado);
        return new Consulta(sentencia, resultado);
    }

    private void preparar(Consulta... consultas) throws Exception {
        AtomicInteger indice = new AtomicInteger();
        when(conexion.prepareStatement(anyString())).thenAnswer(invocacion -> {
            int posicion = indice.getAndIncrement();
            if (posicion >= consultas.length) {
                throw new AssertionError("Se ejecutó una consulta JDBC no esperada: " + invocacion.getArgument(0));
            }
            return consultas[posicion].sentencia();
        });
    }

    private record Consulta(PreparedStatement sentencia, ResultSet resultado) {
    }
}
