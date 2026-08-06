package labsync.interfaz.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import labsync.modelo.Alerta;
import org.junit.jupiter.api.Test;

class ModeloAlertasTest {

    @Test
    void mismosDatos_noNotificaCambio() {
        ModeloAlertas modelo = new ModeloAlertas();
        Alerta alerta = alerta(1, "Aviso original");
        modelo.actualizar(List.of(alerta));
        AtomicInteger notificaciones = escuchar(modelo);

        ModeloAlertas.ResultadoActualizacion resultado = modelo.actualizar(List.of(alerta));

        assertFalse(resultado.cambio());
        assertEquals(0, notificaciones.get());
    }

    @Test
    void alertaNueva_apareceEnElModelo() {
        ModeloAlertas modelo = new ModeloAlertas();
        modelo.actualizar(List.of(alerta(1, "Primera")));

        ModeloAlertas.ResultadoActualizacion resultado = modelo.actualizar(
                List.of(alerta(1, "Primera"), alerta(2, "Nueva")));

        assertTrue(resultado.cambio());
        assertEquals(List.of(2), resultado.nuevas().stream().map(Alerta::id).toList());
        assertEquals(2, modelo.getRowCount());
    }

    @Test
    void alertaActualizada_cambiaSinDuplicarse() {
        ModeloAlertas modelo = new ModeloAlertas();
        modelo.actualizar(List.of(alerta(7, "Anterior")));

        ModeloAlertas.ResultadoActualizacion resultado =
                modelo.actualizar(List.of(alerta(7, "Actualizada")));

        assertTrue(resultado.cambio());
        assertTrue(resultado.nuevas().isEmpty());
        assertEquals(1, modelo.getRowCount());
        assertEquals("Actualizada", modelo.alertaEn(0).titulo());
    }

    @Test
    void alertaEliminada_desapareceDelModelo() {
        ModeloAlertas modelo = new ModeloAlertas();
        modelo.actualizar(List.of(alerta(1, "Primera"), alerta(2, "Segunda")));

        modelo.actualizar(List.of(alerta(2, "Segunda")));

        assertEquals(1, modelo.getRowCount());
        assertEquals(-1, modelo.indicePorId(1));
    }

    @Test
    void buscarPorId_recuperaLaSeleccion() {
        ModeloAlertas modelo = new ModeloAlertas();
        modelo.actualizar(List.of(alerta(11, "Primera"), alerta(25, "Seleccionada")));

        int indice = modelo.indicePorId(25);

        assertEquals(1, indice);
        assertEquals(25, modelo.alertaEn(indice).id());
    }

    private AtomicInteger escuchar(ModeloAlertas modelo) {
        AtomicInteger total = new AtomicInteger();
        modelo.addTableModelListener(evento -> total.incrementAndGet());
        return total;
    }

    private Alerta alerta(int id, String titulo) {
        return new Alerta(id, "MANTENIMIENTO_REQUERIDO", String.valueOf(id), titulo,
                "Detalle " + id, "Media", "Nueva",
                LocalDateTime.of(2026, 8, 5, 10, id % 60));
    }
}
