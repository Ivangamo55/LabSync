package labsync.interfaz.comun;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import labsync.modelo.Alerta;

/**
 * Conserva las alertas conocidas durante toda la ejecución de la aplicación.
 *
 * <p>Los controladores de notificaciones se recrean al navegar entre ventanas.
 * Guardar el estado aquí evita volver a reproducir el sonido por alertas que ya
 * estaban presentes. La primera consulta de cada usuario establece una línea
 * base silenciosa; únicamente los identificadores posteriores se consideran
 * notificaciones nuevas.</p>
 */
public final class RegistroNotificacionesConocidas {
    private static final Map<String, Set<Integer>> CONOCIDAS_POR_DESTINATARIO =
            new ConcurrentHashMap<>();

    private RegistroNotificacionesConocidas() { }

    public static synchronized List<Alerta> detectarNuevas(
            String destinatario, List<Alerta> actuales) {
        Set<Integer> idsActuales = new HashSet<>();
        for (Alerta alerta : actuales) idsActuales.add(alerta.id());

        Set<Integer> conocidas = CONOCIDAS_POR_DESTINATARIO.get(destinatario);
        if (conocidas == null) {
            // Primera carga: muestra el contador, pero no anuncia alertas históricas.
            CONOCIDAS_POR_DESTINATARIO.put(destinatario, idsActuales);
            return List.of();
        }

        List<Alerta> nuevas = new ArrayList<>();
        for (Alerta alerta : actuales) {
            if (!conocidas.contains(alerta.id())) nuevas.add(alerta);
        }

        // Se eliminan IDs inactivos para permitir detectar una alerta realmente
        // regenerada en el futuro con un identificador diferente.
        conocidas.retainAll(idsActuales);
        conocidas.addAll(idsActuales);
        return nuevas;
    }
}
