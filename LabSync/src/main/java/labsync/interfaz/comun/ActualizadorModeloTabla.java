package labsync.interfaz.comun;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import javax.swing.DefaultRowSorter;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/** Actualiza las filas de un modelo estable preservando el estado visual de JTable. */
public final class ActualizadorModeloTabla {
    private ActualizadorModeloTabla() { }

    public static boolean aplicar(JTable tabla, DefaultTableModel datosNuevos,
            int columnaIdentificador, Runnable ajustarColumnas) {
        DefaultTableModel actual = (DefaultTableModel) tabla.getModel();
        if (iguales(actual, datosNuevos)) return false;

        Object seleccionado = identificadorSeleccionado(tabla, columnaIdentificador);
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, tabla);
        Point posicion = viewport == null ? null : viewport.getViewPosition();
        RowSorter<? extends TableModel> ordenador = tabla.getRowSorter();
        List<? extends RowSorter.SortKey> orden = ordenador == null
                ? List.of() : List.copyOf(ordenador.getSortKeys());
        RowFilter<?, ?> filtro = ordenador instanceof DefaultRowSorter<?, ?> sorter
                ? sorter.getRowFilter() : null;

        sincronizarColumnas(actual, datosNuevos);
        copiarFilas(actual, datosNuevos);
        if (ordenador instanceof DefaultRowSorter<?, ?> sorter) {
            restaurarFiltro(sorter, filtro);
            ordenador.setSortKeys(orden);
        }
        ajustarColumnas.run();
        restaurarSeleccion(tabla, columnaIdentificador, seleccionado);
        if (viewport != null && posicion != null) {
            SwingUtilities.invokeLater(() -> viewport.setViewPosition(posicion));
        }
        return true;
    }

    public static Object identificadorSeleccionado(JTable tabla, int columnaIdentificador) {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) return null;
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        return tabla.getModel().getValueAt(filaModelo, columnaIdentificador);
    }

    public static int buscarFilaModelo(JTable tabla, int columnaIdentificador, Object id) {
        if (id == null) return -1;
        for (int fila = 0; fila < tabla.getModel().getRowCount(); fila++) {
            if (Objects.equals(String.valueOf(id),
                    String.valueOf(tabla.getModel().getValueAt(fila, columnaIdentificador)))) {
                return fila;
            }
        }
        return -1;
    }

    private static void restaurarSeleccion(JTable tabla, int columnaIdentificador, Object id) {
        int filaModelo = buscarFilaModelo(tabla, columnaIdentificador, id);
        int filaVista = filaModelo < 0 ? -1 : tabla.convertRowIndexToView(filaModelo);
        if (filaVista < 0) tabla.clearSelection();
        else tabla.getSelectionModel().setSelectionInterval(filaVista, filaVista);
    }

    private static boolean iguales(DefaultTableModel actual, DefaultTableModel nuevo) {
        if (actual.getColumnCount() != nuevo.getColumnCount()
                || actual.getRowCount() != nuevo.getRowCount()) return false;
        for (int columna = 0; columna < actual.getColumnCount(); columna++) {
            if (!Objects.equals(actual.getColumnName(columna), nuevo.getColumnName(columna))) {
                return false;
            }
        }
        for (int fila = 0; fila < actual.getRowCount(); fila++) {
            for (int columna = 0; columna < actual.getColumnCount(); columna++) {
                if (!Objects.equals(actual.getValueAt(fila, columna),
                        nuevo.getValueAt(fila, columna))) return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copiarFilas(DefaultTableModel destino, DefaultTableModel origen) {
        Vector filasDestino = destino.getDataVector();
        filasDestino.clear();
        for (Object fila : origen.getDataVector()) {
            filasDestino.add(new Vector((Vector) fila));
        }
        destino.fireTableDataChanged();
    }

    private static void sincronizarColumnas(DefaultTableModel destino, DefaultTableModel origen) {
        boolean distintas = destino.getColumnCount() != origen.getColumnCount();
        for (int i = 0; !distintas && i < destino.getColumnCount(); i++) {
            distintas = !Objects.equals(destino.getColumnName(i), origen.getColumnName(i));
        }
        if (!distintas) return;
        Vector<String> columnas = new Vector<>();
        for (int i = 0; i < origen.getColumnCount(); i++) {
            columnas.add(origen.getColumnName(i));
        }
        destino.setColumnIdentifiers(columnas);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restaurarFiltro(DefaultRowSorter sorter, RowFilter filtro) {
        sorter.setRowFilter(filtro);
    }
}
