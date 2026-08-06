package labsync.servicio;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.stream.Stream;
import labsync.modelo.TiposMantenimiento;
import labsync.persistencia.RepositorioAlertas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ServicioAlertasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 8, 5);

    @ParameterizedTest(name = "alerta de {0}")
    @MethodSource("tiposDeMantenimiento")
    void describirMantenimiento_generaTituloYDetalleEspecificos(
            String tipo, String codigoAlerta, String textoEsperado) {
        ServicioAlertas.DatosAlertaMantenimiento proxima =
                ServicioAlertas.describirMantenimiento(tipo, false);
        ServicioAlertas.DatosAlertaMantenimiento vencida =
                ServicioAlertas.describirMantenimiento(tipo, true);

        assertAll(
                () -> assertEquals(codigoAlerta, proxima.tipoAlerta()),
                () -> assertEquals(codigoAlerta, vencida.tipoAlerta()),
                () -> assertTrue(proxima.titulo().contains(textoEsperado)),
                () -> assertTrue(proxima.detalle().contains(textoEsperado)),
                () -> assertTrue(proxima.titulo().contains("próxim")),
                () -> assertTrue(vencida.titulo().contains("vencid")),
                () -> assertTrue(vencida.detalle().contains(textoEsperado)));
    }

    private static Stream<Arguments> tiposDeMantenimiento() {
        return Stream.of(
                Arguments.of(TiposMantenimiento.PREVENTIVO,
                        "MANTENIMIENTO_PREVENTIVO", "Mantenimiento preventivo"),
                Arguments.of(TiposMantenimiento.CORRECTIVO,
                        "MANTENIMIENTO_CORRECTIVO", "Mantenimiento correctivo"),
                Arguments.of(TiposMantenimiento.ACTUALIZACION_SOFTWARE,
                        "ACTUALIZACION_SOFTWARE", "Actualización de software"),
                Arguments.of(TiposMantenimiento.ACTUALIZACION_HARDWARE,
                        "ACTUALIZACION_HARDWARE", "Actualización de hardware"),
                Arguments.of(TiposMantenimiento.DISPOSICION_MATERIAL_PELIGROSO,
                        "DISPOSICION_PELIGROSA", "Disposición de material peligroso"),
                Arguments.of(TiposMantenimiento.RETIRO_EQUIPO_OBSOLETO,
                        "RETIRO_EQUIPO_OBSOLETO", "Retiro de equipo obsoleto"));
    }

    @Test
    void equipoRecienRegistrado_sinUltimoMantenimiento_noGeneraAlerta() {
        assertFalse(requiere(null, HOY));
    }

    @Test
    void equipoCon179DiasDesdeRegistro_noGeneraAlerta() {
        assertFalse(requiere(null, HOY.minusDays(179)));
    }

    @Test
    void equipoConMasDe180DiasDesdeRegistroSinMantenimiento_generaAlerta() {
        assertTrue(requiere(null, HOY.minusDays(181)));
    }

    @Test
    void ultimoMantenimientoReciente_noGeneraAlerta() {
        assertFalse(requiere(HOY.minusDays(30), HOY.minusDays(500)));
    }

    @Test
    void ultimoMantenimientoMayorA180Dias_generaAlerta() {
        assertTrue(requiere(HOY.minusDays(181), HOY.minusDays(500)));
    }

    @Test
    void equipoVencidoConMantenimientoPendiente_noGeneraAlerta() {
        assertFalse(ServicioAlertas.requiereMantenimientoPreventivo(
                null, HOY.minusDays(181), HOY, "Disponible", true));
    }

    @Test
    void consultaUsaFechaRegistroCuandoUltimoMantenimientoEsNulo() {
        String condicion = RepositorioAlertas.condicionAntiguedadMantenimiento("?");
        assertAll(
                () -> assertTrue(ServicioAlertas.consultaMantenimientosRequeridos()
                        .contains(condicion)),
                () -> assertTrue(condicion.contains(
                        "COALESCE(i.ultimo_mantenimiento, DATE(i.fecha_registro))")),
                () -> assertTrue(condicion.contains("> ?")));
    }

    private boolean requiere(LocalDate ultimoMantenimiento, LocalDate fechaRegistro) {
        return ServicioAlertas.requiereMantenimientoPreventivo(
                ultimoMantenimiento, fechaRegistro, HOY, "Disponible", false);
    }
}
