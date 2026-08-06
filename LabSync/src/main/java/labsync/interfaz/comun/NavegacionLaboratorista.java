package labsync.interfaz.comun;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import labsync.interfaz.horarios.VentanaGestionHorarios;
import labsync.modelo.SesionUsuario;

/** Acceso común a Ciclos y horarios desde los menús del laboratorista. */
public final class NavegacionLaboratorista {
    private static final String NOMBRE_BOTON = "btnCiclosHorarios";
    private static final String NAVEGACION_EN_CURSO =
            NavegacionLaboratorista.class.getName() + ".enCurso";

    private NavegacionLaboratorista() { }

    public static void agregarAccesoHorarios(JFrame origen, JPanel menu,
            Supplier<SesionUsuario> sesionActual) {
        agregarAccesoHorarios(menu, () -> abrirHorarios(origen, sesionActual.get()));
    }

    static void agregarAccesoHorarios(JPanel menu, Runnable navegacion) {
        for (java.awt.Component componente : menu.getComponents()) {
            if (NOMBRE_BOTON.equals(componente.getName())) return;
        }
        int y = posicionDespuesDeMantenimiento(menu);
        desplazarElementosPosteriores(menu, y);
        JButton boton = crearBotonHorarios(navegacion);
        menu.add(boton, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, y, 200, 50));
        menu.revalidate();
        menu.repaint();
    }

    static JButton crearBotonHorarios(Runnable navegacion) {
        JButton boton = new JButton("Ciclos y horarios");
        boton.setName(NOMBRE_BOTON);
        boton.setBackground(java.awt.Color.WHITE);
        boton.setForeground(new java.awt.Color(6, 140, 115));
        boton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        boton.setPreferredSize(new java.awt.Dimension(200, 50));
        AtomicBoolean navegando = new AtomicBoolean();
        boton.addActionListener(evento -> {
            if (!navegando.compareAndSet(false, true)) return;
            boton.setEnabled(false);
            SwingUtilities.invokeLater(() -> {
                try {
                    navegacion.run();
                } catch (RuntimeException ex) {
                    navegando.set(false);
                    boton.setEnabled(true);
                    java.util.logging.Logger.getLogger(NavegacionLaboratorista.class.getName())
                            .log(java.util.logging.Level.SEVERE,
                                    "No se pudo abrir Ciclos y horarios", ex);
                }
            });
        });
        return boton;
    }

    private static void abrirHorarios(JFrame origen, SesionUsuario sesion) {
        abrir(origen, new VentanaGestionHorarios(sesion));
    }

    /** Abre una ventana principal conservando la geometría y el estado del origen. */
    public static boolean abrir(JFrame origen, JFrame destino) {
        if (origen == null || destino == null) {
            throw new IllegalArgumentException("Las ventanas de navegación son obligatorias.");
        }
        if (Boolean.TRUE.equals(origen.getRootPane().getClientProperty(NAVEGACION_EN_CURSO))) {
            destino.dispose();
            return false;
        }
        origen.getRootPane().putClientProperty(NAVEGACION_EN_CURSO, Boolean.TRUE);

        int estado = origen.getExtendedState();
        boolean maximizada = (estado & JFrame.MAXIMIZED_BOTH) != 0;
        Rectangle limites = origen.getBounds();
        Point posicionVisible = origen.isShowing()
                ? origen.getLocationOnScreen() : limites.getLocation();
        Rectangle limitesDestino = ajustarAlMinimo(limites, destino.getMinimumSize());
        Insets decoracionOrigen = origen.getInsets();
        if (!maximizada) {
            limitesDestino = compensarDecoracion(limitesDestino,
                    decoracionOrigen, destino.getInsets());
        }
        destino.setBounds(limitesDestino);
        if (maximizada) {
            destino.setExtendedState(estado | JFrame.MAXIMIZED_BOTH);
        } else {
            destino.setExtendedState(estado & ~JFrame.MAXIMIZED_BOTH);
        }
        destino.setVisible(true);
        if (maximizada) {
            destino.setExtendedState(estado | JFrame.MAXIMIZED_BOTH);
        } else {
            // El peer puede conocer la decoración apenas al mostrarse (XWayland).
            destino.setBounds(compensarDecoracion(
                    ajustarAlMinimo(limites, destino.getMinimumSize()),
                    decoracionOrigen, destino.getInsets()));
            estabilizarPosicion(destino, posicionVisible);
        }
        origen.dispose();
        return true;
    }

    static Rectangle ajustarAlMinimo(Rectangle limites, Dimension minimo) {
        Rectangle seguros = limites == null || limites.width <= 0 || limites.height <= 0
                ? new Rectangle(0, 0, Math.max(1, minimo.width), Math.max(1, minimo.height))
                : new Rectangle(limites);
        seguros.width = Math.max(seguros.width, Math.max(1, minimo.width));
        seguros.height = Math.max(seguros.height, Math.max(1, minimo.height));
        return seguros;
    }

    /*
     * Algunos gestores de ventanas informan los insets del JFrame empacado hasta
     * su primer mapeo y después los integran en getBounds(). Compensar únicamente
     * la diferencia evita acumular la altura de la barra de título en cada salto.
     */
    static Rectangle compensarDecoracion(Rectangle limites, Insets origen, Insets destino) {
        Rectangle compensados = new Rectangle(limites);
        compensados.x += origen.left - destino.left;
        compensados.y += origen.top - destino.top;
        return compensados;
    }

    private static void estabilizarPosicion(JFrame destino, Point esperada) {
        javax.swing.Timer ajuste = new javax.swing.Timer(45, null);
        final int[] intentos = {0};
        ajuste.addActionListener(evento -> {
            if (!destino.isShowing() || intentos[0]++ >= 6) {
                ajuste.stop();
                return;
            }
            Point actual = destino.getLocationOnScreen();
            int dx = esperada.x - actual.x;
            int dy = esperada.y - actual.y;
            if (dx == 0 && dy == 0) return;
            Point informada = destino.getLocation();
            destino.setLocation(informada.x + dx, informada.y + dy);
        });
        ajuste.setInitialDelay(45);
        ajuste.start();
    }

    private static int posicionDespuesDeMantenimiento(JPanel menu) {
        Integer posicionReservas = null;
        for (java.awt.Component componente : menu.getComponents()) {
            if (componente instanceof JButton boton && boton.getText() != null) {
                String texto = boton.getText().toLowerCase(java.util.Locale.ROOT);
                if (texto.contains("mantenimiento")) {
                    return boton.getY() + Math.max(60, boton.getHeight() + 10);
                }
                if (texto.contains("reserva")) posicionReservas = boton.getY();
            }
        }
        // En la propia ventana de mantenimiento el acceso activo no es botón;
        // Reservas ocupa temporalmente su siguiente posición.
        return posicionReservas == null ? 590 : posicionReservas;
    }

    private static void desplazarElementosPosteriores(JPanel menu, int yNuevo) {
        for (java.awt.Component componente : menu.getComponents().clone()) {
            if (componente.getY() >= yNuevo) {
                java.awt.Rectangle limites = componente.getBounds();
                menu.remove(componente);
                menu.add(componente, new org.netbeans.lib.awtextra.AbsoluteConstraints(
                        limites.x, limites.y + 60, limites.width, limites.height));
            }
        }
    }

}
