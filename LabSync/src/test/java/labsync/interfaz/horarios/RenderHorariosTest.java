package labsync.interfaz.horarios;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Component;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

class RenderHorariosTest {

    @Test
    void renderHorarioUsaEstadoDelModeloAunqueLaVistaEsteOrdenada() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel modelo = new DefaultTableModel(new Object[][]{
                {1,"C1","Carrera",1,"A","Matutino","Materia","Profesor","Lunes","07:00-08:00","LAB-1","Activo"},
                {2,"C1","Carrera",1,"B","Matutino","Materia","Profesor","Martes","08:00-09:00","LAB-2","Inactivo"}
            }, columnas(12));
            JTable tabla = new JTable(modelo);
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
            tabla.setRowSorter(sorter);
            sorter.toggleSortOrder(0);
            sorter.toggleSortOrder(0);
            VentanaGestionHorarios.RenderHorario renderer =
                    new VentanaGestionHorarios.RenderHorario();
            Component celda = assertDoesNotThrow(() -> renderer
                    .getTableCellRendererComponent(tabla, tabla.getValueAt(0, 1),
                            false, false, 0, 1));
            assertEquals(new java.awt.Color(145,145,145), celda.getForeground());
        });
    }

    @Test
    void renderCicloSoloUsaLasCincoColumnasDeCiclos() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel modelo = new DefaultTableModel(new Object[][]{
                {1,"2026-A","2026-01-01","2026-04-30",true},
                {2,"2026-B","2026-05-01","2026-08-31",false}
            }, columnas(5));
            JTable tabla = new JTable(modelo);
            tabla.setRowSorter(new TableRowSorter<>(modelo));
            VentanaGestionHorarios.RenderCiclo renderer =
                    new VentanaGestionHorarios.RenderCiclo();
            for (int vuelta = 0; vuelta < 20; vuelta++) {
                for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                    final int filaActual = fila;
                    assertDoesNotThrow(() -> renderer.getTableCellRendererComponent(
                            tabla, tabla.getValueAt(filaActual, 1), false, false,
                            filaActual, 1));
                }
            }
        });
    }

    @Test
    void cambiarPestanasRepetidamenteNoMezclaRenderers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable horarios = new JTable(new DefaultTableModel(
                    new Object[][]{{1,2,3,4,5,6,7,8,9,10,11,"Activo"}}, columnas(12)));
            JTable ciclos = new JTable(new DefaultTableModel(
                    new Object[][]{{1,"Ciclo","Inicio","Fin",true}}, columnas(5)));
            horarios.setDefaultRenderer(Object.class, new VentanaGestionHorarios.RenderHorario());
            ciclos.setDefaultRenderer(Object.class, new VentanaGestionHorarios.RenderCiclo());
            JTabbedPane pestanas = new JTabbedPane();
            pestanas.addTab("Horarios", new javax.swing.JScrollPane(horarios));
            pestanas.addTab("Ciclos", new javax.swing.JScrollPane(ciclos));
            for (int vuelta = 0; vuelta < 20; vuelta++) {
                pestanas.setSelectedIndex(vuelta % 2);
                JTable tabla = vuelta % 2 == 0 ? horarios : ciclos;
                assertDoesNotThrow(() -> tabla.prepareRenderer(
                        tabla.getCellRenderer(0, 0), 0, 0));
            }
        });
    }

    @Test
    void idsPermanecenOcultosYBusquedaNoLosOfreceAlUsuario() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            VentanaGestionHorarios ventana = new VentanaGestionHorarios(
                    new labsync.modelo.SesionUsuario(1, "Prueba", "Tester", "Laboratorista"));
            try {
                JTable horarios = campo(ventana, "tablaHorarios", JTable.class);
                JTable ciclos = campo(ventana, "tablaCiclos", JTable.class);
                javax.swing.JTextField busqueda = campo(
                        ventana, "campoBusqueda", javax.swing.JTextField.class);
                assertEquals(0, horarios.getColumnModel().getColumn(0).getMaxWidth());
                assertEquals(0, ciclos.getColumnModel().getColumn(0).getMaxWidth());
                assertFalse(busqueda.getToolTipText().contains("ID"));
            } finally {
                ventana.dispose();
            }
        });
    }

    private static <T> T campo(Object objeto, String nombre, Class<T> tipo) {
        try {
            java.lang.reflect.Field campo = objeto.getClass().getDeclaredField(nombre);
            campo.setAccessible(true);
            return tipo.cast(campo.get(objeto));
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static Object[] columnas(int cantidad) {
        Object[] columnas = new Object[cantidad];
        for (int i = 0; i < cantidad; i++) columnas[i] = "C" + i;
        return columnas;
    }
}
