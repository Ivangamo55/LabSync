package labsync.interfaz.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import labsync.interfaz.horarios.VentanaGestionHorarios;
import labsync.interfaz.inventario.VentanaGestionInventario;
import labsync.interfaz.mantenimiento.VentanaGestionMantenimiento;
import labsync.modelo.SesionUsuario;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class NavegacionLaboratoristaTest {

    @Test
    void accesoSeInsertaDespuesDeMantenimientoSinDuplicarse() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            javax.swing.JPanel menu = new javax.swing.JPanel(new org.netbeans.lib.awtextra.AbsoluteLayout());
            JButton mantenimiento = new JButton("Mantenimiento");
            JButton reservas = new JButton("Reservas");
            menu.add(mantenimiento,new org.netbeans.lib.awtextra.AbsoluteConstraints(20,410,200,50));
            menu.add(reservas,new org.netbeans.lib.awtextra.AbsoluteConstraints(20,470,200,50));
            menu.setSize(250,720); menu.doLayout();
            NavegacionLaboratorista.agregarAccesoHorarios(menu, () -> { });
            NavegacionLaboratorista.agregarAccesoHorarios(menu, () -> { });
            menu.doLayout();
            JButton boton=null;
            for(java.awt.Component componente:menu.getComponents())
                if(componente instanceof JButton b&&"Ciclos y horarios".equals(b.getText()))boton=b;
            assertNotNull(boton);
            assertEquals(470,boton.getY());
            assertEquals(530,reservas.getY());
            assertNull(boton.getIcon());
            assertEquals(3,menu.getComponentCount());
        });
    }

    @Test
    void botonConservaEstiloYUnDobleClicSoloNavegaUnaVez() throws Exception {
        AtomicInteger aperturas = new AtomicInteger();
        JButton[] referencia = new JButton[1];

        SwingUtilities.invokeAndWait(() -> {
            JButton boton = NavegacionLaboratorista.crearBotonHorarios(
                    aperturas::incrementAndGet);
            referencia[0] = boton;
            assertEquals("Ciclos y horarios", boton.getText());
            assertEquals(new java.awt.Dimension(200, 50), boton.getPreferredSize());
            assertEquals(new java.awt.Color(6, 140, 115), boton.getForeground());
            boton.doClick(0);
            boton.doClick(0);
        });
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(1, aperturas.get());
        assertFalse(referencia[0].isEnabled());
    }

    @Test
    void ajustaSoloCuandoElDestinoExigeUnMinimoMayor() {
        Rectangle original = new Rectangle(80, 60, 1100, 720);
        assertEquals(original, NavegacionLaboratorista.ajustarAlMinimo(
                original, new java.awt.Dimension(900, 600)));
        assertEquals(new Rectangle(80, 60, 1300, 800),
                NavegacionLaboratorista.ajustarAlMinimo(
                        original, new java.awt.Dimension(1300, 800)));
    }

    @Test
    void compensaSoloLaDiferenciaDeDecoracionEntreVentanas() {
        Rectangle limites = new Rectangle(120, 147, 1280, 780);
        assertEquals(new Rectangle(120, 110, 1280, 780),
                NavegacionLaboratorista.compensarDecoracion(limites,
                        new java.awt.Insets(0, 0, 0, 0),
                        new java.awt.Insets(37, 0, 0, 0)));
        assertEquals(limites, NavegacionLaboratorista.compensarDecoracion(limites,
                new java.awt.Insets(30, 4, 4, 4),
                new java.awt.Insets(30, 4, 4, 4)));
    }

    @Test
    void inventarioHorariosMantenimientoInventarioConservaBoundsYSesion() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SesionUsuario sesion = new SesionUsuario(77, "Prueba", "Tester", "Laboratorista");
        Rectangle limites = new Rectangle(40, 100, 1400, 850);
        JFrame[] ventanas = new JFrame[4];
        java.awt.Dimension[] dimensionAplicada = new java.awt.Dimension[1];
        java.awt.Point[] posicionAplicada = new java.awt.Point[1];

        SwingUtilities.invokeAndWait(() -> {
            ventanas[0] = new VentanaGestionInventario(sesion);
            ventanas[0].setBounds(limites);
            ventanas[0].setVisible(true);
        });
        vaciarEdt();
        SwingUtilities.invokeAndWait(() -> posicionAplicada[0] = ventanas[0].getLocationOnScreen());
        SwingUtilities.invokeAndWait(() -> {
            ventanas[1] = new VentanaGestionHorarios(sesion);
            dimensionAplicada[0] = ventanas[0].getSize();
            assertTrue(NavegacionLaboratorista.abrir(ventanas[0], ventanas[1]));
        });
        vaciarEdt();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(dimensionAplicada[0], ventanas[1].getSize());
            assertEquals(posicionAplicada[0], ventanas[1].getLocationOnScreen());
            assertSame(sesion, campoSesion(ventanas[1]));

            ventanas[2] = new VentanaGestionMantenimiento(sesion);
            assertTrue(NavegacionLaboratorista.abrir(ventanas[1], ventanas[2]));
        });
        vaciarEdt();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(dimensionAplicada[0], ventanas[2].getSize());
            assertEquals(posicionAplicada[0], ventanas[2].getLocationOnScreen());
            assertSame(sesion, campoSesion(ventanas[2]));

            ventanas[3] = new VentanaGestionInventario(sesion);
            assertTrue(NavegacionLaboratorista.abrir(ventanas[2], ventanas[3]));
        });
        vaciarEdt();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(dimensionAplicada[0], ventanas[3].getSize());
            assertEquals(posicionAplicada[0], ventanas[3].getLocationOnScreen());
            assertSame(sesion, campoSesion(ventanas[3]));
            ventanas[3].dispose();
        });
    }

    @Test
    void conservaMaximizadoYEvitaSegundaApertura() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            JFrame origen = new JFrame();
            origen.setBounds(25, 30, 1000, 700);
            origen.setVisible(true);
            origen.setExtendedState(JFrame.MAXIMIZED_BOTH);
            JFrame destino = new JFrame();
            assertTrue(NavegacionLaboratorista.abrir(origen, destino));
            assertTrue((destino.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0);
            JFrame duplicada = new JFrame();
            assertFalse(NavegacionLaboratorista.abrir(origen, duplicada));
            assertFalse(duplicada.isDisplayable());
            destino.dispose();
        });
    }

    @Test
    void conservaBoundsExactosEnCadenaSinIntervencionDelGestorDeVentanas() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Rectangle limites = new Rectangle(120, 140, 900, 650);
            JFrame a = new JFrame(); a.setUndecorated(true); a.setBounds(limites); a.setVisible(true);
            JFrame b = new JFrame(); b.setUndecorated(true);
            assertTrue(NavegacionLaboratorista.abrir(a, b));
            assertEquals(limites, b.getBounds());
            JFrame c = new JFrame(); c.setUndecorated(true);
            assertTrue(NavegacionLaboratorista.abrir(b, c));
            assertEquals(limites, c.getBounds());
            c.dispose();
        });
    }

    private static Object campoSesion(JFrame ventana) {
        try {
            java.lang.reflect.Field campo = ventana.getClass().getDeclaredField("sesion");
            campo.setAccessible(true);
            return campo.get(ventana);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static void vaciarEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        Thread.sleep(500);
        SwingUtilities.invokeAndWait(() -> { });
    }

}
