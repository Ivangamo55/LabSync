package labsync.interfaz.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.sql.SQLSyntaxErrorException;
import org.junit.jupiter.api.Test;

class PanelSoftwareLaboratorioTest {

    @Test
    void tablaAusenteProduceMensajeDeEsquemaSinDetalleTecnico() {
        Throwable error = new IllegalStateException(new SQLSyntaxErrorException(
                "Table 'labsync_db.software_laboratorio' doesn't exist", "42S02", 1146));
        String mensaje = PanelSoftwareLaboratorio.mensajeUsuario(error);
        assertEquals(PanelSoftwareLaboratorio.MENSAJE_ESQUEMA_DESACTUALIZADO, mensaje);
        assertFalse(mensaje.contains("SQLSyntaxErrorException"));
        assertFalse(mensaje.contains("software_laboratorio"));
    }

    @Test
    void errorSqlDistintoNoSeExponeAlUsuario() {
        String mensaje = PanelSoftwareLaboratorio.mensajeUsuario(
                new SQLSyntaxErrorException("SELECT secreto FROM tabla_interna"));
        assertEquals("No fue posible completar la operación de software. Inténtalo nuevamente.",
                mensaje);
    }

    @Test
    void registroValidoCumpleReglasVisuales() {
        assertNull(ReglasFormularioSoftware.validar(3, "IntelliJ IDEA", "2026.1", "",
                "Actualizado", ""));
    }

    @Test
    void creacionNoOfreceEliminadoPeroEdicionSi() {
        assertFalse(ReglasFormularioSoftware.estados(false).contains("Eliminado"));
        assertTrue(ReglasFormularioSoftware.estados(true).contains("Eliminado"));
    }

    @Test
    void pendienteInstalacionDeshabilitaInstaladaYExigeObjetivo() {
        ReglasFormularioSoftware.Configuracion configuracion =
                ReglasFormularioSoftware.configuracion("Pendiente de instalación");
        assertFalse(configuracion.versionInstaladaHabilitada());
        assertTrue(configuracion.versionObjetivoObligatoria());
        assertEquals("Indica la versión que debe instalarse.",
                ReglasFormularioSoftware.validar(1, "Docker", "", "",
                        "Pendiente de instalación", ""));
    }

    @Test
    void desactualizadoExigeAmbasVersiones() {
        assertEquals("Indica la versión instalada.",
                ReglasFormularioSoftware.validar(1, "Java", "", "21",
                        "Desactualizado", ""));
        assertEquals("Indica la versión objetivo para software desactualizado.",
                ReglasFormularioSoftware.validar(1, "Java", "17", "",
                        "Desactualizado", ""));
    }

    @Test
    void pendienteEliminacionExigeObservaciones() {
        assertEquals("Indica el motivo de la eliminación.",
                ReglasFormularioSoftware.validar(1, "Java", "17", "",
                        "Pendiente de eliminación", ""));
    }

    @Test
    void fechaInicialEsHoy() {
        assertEquals(LocalDate.now(), ReglasFormularioSoftware.fechaInicial());
    }

    @Test
    void resumenCuentaSoloEstadosQueRequierenAtencion() {
        List<labsync.modelo.SoftwareLaboratorio> filas = List.of(
                software(1, "Actualizado"), software(2, "Desactualizado"),
                software(3, "Pendiente de instalación"),
                software(4, "Pendiente de eliminación"), software(5, "Eliminado"));
        PanelSoftwareLaboratorio.ResumenSoftware resumen =
                PanelSoftwareLaboratorio.calcularResumen(filas);
        assertEquals(5, resumen.total());
        assertEquals(3, resumen.requierenAtencion());
    }

    private static labsync.modelo.SoftwareLaboratorio software(int id, String estado) {
        return new labsync.modelo.SoftwareLaboratorio(id, 1, "LAB", "Software " + id,
                "1", "2", "General", estado, LocalDate.now(), "");
    }
}
