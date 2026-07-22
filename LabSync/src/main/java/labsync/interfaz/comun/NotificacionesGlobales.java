package labsync.interfaz.comun;

import javax.swing.JFrame;
import javax.swing.JPanel;
import labsync.interfaz.panel.VentanaPanelAlumno;
import labsync.interfaz.reservas.VentanaMisReservasProfesor;
import labsync.modelo.SesionUsuario;

/** Punto único para instalar notificaciones según el rol de la ventana. */
public final class NotificacionesGlobales {
    private NotificacionesGlobales() { }

    public static void profesor(JFrame ventana, JPanel encabezado, SesionUsuario sesion) {
        new ControlNotificacionesReserva(ventana, encabezado, sesion.getId(),
                sesion.getNombreCompleto(), "Profesor", () -> {
                    VentanaMisReservasProfesor destino = new VentanaMisReservasProfesor(sesion);
                    destino.setLocationRelativeTo(null);
                    destino.setVisible(true);
                    ventana.dispose();
                });
    }

    public static void alumno(JFrame ventana, JPanel encabezado, String nombreUsuario) {
        SesionUsuario sesion = SesionUsuario.buscarEstudiante(nombreUsuario);
        new ControlNotificacionesReserva(ventana, encabezado, sesion.getId(),
                sesion.getNombreCompleto(), "Estudiante", () -> {
                    VentanaPanelAlumno destino = new VentanaPanelAlumno(sesion.getNombre());
                    destino.setLocationRelativeTo(null);
                    destino.setVisible(true);
                    ventana.dispose();
                });
    }

    public static void laboratorista(JFrame ventana, JPanel encabezado, String nombreUsuario) {
        new ControlNotificacionesLaboratorista(ventana, encabezado, nombreUsuario);
    }
}
