package labsync.interfaz.comun;

import labsync.modelo.Alerta;
import labsync.aplicacion.AplicacionLabSync;
import labsync.configuracion.ConexionBaseDatos;
import labsync.servicio.ServicioAlertas;
import labsync.interfaz.mantenimiento.VentanaGestionMantenimiento;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

/** Ventana compacta para consultar y atender las alertas del laboratorista. */
public final class DialogoAlertas extends JDialog {
    @FunctionalInterface
    public interface CargadorAlertas {
        List<Alerta> cargar(Connection conexion, boolean sincronizar) throws Exception;
    }
    private static final Color VERDE = new Color(8, 173, 141);
    private static final Color FONDO = new Color(245, 247, 249);

    private final ServicioAlertas alertaService;
    private final Runnable alActualizar;
    private final java.util.function.Consumer<Alerta> alResolver;
    private final ModeloAlertas modelo;
    private final JTable tabla;
    private final TableRowSorter<ModeloAlertas> ordenador;
    private final JScrollPane scrollTabla;
    private final JTextArea detalle = new JTextArea();
    private final JLabel resumen = new JLabel("Cargando alertas...");
    private final JButton btnResolver = new JButton("Ir a resolver");
    private final JButton btnActualizar = new JButton("Actualizar");
    private List<Alerta> alertas = List.of();
    private final javax.swing.Timer temporizador;
    private boolean actualizando;
    private final CargadorAlertas cargador;

    public DialogoAlertas(Frame propietario, ServicioAlertas alertaService, Runnable alActualizar,
            java.util.function.Consumer<Alerta> alResolver) {
        this(propietario, alertaService, alActualizar, alResolver, (conexion, sincronizar) -> {
            if (sincronizar) alertaService.sincronizar(conexion);
            return alertaService.listar(conexion, true);
        });
    }

    public DialogoAlertas(Frame propietario, ServicioAlertas alertaService, Runnable alActualizar,
            java.util.function.Consumer<Alerta> alResolver, CargadorAlertas cargador) {
        super(propietario, "Centro de alertas · LabSync", true);
        this.alertaService = alertaService;
        this.alActualizar = alActualizar;
        this.alResolver = alResolver;
        this.cargador = cargador;
        modelo = new ModeloAlertas();
        tabla = new JTable(modelo);
        ordenador = new TableRowSorter<>(modelo);
        tabla.setRowSorter(ordenador);
        scrollTabla = new JScrollPane(tabla);
        temporizador = new javax.swing.Timer(7_000, e -> cargarAlertas(true));
        temporizador.setInitialDelay(7_000);
        inicializarComponentes();
        cargarAlertas(true);
        temporizador.start();
    }

