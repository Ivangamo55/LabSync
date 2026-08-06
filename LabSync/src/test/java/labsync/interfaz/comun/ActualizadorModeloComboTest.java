package labsync.interfaz.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ActualizadorModeloComboTest {
    @Test
    void tresCiclosIdenticosNoReemplazanModeloNiGeneranEventos() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<String> combo = combo("Todos", "LAB-1", "LAB-2");
            var modelo = combo.getModel();
            AtomicInteger eventos = new AtomicInteger();
            combo.addActionListener(evento -> eventos.incrementAndGet());
            for (int ciclo = 0; ciclo < 3; ciclo++) {
                assertFalse(ActualizadorModeloCombo.aplicar(combo,
                        List.of("LAB-1", "LAB-2"), "Todos", null));
            }
            assertSame(modelo, combo.getModel());
            assertEquals(0, eventos.get());
        });
    }

    @Test
    void conservaSeleccionYActualizaCuandoCambianOpciones() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<String> combo = combo("Todos", "LAB-1", "LAB-2");
            combo.setSelectedItem("LAB-2");
            assertTrue(ActualizadorModeloCombo.aplicar(combo,
                    List.of("LAB-2", "LAB-3"), "Todos", null));
            assertEquals("LAB-2", combo.getSelectedItem());
            assertEquals(List.of("Todos", "LAB-2", "LAB-3"), elementos(combo));
        });
    }

    @Test
    void seleccionInexistenteVuelveDeFormaSeguraATodos() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<String> combo = combo("Todos", "LAB-1");
            combo.setSelectedItem("LAB-1");
            assertTrue(ActualizadorModeloCombo.aplicar(combo,
                    List.of("LAB-2"), "Todos", null));
            assertEquals("Todos", combo.getSelectedItem());
        });
    }

    private static JComboBox<String> combo(String... valores) {
        return new JComboBox<>(new DefaultComboBoxModel<>(valores));
    }

    private static List<String> elementos(JComboBox<String> combo) {
        return java.util.stream.IntStream.range(0, combo.getItemCount())
                .mapToObj(combo::getItemAt).toList();
    }
}
