package labsync.interfaz.mantenimiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.junit.jupiter.api.Test;

class SeleccionMantenimientoTablaTest {

    @Test
    void filaVisualOrdenada_utilizaElIdDelModeloCorrecto() {
        JTable tabla = tabla();
        TableRowSorter<DefaultTableModel> ordenador =
                new TableRowSorter<>((DefaultTableModel) tabla.getModel());
        tabla.setRowSorter(ordenador);
        ordenador.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        tabla.setRowSelectionInterval(0, 0);
        AtomicReference<SeleccionMantenimientoTabla.Seleccion> recibida = new AtomicReference<>();

        boolean ejecutada = SeleccionMantenimientoTabla.ejecutarSiExiste(tabla, recibida::set);

        assertTrue(ejecutada);
        assertEquals(22, recibida.get().idMantenimiento());
        assertEquals("EQ-22", recibida.get().codigoEquipo());
    }

    @Test
    void ausenciaDeSeleccion_noEjecutaElServicio() {
        JTable tabla = tabla();
        AtomicBoolean servicioInvocado = new AtomicBoolean();

        boolean ejecutada = SeleccionMantenimientoTabla.ejecutarSiExiste(
                tabla, seleccion -> servicioInvocado.set(true));

        assertFalse(ejecutada);
        assertFalse(servicioInvocado.get());
    }

    private JTable tabla() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[][] {{18, "EQ-18"}, {22, "EQ-22"}},
                new Object[] {"ID", "Código"});
        return new JTable(modelo);
    }
}
