package labsync.interfaz.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import labsync.interfaz.comun.ActualizadorModeloTabla;
import labsync.modelo.SesionUsuario;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class InventarioPestanasTest {

    @Test
    void equiposConservaRegionTablaSoftwareYModeloEstable() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            VentanaGestionInventario ventana = new VentanaGestionInventario(
                    new SesionUsuario(88,"Prueba","Tester","Laboratorista"));
            try {
                JTabbedPane pestanas = campo(ventana, "pestanasInventario", JTabbedPane.class);
                JTable tabla = campo(ventana, "tablaInventario", JTable.class);
                JScrollPane scroll = campo(ventana, "jScrollPane1", JScrollPane.class);
                javax.swing.JPanel body = campo(ventana, "body", javax.swing.JPanel.class);
                assertTrue(ventana.getContentPane().getLayout() instanceof java.awt.BorderLayout);
                assertEquals("panelDerechoLaboratorista", body.getParent().getName());
                assertEquals(2, pestanas.getTabCount());
                assertEquals("Equipos", pestanas.getTitleAt(0));
                assertEquals("Software por laboratorio", pestanas.getTitleAt(1));
                assertSame(tabla, scroll.getViewport().getView());
                assertEquals(36, tabla.getRowHeight());
                assertEquals(0, tabla.getColumnModel().getColumn(1).getMaxWidth());

                javax.swing.table.TableModel identidad = tabla.getModel();
                DefaultTableModel iguales = copia(tabla);
                for (int ciclo = 0; ciclo < 3; ciclo++) {
                    ActualizadorModeloTabla.aplicar(tabla, iguales, 0, () -> { });
                }
                assertSame(identidad, tabla.getModel());
                if (tabla.getRowCount() > 0) {
                    tabla.setRowSelectionInterval(0, 0);
                    ActualizadorModeloTabla.aplicar(tabla, copia(tabla), 0, () -> { });
                    assertEquals(0, tabla.getSelectedRow());
                }
            } finally {
                ventana.dispose();
            }
        });
    }

    private static DefaultTableModel copia(JTable tabla) {
        Object[] columnas = new Object[tabla.getColumnCount()];
        for (int c = 0; c < columnas.length; c++) columnas[c] = tabla.getColumnName(c);
        DefaultTableModel copia = new DefaultTableModel(columnas, 0);
        for (int f = 0; f < tabla.getRowCount(); f++) {
            Object[] fila = new Object[tabla.getColumnCount()];
            for (int c = 0; c < fila.length; c++) fila[c] = tabla.getValueAt(f, c);
            copia.addRow(fila);
        }
        return copia;
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
