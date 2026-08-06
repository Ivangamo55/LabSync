package labsync.interfaz.inventario;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import labsync.configuracion.ConexionBaseDatos;
import labsync.interfaz.comun.ActualizacionAutomatica;
import labsync.interfaz.comun.ActualizadorModeloTabla;
import labsync.modelo.SoftwareLaboratorio;
import labsync.servicio.ServicioSoftwareLaboratorio;

/** Pestaña acotada de control de software, construida con Swing estándar. */
final class PanelSoftwareLaboratorio extends JPanel {
    private static final Logger LOGGER =
            Logger.getLogger(PanelSoftwareLaboratorio.class.getName());
    static final String MENSAJE_ESQUEMA_DESACTUALIZADO =
            "La base de datos instalada no corresponde con la versión actual de LabSync.\n"
            + "Actualiza el esquema de la aplicación.";
    private static final Color VERDE = new Color(8, 173, 141);
    private static final Color VERDE_OSCURO = new Color(6, 140, 115);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final String[] COLUMNAS = {"ID", "Laboratorio", "Nombre",
        "Versión instalada", "Versión objetivo", "Uso académico", "Estado",
        "Fecha de revisión", "Observaciones", "ID laboratorio"};

    private final VentanaGestionInventario ventana;
    private final ServicioSoftwareLaboratorio servicio = new ServicioSoftwareLaboratorio();
    private final DefaultTableModel modelo = modeloVacio();
    private final JTable tabla = new JTable(modelo) {
        @Override public String getToolTipText(java.awt.event.MouseEvent evento) {
            int fila = rowAtPoint(evento.getPoint()), columna = columnAtPoint(evento.getPoint());
            if (fila < 0 || columna < 0) return null;
            Object valor = getValueAt(fila, columna);
            if (valor == null) return null;
            Component renderer = prepareRenderer(getCellRenderer(fila, columna), fila, columna);
            return renderer.getPreferredSize().width > getCellRect(fila, columna, false).width
                    ? valor.toString() : null;
        }
    };
    private final JComboBox<ItemLaboratorio> filtroLaboratorio = new JComboBox<>();
    private final JComboBox<String> filtroEstado = new JComboBox<>();
    private final JTextField filtroUso = new JTextField(12);
    private final JTextField filtroBusqueda = new JTextField(15);
    private final JLabel resumen = new JLabel("0 programas registrados · 0 requieren atención");
    private final JButton botonEditar = boton("Editar", new Color(13, 110, 253), Color.WHITE);
    private final JButton botonCambiarEstado = boton(
            "Cambiar estado", new Color(108, 117, 125), Color.WHITE);
    private final AtomicBoolean consultando = new AtomicBoolean();
    private final AtomicBoolean dialogoAbierto = new AtomicBoolean();
    private List<ItemLaboratorio> laboratorios = List.of();
    private volatile Filtros filtrosAplicados = new Filtros(null, null, "", "");

