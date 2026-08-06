package labsync.interfaz.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.junit.jupiter.api.Test;

class ActualizadorModeloTablaTest {

    @Test
    void tresCiclosSinCambiosConservanUnRegistroRealizado() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel actual = modelo(fila(18, "Realizado"));
            JTable tabla = new JTable(actual);
            AtomicInteger eventos = new AtomicInteger();
            actual.addTableModelListener(evento -> eventos.incrementAndGet());

            for (int ciclo = 0; ciclo < 3; ciclo++) {
                assertFalse(ActualizadorModeloTabla.aplicar(
                        tabla, modelo(fila(18, "Realizado")), 0, () -> { }));
            }

            assertEquals(1, tabla.getRowCount());
            assertEquals("Realizado", tabla.getValueAt(0, 1));
            assertEquals(0, eventos.get());
        });
    }

    @Test
    void datosIgualesNoNotificanNiReemplazanModelo() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel actual = modelo(fila(1, "Pendiente"));
            JTable tabla = new JTable(actual);
            AtomicInteger eventos = new AtomicInteger();
            actual.addTableModelListener(evento -> eventos.incrementAndGet());

            assertFalse(ActualizadorModeloTabla.aplicar(
                    tabla, modelo(fila(1, "Pendiente")), 0, () -> { }));
            assertEquals(0, eventos.get());
            assertSame(actual, tabla.getModel());
        });
    }

    @Test
    void incorporaActualizaYEliminaSinDuplicar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel actual = modelo(fila(1, "Pendiente"));
            JTable tabla = new JTable(actual);

            assertTrue(ActualizadorModeloTabla.aplicar(tabla,
                    modelo(fila(1, "Pendiente"), fila(2, "Pendiente")), 0, () -> { }));
            assertEquals(2, actual.getRowCount());

            assertTrue(ActualizadorModeloTabla.aplicar(tabla,
                    modelo(fila(1, "Realizado"), fila(2, "Pendiente")), 0, () -> { }));
            assertEquals(2, actual.getRowCount());
            assertEquals("Realizado", actual.getValueAt(0, 1));

            assertTrue(ActualizadorModeloTabla.aplicar(tabla,
                    modelo(fila(2, "Pendiente")), 0, () -> { }));
            assertEquals(1, actual.getRowCount());
            assertEquals(2, actual.getValueAt(0, 0));
        });
    }

    @Test
    void conservaFiltroOrdenSeleccionYScrollUsandoIndicesVistaModelo() throws Exception {
        JTable[] referenciaTabla = new JTable[1];
        JScrollPane[] referenciaScroll = new JScrollPane[1];
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel actual = modelo(
                    fila(1, "Pendiente"), fila(2, "Realizado"), fila(3, "Pendiente"));
            JTable tabla = new JTable(actual);
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(actual);
            sorter.setRowFilter(RowFilter.regexFilter("Pendiente", 1));
            sorter.setSortKeys(List.of(new javax.swing.RowSorter.SortKey(
                    0, javax.swing.SortOrder.DESCENDING)));
            tabla.setRowSorter(sorter);
            int filaModeloSeleccionada = ActualizadorModeloTabla.buscarFilaModelo(tabla, 0, 1);
            tabla.setRowSelectionInterval(tabla.convertRowIndexToView(filaModeloSeleccionada),
                    tabla.convertRowIndexToView(filaModeloSeleccionada));

            tabla.setPreferredSize(new Dimension(300, 600));
            JScrollPane scroll = new JScrollPane(tabla);
            scroll.setSize(200, 100);
            scroll.doLayout();
            scroll.getViewport().setViewPosition(new Point(0, 32));

            assertTrue(ActualizadorModeloTabla.aplicar(tabla,
                    modelo(fila(1, "Pendiente"), fila(2, "Realizado"),
                            fila(3, "Pendiente"), fila(4, "Pendiente")), 0, () -> { }));
            assertEquals(1, ActualizadorModeloTabla.identificadorSeleccionado(tabla, 0));
            assertEquals(3, tabla.getRowCount());
            assertEquals(javax.swing.SortOrder.DESCENDING,
                    tabla.getRowSorter().getSortKeys().get(0).getSortOrder());
            assertSame(actual, tabla.getModel());
            referenciaTabla[0] = tabla;
            referenciaScroll[0] = scroll;
        });
        SwingUtilities.invokeAndWait(() -> { });
        assertEquals(new Point(0, 32), referenciaScroll[0].getViewport().getViewPosition());
        assertEquals(1, ActualizadorModeloTabla.identificadorSeleccionado(referenciaTabla[0], 0));
    }

    private static Object[] fila(int id, String estado) {
        return new Object[]{id, estado};
    }

    private static DefaultTableModel modelo(Object[]... filas) {
        return new DefaultTableModel(filas, new String[]{"ID", "Estado"}) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }
}
