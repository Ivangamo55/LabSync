package labsync.interfaz.mantenimiento;

import java.util.function.Consumer;
import javax.swing.JTable;

/** Obtiene de forma estable la identidad real de una fila seleccionada. */
public final class SeleccionMantenimientoTabla {
    private SeleccionMantenimientoTabla() { }

    public static boolean ejecutarSiExiste(JTable tabla, Consumer<Seleccion> accion) {
        Seleccion seleccion = obtener(tabla);
        if (seleccion == null) return false;
        accion.accept(seleccion);
        return true;
    }

    public static Seleccion obtener(JTable tabla) {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) return null;
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Object id = tabla.getModel().getValueAt(filaModelo, 0);
        Object codigo = tabla.getModel().getValueAt(filaModelo, 1);
        return new Seleccion(Integer.parseInt(String.valueOf(id)), String.valueOf(codigo));
    }

    public record Seleccion(int idMantenimiento, String codigoEquipo) { }
}