    private void inicializarComponentes() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(780, 480));
        setSize(940, 560);
        setLocationRelativeTo(getOwner());
        getContentPane().setBackground(FONDO);
        setLayout(new BorderLayout(12, 12));

        JPanel encabezado = new JPanel(new BorderLayout(10, 5));
        encabezado.setBackground(Color.WHITE);
        encabezado.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));
        JLabel titulo = new JLabel("Centro de alertas");
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setForeground(VERDE);
        resumen.setForeground(new Color(85, 85, 85));
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(resumen, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);

        configurarTabla();
        detalle.setEditable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setFont(new Font("Arial", Font.PLAIN, 13));
        detalle.setBackground(Color.WHITE);
        detalle.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        detalle.setText("Selecciona una alerta para consultar su detalle.");

        JPanel panelDetalle = new JPanel(new BorderLayout());
        panelDetalle.setBorder(BorderFactory.createTitledBorder("Detalle"));
        panelDetalle.add(new JScrollPane(detalle), BorderLayout.CENTER);
        JSplitPane contenido = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                scrollTabla, panelDetalle);
        contenido.setResizeWeight(0.68);
        contenido.setDividerSize(7);
        contenido.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        add(contenido, BorderLayout.CENTER);

        btnActualizar.addActionListener(e -> cargarAlertas(true));
        btnResolver.addActionListener(e -> irAResolver());
        btnResolver.setBackground(VERDE);
        btnResolver.setForeground(Color.WHITE);
        btnResolver.setFocusPainted(false);

        JLabel ayuda = new JLabel("Los avisos desaparecen al resolver su causa");
        ayuda.setForeground(new Color(95, 95, 95));
        JPanel botones = new JPanel(new GridLayout(1, 2, 8, 0));
        botones.setOpaque(false);
        botones.add(btnActualizar);
        botones.add(btnResolver);
        JPanel acciones = new JPanel(new BorderLayout(10, 0));
        acciones.setBackground(FONDO);
        acciones.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
        acciones.add(ayuda, BorderLayout.WEST);
        acciones.add(botones, BorderLayout.EAST);
        add(acciones, BorderLayout.SOUTH);
        habilitarAcciones(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { temporizador.stop(); }
            @Override public void windowClosed(java.awt.event.WindowEvent e) { temporizador.stop(); }
        });
    }

    private void configurarTabla() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(30);
        tabla.setFillsViewportHeight(true);
        tabla.setShowVerticalLines(false);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(75);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(290);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(115);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(JTable table,
                    Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 7));
                setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                if (!selected) {
                    int modelRow = table.convertRowIndexToModel(row);
                    String prioridad = String.valueOf(modelo.getValueAt(modelRow, 0));
                    setBackground("Crítica".equals(prioridad) ? new Color(255, 239, 239)
                            : (row % 2 == 0 ? Color.WHITE : new Color(249, 250, 251)));
                    setForeground(Color.DARK_GRAY);
                }
                return this;
            }
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarDetalleSeleccionado();
        });
    }

    private void cargarAlertas(boolean sincronizar) {
        if (actualizando) return;
        actualizando = true;
        new SwingWorker<List<Alerta>, Void>() {
            @Override protected List<Alerta> doInBackground() throws Exception {
                try (Connection conexion = ConexionBaseDatos.conectar()) {
                    if (conexion == null) throw new IllegalStateException("No hay conexión con la base de datos.");
                    return cargador.cargar(conexion, sincronizar);
                }
            }

            @Override protected void done() {
                try {
                    aplicarAlertas(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    mostrarError("La actualización fue interrumpida.");
                } catch (ExecutionException ex) {
                    mostrarError(mensajeDe(ex.getCause()));
                } finally {
                    actualizando = false;
                }
            }
        }.execute();
    }

    private void aplicarAlertas(List<Alerta> datosNuevos) {
        Alerta seleccionada = alertaSeleccionada();
        Integer idSeleccionada = seleccionada == null ? null : seleccionada.id();
        Point posicionScroll = scrollTabla.getViewport().getViewPosition();
        List<? extends RowSorter.SortKey> ordenActual = List.copyOf(ordenador.getSortKeys());
        RowFilter<? super ModeloAlertas, ? super Integer> filtroActual = ordenador.getRowFilter();
        int cantidadAnterior = modelo.getRowCount();

        ModeloAlertas.ResultadoActualizacion resultado = modelo.actualizar(datosNuevos);
        if (!resultado.cambio()) return;

        alertas = modelo.alertas();
        ordenador.setRowFilter(filtroActual);
        ordenador.setSortKeys(ordenActual);
        long criticas = alertas.stream().filter(a -> "Crítica".equals(a.prioridad())).count();
        resumen.setText(alertas.isEmpty() ? "No hay acciones pendientes"
                : alertas.size() + (alertas.size() == 1 ? " aviso activo" : " avisos activos")
                        + (criticas > 0 ? " · " + criticas + " críticos" : ""));

        restaurarSeleccion(idSeleccionada);
        SwingUtilities.invokeLater(() -> scrollTabla.getViewport().setViewPosition(posicionScroll));

        if (!resultado.nuevas().isEmpty() || cantidadAnterior != modelo.getRowCount()) {
            // El controlador propietario actualiza el contador y ejecuta su mecanismo
            // existente de RegistroNotificacionesConocidas, aviso visual y sonido.
            alActualizar.run();
        }
    }

    private void restaurarSeleccion(Integer idAlerta) {
        if (idAlerta == null) {
            tabla.clearSelection();
            mostrarDetalleSeleccionado();
            return;
        }
        int filaModelo = modelo.indicePorId(idAlerta);
        int filaVista = filaModelo < 0 ? -1 : tabla.convertRowIndexToView(filaModelo);
        if (filaVista < 0) {
            tabla.clearSelection();
            mostrarDetalleSeleccionado();
            return;
        }
        tabla.getSelectionModel().setSelectionInterval(filaVista, filaVista);
        mostrarDetalleSeleccionado();
    }

    private void irAResolver() {
        Alerta seleccionada = alertaSeleccionada();
        if (seleccionada == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una alerta.",
                    "Centro de alertas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        dispose();
        alResolver.accept(seleccionada);
    }

    private Alerta alertaSeleccionada() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) return null;
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        return modelo.alertaEn(filaModelo);
    }

    private void mostrarDetalleSeleccionado() {
        Alerta alerta = alertaSeleccionada();
        detalle.setText(alerta == null ? "Selecciona una alerta para consultar su detalle."
                : alerta.titulo() + "\n\n" + alerta.detalle());
        detalle.setCaretPosition(0);
        habilitarAcciones(alerta != null);
    }

    private void habilitarAcciones(boolean habilitar) {
        btnResolver.setEnabled(habilitar);
    }

    private String mensajeDe(Throwable causa) {
        return causa == null || causa.getMessage() == null
                ? "No fue posible completar la operación." : causa.getMessage();
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Centro de alertas", JOptionPane.ERROR_MESSAGE);
    }
}
