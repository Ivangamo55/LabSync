package labsync.interfaz.comun;

import java.sql.Connection;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import labsync.configuracion.ConexionBaseDatos;
import labsync.interfaz.fallas.VentanaGestionReportesFallas;
import labsync.interfaz.inventario.VentanaGestionInventario;
import labsync.interfaz.mantenimiento.VentanaGestionMantenimiento;
import labsync.interfaz.reservas.VentanaGestionReservas;
import labsync.modelo.Alerta;
import labsync.servicio.ServicioAlertas;

/** Campana reutilizable del laboratorista para cualquier ventana de su interfaz. */
public final class ControlNotificacionesLaboratorista {
    private final JFrame ventana;
    private final String nombreUsuario;
    private final ServicioAlertas servicio = new ServicioAlertas();
    private final JButton boton = new JButton("🔔 0");
    private final javax.swing.Timer temporizador;
    private java.awt.TrayIcon icono;
    private boolean actualizando;

    public ControlNotificacionesLaboratorista(JFrame ventana, JPanel encabezado,
            String nombreUsuario) {
        this.ventana = ventana;
        this.nombreUsuario = nombreUsuario;
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
        boton.setToolTipText("Consultar alertas");
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
        return servicio.listar(conexion, true);
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
        // Todas las ventanas del laboratorista comparten la misma línea base sonora.
        List<Alerta> nuevas = RegistroNotificacionesConocidas.detectarNuevas(
                "LABORATORISTA", alertas);
        if (nuevas.isEmpty()) return;
        Alerta principal = nuevas.get(0);
        SonidosNotificacion.solicitudPendiente();
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
                        java.awt.TrayIcon.MessageType.INFO);
            } catch (java.awt.AWTException ignored) { }
        }
    }

    private void abrirCentro() {
        new DialogoAlertas(ventana, servicio, () -> actualizar(false), this::abrirModulo,
                this::cargar).setVisible(true);
    }

    private void abrirModulo(Alerta alerta) {
        JFrame destino;
        if ("FALLA_PENDIENTE".equals(alerta.tipo())) {
            destino = new VentanaGestionReportesFallas(nombreUsuario);
        } else if ("RESERVA_PENDIENTE".equals(alerta.tipo())) {
            destino = new VentanaGestionReservas(nombreUsuario);
        } else if ("MANTENIMIENTO_REQUERIDO".equals(alerta.tipo())) {
            destino = new VentanaGestionMantenimiento(nombreUsuario, alerta.referencia());
        } else if (alerta.tipo().startsWith("MANTENIMIENTO")
                || "SOFTWARE_ACTUALIZACION".equals(alerta.tipo())) {
            destino = new VentanaGestionMantenimiento(nombreUsuario);
        } else {
            destino = new VentanaGestionInventario(nombreUsuario, alerta.referencia());
        }
        destino.setLocationRelativeTo(null);
        destino.setVisible(true);
        ventana.dispose();
    }

    private void detener() {
        temporizador.stop();
        if (icono != null && java.awt.SystemTray.isSupported()) {
            java.awt.SystemTray.getSystemTray().remove(icono);
        }
    }
}
