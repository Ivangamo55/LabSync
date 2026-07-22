package labsync.interfaz.comun;

import java.sql.Connection;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import labsync.configuracion.ConexionBaseDatos;
import labsync.modelo.Alerta;
import labsync.servicio.ServicioAlertas;

/** Campana compartida por profesor y alumno para resoluciones de sus reservas. */
public final class ControlNotificacionesReserva {
    private final JFrame ventana;
    private final ServicioAlertas servicio = new ServicioAlertas();
    private final int idUsuario;
    private final String nombreCompleto;
    private final String rol;
    private final Runnable abrirReservas;
    private final JButton boton = new JButton("🔔 0");
    private final String destinatarioNotificaciones;
    private final javax.swing.Timer temporizador;
    private java.awt.TrayIcon icono;
    private boolean actualizando;

    public ControlNotificacionesReserva(JFrame ventana, JPanel encabezado, int idUsuario,
            String nombreCompleto, String rol, Runnable abrirReservas) {
        this.ventana = ventana;
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.destinatarioNotificaciones = rol + ":" + idUsuario;
        this.abrirReservas = abrirReservas;
        configurarBoton(encabezado);
        temporizador = new javax.swing.Timer(7_000, e -> actualizar(true));
        temporizador.setInitialDelay(7_000);
        temporizador.start();
        ventana.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { detener(); }
        });
        actualizar(true);
    }

    private void configurarBoton(JPanel encabezado) {
        boton.setToolTipText("Notificaciones de tus reservas");
        boton.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 15));
        boton.setForeground(new java.awt.Color(8, 173, 141));
        boton.setBackground(java.awt.Color.WHITE);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        boton.addActionListener(e -> abrirCentro());
        encabezado.add(boton, new org.netbeans.lib.awtextra.AbsoluteConstraints(575, 36, 90, 42));
        encabezado.setComponentZOrder(boton, 0);
    }

    private List<Alerta> cargar(Connection conexion, boolean sincronizar) throws Exception {
        if (sincronizar) servicio.sincronizar(conexion);
        return servicio.listarResolucionesReserva(conexion, idUsuario, nombreCompleto, rol);
    }

    private void actualizar(boolean sincronizar) {
        if (actualizando) return;
        actualizando = true;
        boton.setEnabled(false);
        new SwingWorker<List<Alerta>, Void>() {
            @Override protected List<Alerta> doInBackground() throws Exception {
                try (Connection conexion = ConexionBaseDatos.conectar()) {
                    return conexion == null ? null : cargar(conexion, sincronizar);
                }
            }
            @Override protected void done() {
                try {
                    List<Alerta> alertas = get();
                    boton.setText(alertas == null ? "🔔 -" : "🔔 " + alertas.size());
                    if (alertas != null) avisarNuevas(alertas);
                } catch (Exception ex) {
                    boton.setText("🔔 E");
                } finally {
                    actualizando = false;
                    boton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void avisarNuevas(List<Alerta> alertas) {
        // El registro compartido sobrevive al cambio de ventana y evita repetir sonidos.
        List<Alerta> nuevas = RegistroNotificacionesConocidas.detectarNuevas(
                destinatarioNotificaciones, alertas);
        if (nuevas.isEmpty()) return;
        Alerta principal = nuevas.get(0);
        if ("RESERVA_RECHAZADA".equals(principal.tipo())) {
            SonidosNotificacion.reservaRechazada();
        } else {
            SonidosNotificacion.reservaAprobada();
        }
        if (java.awt.SystemTray.isSupported()) {
            try {
                if (icono == null) {
                    java.awt.Image imagen = Recursos
                            .icono("/images/logo_labsync_no_background.png").getImage();
                    icono = new java.awt.TrayIcon(imagen, "LabSync");
                    icono.setImageAutoSize(true);
                    icono.addActionListener(e -> abrirCentro());
                    java.awt.SystemTray.getSystemTray().add(icono);
                }
                icono.displayMessage(principal.titulo(), principal.detalle(),
                        "RESERVA_RECHAZADA".equals(principal.tipo())
                                ? java.awt.TrayIcon.MessageType.WARNING
                                : java.awt.TrayIcon.MessageType.INFO);
                return;
            } catch (java.awt.AWTException ignored) { }
        }
    }

    private void abrirCentro() {
        new DialogoAlertas(ventana, servicio, () -> actualizar(false), alerta -> {
            try (Connection conexion = ConexionBaseDatos.conectar()) {
                if (conexion != null) servicio.marcarAtendida(conexion, alerta.id());
            } catch (Exception ignored) { }
            abrirReservas.run();
        }, this::cargar).setVisible(true);
    }

    private void detener() {
        temporizador.stop();
        if (icono != null && java.awt.SystemTray.isSupported()) {
            java.awt.SystemTray.getSystemTray().remove(icono);
        }
    }
}