    PanelSoftwareLaboratorio(VentanaGestionInventario ventana) {
        super(new BorderLayout(0, 10));
        this.ventana = ventana;
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));
        add(crearEncabezadoYFiltros(), BorderLayout.NORTH);
        add(crearTabla(), BorderLayout.CENTER);
        add(crearAcciones(), BorderLayout.SOUTH);
        cargarLaboratoriosYDatos();
        new ActualizacionAutomatica<>(ventana, 7_000,
                () -> consultar(filtrosAplicados), resultado -> {
                    if (resultado.filtros().equals(filtrosAplicados)) aplicar(resultado.filas());
                });
    }

    private JPanel crearEncabezadoYFiltros() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        JLabel titulo = new JLabel("Software por laboratorio");
        titulo.setFont(new Font("Arial", Font.BOLD, 20));
        titulo.setForeground(new Color(48, 54, 60));
        resumen.setFont(new Font("Arial", Font.PLAIN, 12));
        resumen.setForeground(new Color(90, 97, 104));
        encabezado.add(titulo, BorderLayout.WEST);
        encabezado.add(resumen, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(crearFiltros(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearFiltros() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 220, 224)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        filtroEstado.setModel(new DefaultComboBoxModel<>(estadosConTodos()));
        filtroUso.setToolTipText("Ej. TSU - DSM");
        filtroBusqueda.setToolTipText("Nombre o versión");
        int columna = 0;
        columna = agregarFiltro(panel, "Laboratorio", filtroLaboratorio, columna, 1.0);
        columna = agregarFiltro(panel, "Estado", filtroEstado, columna, 1.15);
        columna = agregarFiltro(panel, "Uso académico", filtroUso, columna, 1.0);
        columna = agregarFiltro(panel, "Nombre o versión", filtroBusqueda, columna, 1.15);
        JButton buscar = boton("Buscar", VERDE, Color.WHITE);
        buscar.addActionListener(e -> buscar());
        JButton limpiar = boton("Limpiar", Color.WHITE, VERDE_OSCURO);
        limpiar.addActionListener(e -> limpiar());
        GridBagConstraints acciones = new GridBagConstraints();
        acciones.gridx = columna; acciones.gridy = 0; acciones.gridheight = 2;
        acciones.insets = new Insets(16, 6, 0, 0);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botones.setOpaque(false); botones.add(buscar); botones.add(limpiar);
        panel.add(botones, acciones);
        filtroBusqueda.addActionListener(e -> buscar());
        return panel;
    }

    private static int agregarFiltro(JPanel panel, String texto, Component componente,
            int columna, double peso) {
        GridBagConstraints etiqueta = new GridBagConstraints();
        etiqueta.gridx = columna; etiqueta.gridy = 0; etiqueta.anchor = GridBagConstraints.WEST;
        etiqueta.insets = new Insets(0, 0, 3, 8);
        panel.add(etiqueta(texto), etiqueta);
        GridBagConstraints campo = new GridBagConstraints();
        campo.gridx = columna; campo.gridy = 1; campo.weightx = peso;
        campo.fill = GridBagConstraints.HORIZONTAL; campo.insets = new Insets(0, 0, 0, 8);
        componente.setPreferredSize(new Dimension(140, 30));
        panel.add(componente, campo);
        return columna + 1;
    }

    private JScrollPane crearTabla() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(false);
        tabla.setRowSorter(new TableRowSorter<>(modelo));
        labsync.interfaz.comun.EstiloTablaLaboratorista.aplicar(tabla);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarAcciones());
        ajustarColumnas();
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(215, 220, 224)));
        return scroll;
    }

    private JPanel crearAcciones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        JButton registrar = boton("+ Registrar software", VERDE, Color.WHITE);
        registrar.setPreferredSize(new Dimension(175, 34));
        registrar.addActionListener(e -> editar(null));
        botonEditar.addActionListener(e -> { Integer id = idSeleccionado(); if (id != null) editar(id); });
        botonCambiarEstado.setPreferredSize(new Dimension(145, 32));
        botonCambiarEstado.addActionListener(e -> { Integer id = idSeleccionado(); if (id != null) editar(id); });
        JButton actualizar = boton("Actualizar", Color.WHITE, VERDE_OSCURO);
        actualizar.addActionListener(e -> buscar());
        panel.add(registrar); panel.add(botonEditar); panel.add(botonCambiarEstado); panel.add(actualizar);
        actualizarAcciones();
        return panel;
    }

    private void cargarLaboratoriosYDatos() {
        new SwingWorker<List<ItemLaboratorio>, Void>() {
            @Override protected List<ItemLaboratorio> doInBackground() throws Exception {
                List<ItemLaboratorio> items = new ArrayList<>();
                try (Connection con = ConexionBaseDatos.conectar()) {
                    if (con == null) return items;
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT id_laboratorio,nombre FROM laboratorios ORDER BY nombre");
                            ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) items.add(new ItemLaboratorio(rs.getInt(1), rs.getString(2)));
                    }
                }
                return items;
            }
            @Override protected void done() {
                try {
                    laboratorios = get();
                    filtroLaboratorio.removeAllItems();
                    filtroLaboratorio.addItem(new ItemLaboratorio(0, "Todos"));
                    laboratorios.forEach(filtroLaboratorio::addItem);
                    buscar();
                } catch (Exception ex) { mostrarError(ex); }
            }
        }.execute();
    }

    private void buscar() {
        ItemLaboratorio item = (ItemLaboratorio) filtroLaboratorio.getSelectedItem();
        String estado = filtroEstado.getSelectedIndex() <= 0 ? null
                : (String) filtroEstado.getSelectedItem();
        filtrosAplicados = new Filtros(item == null || item.id() == 0 ? null : item.id(),
                estado, filtroUso.getText().trim(), filtroBusqueda.getText().trim());
        refrescar(filtrosAplicados);
    }

    private void limpiar() {
        if (filtroLaboratorio.getItemCount() > 0) filtroLaboratorio.setSelectedIndex(0);
        filtroEstado.setSelectedIndex(0);
        filtroUso.setText(""); filtroBusqueda.setText("");
        buscar();
    }

    private void refrescar(Filtros filtros) {
        if (!consultando.compareAndSet(false, true)) return;
        new SwingWorker<Resultado, Void>() {
            @Override protected Resultado doInBackground() { return consultar(filtros); }
            @Override protected void done() {
                try {
                    Resultado resultado = get();
                    if (resultado.filtros().equals(filtrosAplicados)) aplicar(resultado.filas());
                } catch (Exception ex) { mostrarError(ex); }
                finally { consultando.set(false); }
            }
        }.execute();
    }

    private Resultado consultar(Filtros filtros) {
        try (Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) return new Resultado(filtros, List.of());
            return new Resultado(filtros, servicio.consultar(con, filtros.laboratorio(),
                    filtros.estado(), filtros.uso(), filtros.busqueda()));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private void aplicar(List<SoftwareLaboratorio> filas) {
        DefaultTableModel nuevos = modeloVacio();
        for (SoftwareLaboratorio s : filas) nuevos.addRow(new Object[]{s.id(), s.laboratorio(),
                s.nombre(), s.versionInstalada(), s.versionObjetivo(), s.usoAcademico(),
                s.estado(), s.fechaRevision(), s.observaciones(), s.idLaboratorio()});
        ActualizadorModeloTabla.aplicar(tabla, nuevos, 0, this::ajustarColumnas);
        ResumenSoftware conteo = calcularResumen(filas);
        resumen.setText(conteo.total() + " programas registrados · "
                + conteo.requierenAtencion() + " requieren atención");
        actualizarAcciones();
    }

    private void editar(Integer idSoftware) {
        if (!dialogoAbierto.compareAndSet(false, true)) return;
        SoftwareLaboratorio actual = idSoftware == null ? null : filaSeleccionada();
        try {
            DialogoSoftwareLaboratorio.Datos datos = new DialogoSoftwareLaboratorio(
                    ventana, laboratorios, actual).mostrar();
            if (datos == null) return;
            try (Connection con = ConexionBaseDatos.conectar()) {
                servicio.guardar(con, idSoftware, datos.idLaboratorio(), datos.nombre(),
                        datos.versionInstalada(), datos.versionObjetivo(), datos.usoAcademico(),
                        datos.estado(), datos.fechaRevision(), datos.observaciones());
                buscar();
            }
        } catch (Exception ex) {
            mostrarError(ex);
        } finally {
            dialogoAbierto.set(false);
        }
    }

    private SoftwareLaboratorio filaSeleccionada() {
        int vista = tabla.getSelectedRow();
        if (vista < 0) return null;
        int fila = tabla.convertRowIndexToModel(vista);
        return new SoftwareLaboratorio(numero(fila, 0), numero(fila, 9), valor(fila, 1),
                valor(fila, 2), valor(fila, 3), valor(fila, 4), valor(fila, 5),
                valor(fila, 6), modelo.getValueAt(fila, 7) instanceof LocalDate fecha ? fecha : null,
                valor(fila, 8));
    }

    private Integer idSeleccionado() {
        SoftwareLaboratorio fila = filaSeleccionada();
        if (fila == null) JOptionPane.showMessageDialog(ventana, "Selecciona un registro.");
        return fila == null ? null : fila.id();
    }

    private void ajustarColumnas() {
        int[] anchos = {0, 90, 180, 115, 115, 160, 165, 115, 0, 0};
        for (int i = 0; i < anchos.length && i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            if (anchos[i] == 0) {
                tabla.getColumnModel().getColumn(i).setMinWidth(0);
                tabla.getColumnModel().getColumn(i).setMaxWidth(0);
            }
        }
    }

    private static DefaultTableModel modeloVacio() {
        return new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }
    private static JButton boton(String texto, Color fondo, Color frente) {
        JButton boton = new JButton(texto); boton.setBackground(fondo); boton.setForeground(frente);
        boton.setFont(new Font("Arial", Font.BOLD, 12)); boton.setFocusPainted(false);
        boton.setPreferredSize(new Dimension(125, 32)); return boton;
    }
    private static JLabel etiqueta(String texto) { JLabel e = new JLabel(texto); e.setFont(new Font("Arial", Font.BOLD, 11)); return e; }
    private static String texto(String valor) { return valor == null ? "" : valor; }
    private String valor(int fila, int columna) { Object v=modelo.getValueAt(fila,columna); return v==null?null:v.toString(); }
    private int numero(int fila, int columna) { return ((Number) modelo.getValueAt(fila,columna)).intValue(); }
    private void actualizarAcciones() {
        boolean seleccion = tabla.getSelectedRow() >= 0;
        botonEditar.setEnabled(seleccion);
        botonCambiarEstado.setEnabled(seleccion);
    }

    static ResumenSoftware calcularResumen(List<SoftwareLaboratorio> filas) {
        int atencion = 0;
        for (SoftwareLaboratorio fila : filas) {
            if ("Desactualizado".equals(fila.estado())
                    || "Pendiente de instalación".equals(fila.estado())
                    || "Pendiente de eliminación".equals(fila.estado())) atencion++;
        }
        return new ResumenSoftware(filas.size(), atencion);
    }
    private void mostrarError(Exception ex) {
        LOGGER.log(Level.WARNING, "No se pudo completar una operación de software", ex);
        JOptionPane.showMessageDialog(ventana, mensajeUsuario(ex),
                "No se pudo completar la operación", JOptionPane.ERROR_MESSAGE);
    }

    static String mensajeUsuario(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (esTablaSoftwareAusente(causa)) return MENSAJE_ESQUEMA_DESACTUALIZADO;
            if (causa instanceof IllegalArgumentException
                    && causa.getMessage() != null && !causa.getMessage().isBlank()) {
                return causa.getMessage();
            }
        }
        return "No fue posible completar la operación de software. Inténtalo nuevamente.";
    }

    private static boolean esTablaSoftwareAusente(Throwable causa) {
        if (!(causa instanceof SQLException sql)) return false;
        String mensaje = sql.getMessage() == null ? "" : sql.getMessage().toLowerCase(java.util.Locale.ROOT);
        return (causa instanceof SQLSyntaxErrorException || sql.getErrorCode() == 1146)
                && mensaje.contains("software_laboratorio")
                && (mensaje.contains("doesn't exist") || mensaje.contains("no existe"));
    }
    private static String[] estadosConTodos() { List<String> e=new ArrayList<>();e.add("Todos");e.addAll(ServicioSoftwareLaboratorio.ESTADOS);return e.toArray(String[]::new); }

    private record Filtros(Integer laboratorio, String estado, String uso, String busqueda) { }
    private record Resultado(Filtros filtros, List<SoftwareLaboratorio> filas) { }
    record ResumenSoftware(int total, int requierenAtencion) { }

}
