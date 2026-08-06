package labsync.interfaz.comun;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

/** Reemplaza las opciones de un combo solo cuando su conjunto realmente cambia. */
public final class ActualizadorModeloCombo {
    private ActualizadorModeloCombo() { }

    public static boolean aplicar(JComboBox<String> combo, List<String> valores,
            String opcionGeneral, Comparator<String> comparador) {
        List<String> opciones = preparar(valores, opcionGeneral, comparador);
        if (iguales(combo, opciones)) return false;

        Object seleccionAnterior = combo.getSelectedItem();
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>(
                opciones.toArray(String[]::new));
        combo.setModel(modelo);
        if (seleccionAnterior != null && opciones.contains(seleccionAnterior.toString())) {
            combo.setSelectedItem(seleccionAnterior);
        } else {
            combo.setSelectedIndex(0);
        }
        return true;
    }

    static List<String> preparar(List<String> valores, String opcionGeneral,
            Comparator<String> comparador) {
        LinkedHashSet<String> unicos = new LinkedHashSet<>();
        valores.stream().filter(Objects::nonNull).filter(valor -> !valor.isBlank())
                .forEach(unicos::add);
        List<String> ordenados = new ArrayList<>(unicos);
        if (comparador != null) ordenados.sort(comparador);
        ordenados.add(0, opcionGeneral);
        return List.copyOf(ordenados);
    }

    private static boolean iguales(JComboBox<String> combo, List<String> opciones) {
        if (combo.getItemCount() != opciones.size()) return false;
        for (int indice = 0; indice < opciones.size(); indice++) {
            if (!Objects.equals(combo.getItemAt(indice), opciones.get(indice))) return false;
        }
        return true;
    }
}
