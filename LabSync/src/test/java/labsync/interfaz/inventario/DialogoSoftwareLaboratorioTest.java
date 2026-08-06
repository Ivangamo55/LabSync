package labsync.interfaz.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toedter.calendar.JDateChooser;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import labsync.modelo.SesionUsuario;
import labsync.modelo.SoftwareLaboratorio;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class DialogoSoftwareLaboratorioTest {

    @Test
    void dialogoCompactoUsaFechaVisualYComportamientoDeEstado() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            DialogoSoftwareLaboratorio dialogo = new DialogoSoftwareLaboratorio(null,
                    List.of(new ItemLaboratorio(1, "LAB-1")), null);
            try {
                assertEquals(750, dialogo.getWidth());
                assertEquals(550, dialogo.getHeight());
                JComboBox<?> uso = campo(dialogo, "usoAcademico", JComboBox.class);
                JComboBox<?> estado = campo(dialogo, "estado", JComboBox.class);
                JTextField instalada = campo(dialogo, "versionInstalada", JTextField.class);
                assertTrue(uso.isEditable());
                assertTrue(campo(dialogo, "fechaRevision", JDateChooser.class).getDate() != null);
                estado.setSelectedItem("Pendiente de instalación");
                assertFalse(instalada.isEnabled());
            } finally {
                dialogo.dispose();
            }
        });
    }

    @Test
    void edicionPermiteConservarEstadoEliminado() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            SoftwareLaboratorio actual = software(7, "Eliminado");
            DialogoSoftwareLaboratorio dialogo = new DialogoSoftwareLaboratorio(null,
                    List.of(new ItemLaboratorio(1, "LAB-1")), actual);
            try {
                JComboBox<?> estado = campo(dialogo, "estado", JComboBox.class);
                assertEquals("Eliminado", estado.getSelectedItem());
            } finally {
                dialogo.dispose();
            }
        });
    }

    @Test
    void accionesYPollingConservanModeloSeleccionYDialogo() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            VentanaGestionInventario ventana = new VentanaGestionInventario(
                    new SesionUsuario(90, "Prueba", "Tester", "Laboratorista"));
            DialogoSoftwareLaboratorio dialogo = null;
            try {
                JTabbedPane pestanas = campo(ventana, "pestanasInventario", JTabbedPane.class);
                PanelSoftwareLaboratorio panel = (PanelSoftwareLaboratorio) pestanas.getComponentAt(1);
                JButton editar = campo(panel, "botonEditar", JButton.class);
                JButton cambiar = campo(panel, "botonCambiarEstado", JButton.class);
                JTable tabla = campo(panel, "tabla", JTable.class);
                assertFalse(editar.isEnabled());
                assertFalse(cambiar.isEnabled());

                List<SoftwareLaboratorio> filas = List.of(
                        software(1, "Actualizado"), software(2, "Desactualizado"));
                aplicar(panel, filas);
                javax.swing.table.TableModel modelo = tabla.getModel();
                tabla.setRowSelectionInterval(0, 0);

                dialogo = new DialogoSoftwareLaboratorio(ventana,
                        List.of(new ItemLaboratorio(1, "LAB-1")), null);
                dialogo.setModal(false);
                JTextField nombre = campo(dialogo, "nombre", JTextField.class);
                nombre.setText("Sin alterar");
                dialogo.setVisible(true);
                aplicar(panel, filas);

                assertSame(modelo, tabla.getModel());
                assertEquals(0, tabla.getSelectedRow());
                assertEquals(0, tabla.getColumnModel().getColumn(0).getMaxWidth());
                assertTrue(dialogo.isShowing());
                assertEquals("Sin alterar", nombre.getText());
                assertTrue(editar.isEnabled());
                assertTrue(cambiar.isEnabled());
            } finally {
                if (dialogo != null) dialogo.dispose();
                ventana.dispose();
            }
        });
    }

    private static void aplicar(PanelSoftwareLaboratorio panel,
            List<SoftwareLaboratorio> filas) {
        try {
            Method metodo = PanelSoftwareLaboratorio.class.getDeclaredMethod("aplicar", List.class);
            metodo.setAccessible(true);
            metodo.invoke(panel, filas);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static SoftwareLaboratorio software(int id, String estado) {
        return new SoftwareLaboratorio(id, 1, "LAB-1", "Software " + id,
                "1.0", "2.0", "General", estado, LocalDate.now(), "Prueba");
    }

    private static <T> T campo(Object objeto, String nombre, Class<T> tipo) {
        try {
            Field campo = objeto.getClass().getDeclaredField(nombre);
            campo.setAccessible(true);
            return tipo.cast(campo.get(objeto));
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
