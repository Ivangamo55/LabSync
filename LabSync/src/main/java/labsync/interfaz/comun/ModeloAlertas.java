package labsync.interfaz.comun;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import labsync.modelo.Alerta;

/** Modelo estable que reconcilia alertas sin reemplazar la instancia usada por JTable. */
public final class ModeloAlertas extends AbstractTableModel {
    private static final String[] COLUMNAS = {"Prioridad", "Tipo", "Aviso", "Detectado"};
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final List<Alerta> alertas = new ArrayList<>();
    private boolean inicializado;

    public ResultadoActualizacion actualizar(List<Alerta> nuevasAlertas) {
        List<Alerta> datosNuevos = List.copyOf(nuevasAlertas);
        if (inicializado && alertas.equals(datosNuevos)) {
            return new ResultadoActualizacion(false, List.of());
        }
        Set<Integer> idsAnteriores = new HashSet<>();
        for (Alerta alerta : alertas) idsAnteriores.add(alerta.id());
        List<Alerta> incorporadas = datosNuevos.stream()
                .filter(alerta -> !idsAnteriores.contains(alerta.id())).toList();
        alertas.clear();
        alertas.addAll(datosNuevos);
        inicializado = true;
        fireTableDataChanged();
        return new ResultadoActualizacion(true, incorporadas);
    }

    public Alerta alertaEn(int filaModelo) {
        return filaModelo >= 0 && filaModelo < alertas.size() ? alertas.get(filaModelo) : null;
    }

    public int indicePorId(int idAlerta) {
        for (int i = 0; i < alertas.size(); i++) {
            if (alertas.get(i).id() == idAlerta) return i;
        }
        return -1;
    }

    public List<Alerta> alertas() {
        return List.copyOf(alertas);
    }

    @Override public int getRowCount() { return alertas.size(); }
    @Override public int getColumnCount() { return COLUMNAS.length; }
    @Override public String getColumnName(int column) { return COLUMNAS[column]; }
    @Override public Class<?> getColumnClass(int columnIndex) { return String.class; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Alerta alerta = alertas.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> alerta.prioridad();
            case 1 -> nombreTipo(alerta.tipo());
            case 2 -> alerta.titulo();
            case 3 -> alerta.fechaCreacion() == null ? "" : FORMATO_FECHA.format(alerta.fechaCreacion());
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + columnIndex);
        };
    }

    private String nombreTipo(String tipo) {
        return switch (tipo) {
            case "MANTENIMIENTO_VENCIDO" -> "Mantenimiento vencido";
            case "MANTENIMIENTO_PROXIMO" -> "Mantenimiento próximo";
            case "MANTENIMIENTO_REQUERIDO" -> "Mantenimiento requerido";
            case "SOFTWARE_ACTUALIZACION", "ACTUALIZACION_SOFTWARE" -> "Actualizar software";
            case "ACTUALIZACION_HARDWARE" -> "Actualizar hardware";
            case "DISPOSICION_PELIGROSA" -> "Disposición peligrosa";
            case "RETIRO_EQUIPO_OBSOLETO" -> "Retiro de equipo";
            case "MANTENIMIENTO_PREVENTIVO" -> "Mantenimiento preventivo";
            case "MANTENIMIENTO_CORRECTIVO" -> "Mantenimiento correctivo";
            case "FALLA_PENDIENTE" -> "Falla pendiente";
            case "RESERVA_PENDIENTE" -> "Reserva pendiente";
            case "RESERVA_APROBADA" -> "Reserva autorizada";
            case "RESERVA_RECHAZADA" -> "Reserva rechazada";
            case "EQUIPO_BAJA" -> "Valorar baja";
            default -> "Revisión de equipo";
        };
    }

    public record ResultadoActualizacion(boolean cambio, List<Alerta> nuevas) { }
}
