package labsync.interfaz.inventario;

import com.toedter.calendar.JDateChooser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import labsync.modelo.SoftwareLaboratorio;
import labsync.servicio.ServicioSoftwareLaboratorio;

/** Formulario modal y programático para registrar o editar software. */
final class DialogoSoftwareLaboratorio extends JDialog {
    private static final Color VERDE = new Color(8, 173, 141);
    private static final Color VERDE_OSCURO = new Color(6, 140, 115);
    private static final Color FONDO = new Color(245, 247, 249);

    private final boolean edicion;
    private final JComboBox<ItemLaboratorio> laboratorio;
    private final JTextField nombre = new JTextField();
    private final JTextField versionInstalada = new JTextField();
    private final JTextField versionObjetivo = new JTextField();
    private final JComboBox<String> usoAcademico = new JComboBox<>(new String[]{
        "General", "TSU - DSM", "Ingeniería - ITI-ID", "Otro..."});
    private final JComboBox<String> estado;
    private final JDateChooser fechaRevision = new JDateChooser();
    private final JTextArea observaciones = new JTextArea(4, 30);
    private final JLabel ayudaEstado = new JLabel(" ");
    private Datos datos;

    DialogoSoftwareLaboratorio(Window propietario, List<ItemLaboratorio> laboratorios,
            SoftwareLaboratorio actual) {
        super(propietario, actual == null ? "Registrar software" : "Editar software",
                ModalityType.APPLICATION_MODAL);
        edicion = actual != null;
        laboratorio = new JComboBox<>(laboratorios.toArray(ItemLaboratorio[]::new));
        estado = new JComboBox<>(ReglasFormularioSoftware.estados(edicion).toArray(String[]::new));
        construirInterfaz();
        cargar(actual);
        actualizarEstado();
    }

    Datos mostrar() {
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return datos;
    }

    private void construirInterfaz() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(700, 500));
        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
        raiz.add(crearEncabezado(), BorderLayout.NORTH);
        raiz.add(crearFormulario(), BorderLayout.CENTER);
        raiz.add(crearAcciones(), BorderLayout.SOUTH);
        setContentPane(raiz);
        pack();
        setSize(new Dimension(750, 550));
        setResizable(true);
    }

    private JPanel crearEncabezado() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = base(0, 0);
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        JLabel titulo = new JLabel(edicion ? "Editar software" : "Registrar software");
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setForeground(new Color(48, 54, 60));
        panel.add(titulo, c);
        c.gridy = 1; c.insets = new Insets(4, 0, 0, 0);
        JLabel subtitulo = new JLabel(
                "Registra una aplicación instalada o requerida en un laboratorio.");
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitulo.setForeground(new Color(100, 107, 114));
        panel.add(subtitulo, c);
        return panel;
    }

    private JPanel crearFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 228, 231)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        int fila = 0;
        fila = seccion(panel, "IDENTIFICACIÓN", fila);
        agregarCampo(panel, "Laboratorio *", laboratorio, fila, 0);
        agregarCampo(panel, "Nombre del software *", nombre, fila++, 1);
        fila = seccion(panel, "VERSIONES Y USO", fila);
        agregarCampo(panel, "Versión instalada", versionInstalada, fila, 0);
        agregarCampo(panel, "Versión objetivo", versionObjetivo, fila++, 1);
        usoAcademico.setEditable(true);
        agregarCampoAncho(panel, "Uso académico", usoAcademico, fila++);
        fila = seccion(panel, "CONTROL", fila);
        agregarCampo(panel, "Estado", estado, fila, 0);
        fechaRevision.setDateFormatString("dd/MM/yyyy");
        agregarCampo(panel, "Última revisión", fechaRevision, fila++, 1);
        GridBagConstraints ayuda = base(0, fila++);
        ayuda.gridwidth = 2; ayuda.fill = GridBagConstraints.HORIZONTAL;
        ayudaEstado.setFont(new Font("Arial", Font.PLAIN, 11));
        ayudaEstado.setForeground(new Color(166, 92, 23));
        panel.add(ayudaEstado, ayuda);
        observaciones.setLineWrap(true);
        observaciones.setWrapStyleWord(true);
        JScrollPane scrollObservaciones = new JScrollPane(observaciones);
        scrollObservaciones.setPreferredSize(new Dimension(500, 78));
        scrollObservaciones.setMinimumSize(new Dimension(300, 68));
        agregarCampoAncho(panel, "Observaciones", scrollObservaciones, fila);
        estado.addActionListener(e -> actualizarEstado());
        return panel;
    }

    private JPanel crearAcciones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setOpaque(false);
        JButton cancelar = boton("Cancelar", Color.WHITE, VERDE_OSCURO);
        cancelar.addActionListener(e -> dispose());
        JButton guardar = boton(edicion ? "Guardar cambios" : "Registrar software",
                VERDE, Color.WHITE);
        guardar.setPreferredSize(new Dimension(edicion ? 150 : 175, 36));
        guardar.addActionListener(e -> aceptar());
        panel.add(cancelar);
        panel.add(guardar);
        return panel;
    }

    private void cargar(SoftwareLaboratorio actual) {
        usoAcademico.setSelectedItem("General");
        fechaRevision.setDate(Date.from(ReglasFormularioSoftware.fechaInicial().atStartOfDay(
                ZoneId.systemDefault()).toInstant()));
        if (actual == null) return;
        seleccionar(laboratorio, actual.idLaboratorio());
        nombre.setText(actual.nombre());
        versionInstalada.setText(texto(actual.versionInstalada()));
        versionObjetivo.setText(texto(actual.versionObjetivo()));
        usoAcademico.setSelectedItem(actual.usoAcademico());
        estado.setSelectedItem(actual.estado());
        if (actual.fechaRevision() != null) {
            fechaRevision.setDate(Date.from(actual.fechaRevision().atStartOfDay(
                    ZoneId.systemDefault()).toInstant()));
        } else {
            fechaRevision.setDate(null);
        }
        observaciones.setText(texto(actual.observaciones()));
    }

    private void actualizarEstado() {
        ReglasFormularioSoftware.Configuracion configuracion =
                ReglasFormularioSoftware.configuracion((String) estado.getSelectedItem());
        versionInstalada.setEnabled(configuracion.versionInstaladaHabilitada());
        if (!configuracion.versionInstaladaHabilitada()) versionInstalada.setText("");
        ayudaEstado.setText(configuracion.ayuda().isBlank() ? " " : configuracion.ayuda());
    }

    private void aceptar() {
        ItemLaboratorio lab = (ItemLaboratorio) laboratorio.getSelectedItem();
        String uso = String.valueOf(usoAcademico.getEditor().getItem());
        String error = ReglasFormularioSoftware.validar(lab == null ? 0 : lab.id(),
                nombre.getText(), versionInstalada.getText(), versionObjetivo.getText(),
                (String) estado.getSelectedItem(), observaciones.getText());
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Revisa la información",
                    JOptionPane.WARNING_MESSAGE);
            enfocar(error);
            return;
        }
        Date fecha = fechaRevision.getDate();
        LocalDate local = fecha == null ? null : fecha.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        datos = new Datos(lab.id(), nombre.getText(), versionInstalada.getText(),
                versionObjetivo.getText(), uso, (String) estado.getSelectedItem(),
                local, observaciones.getText());
        dispose();
    }

    private void enfocar(String error) {
        Component campo = error.startsWith("Selecciona") ? laboratorio
                : error.startsWith("Escribe") ? nombre
                : error.contains("versión instalada") ? versionInstalada
                : error.contains("versión objetivo") ? versionObjetivo : observaciones;
        campo.requestFocusInWindow();
    }

    private static int seccion(JPanel panel, String texto, int fila) {
        GridBagConstraints c = base(0, fila);
        c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(fila == 0 ? 0 : 10, 0, 5, 0);
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("Arial", Font.BOLD, 11));
        etiqueta.setForeground(VERDE_OSCURO);
        panel.add(etiqueta, c);
        return fila + 1;
    }

    private static void agregarCampo(JPanel panel, String texto, Component componente,
            int fila, int columna) {
        GridBagConstraints campo = base(columna, fila);
        campo.weightx = 1; campo.fill = GridBagConstraints.HORIZONTAL;
        campo.insets = new Insets(0, columna == 0 ? 0 : 8, 5, columna == 0 ? 8 : 0);
        componente.setPreferredSize(new Dimension(250, 30));
        panel.add(contenedorCampo(texto, componente), campo);
    }

    private static void agregarCampoAncho(JPanel panel, String texto, Component componente, int fila) {
        GridBagConstraints campo = base(0, fila);
        campo.gridwidth = 2; campo.weightx = 1; campo.fill = GridBagConstraints.BOTH;
        if (componente instanceof JScrollPane) campo.weighty = 1;
        campo.insets = new Insets(0, 0, 5, 0);
        panel.add(contenedorCampo(texto, componente), campo);
    }

    private static JPanel contenedorCampo(String texto, Component componente) {
        JPanel campo = new JPanel(new BorderLayout(0, 4));
        campo.setOpaque(false);
        campo.add(etiqueta(texto), BorderLayout.NORTH);
        campo.add(componente, BorderLayout.CENTER);
        return campo;
    }

    private static GridBagConstraints base(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x; c.gridy = y; c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("Arial", Font.BOLD, 11));
        etiqueta.setForeground(new Color(70, 76, 82));
        return etiqueta;
    }

    private static JButton boton(String texto, Color fondo, Color frente) {
        JButton boton = new JButton(texto);
        boton.setBackground(fondo); boton.setForeground(frente);
        boton.setFont(new Font("Arial", Font.BOLD, 12));
        boton.setFocusPainted(false); boton.setPreferredSize(new Dimension(120, 36));
        return boton;
    }

    private static void seleccionar(JComboBox<ItemLaboratorio> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id() == id) combo.setSelectedIndex(i);
        }
    }

    private static String texto(String valor) { return valor == null ? "" : valor; }

    record Datos(int idLaboratorio, String nombre, String versionInstalada,
            String versionObjetivo, String usoAcademico, String estado,
            LocalDate fechaRevision, String observaciones) { }
}

record ItemLaboratorio(int id, String nombre) {
    @Override public String toString() { return nombre; }
}

final class ReglasFormularioSoftware {
    private ReglasFormularioSoftware() { }

    static List<String> estados(boolean edicion) {
        List<String> estados = new ArrayList<>(ServicioSoftwareLaboratorio.ESTADOS);
        if (!edicion) estados.remove("Eliminado");
        return estados;
    }

    static Configuracion configuracion(String estado) {
        if ("Pendiente de instalación".equals(estado)) {
            return new Configuracion(false, false, true, false,
                    "Indica la versión que debe instalarse.");
        }
        if ("Desactualizado".equals(estado)) {
            return new Configuracion(true, true, true, false, " ");
        }
        if ("Actualizado".equals(estado)) {
            return new Configuracion(true, true, false, false, " ");
        }
        if ("Pendiente de eliminación".equals(estado)) {
            return new Configuracion(true, false, false, true,
                    "Indica por qué este software debe retirarse.");
        }
        return new Configuracion(true, false, false, false, " ");
    }

    static String validar(int idLaboratorio, String nombre, String instalada,
            String objetivo, String estado, String observaciones) {
        if (idLaboratorio <= 0) return "Selecciona un laboratorio.";
        if (nombre == null || nombre.isBlank()) return "Escribe el nombre del software.";
        Configuracion c = configuracion(estado);
        if (c.versionInstaladaObligatoria() && vacio(instalada)) {
            return "Indica la versión instalada.";
        }
        if (c.versionObjetivoObligatoria() && vacio(objetivo)) {
            return "Pendiente de instalación".equals(estado)
                    ? "Indica la versión que debe instalarse."
                    : "Indica la versión objetivo para software desactualizado.";
        }
        if (c.observacionesObligatorias() && vacio(observaciones)) {
            return "Indica el motivo de la eliminación.";
        }
        return null;
    }

    private static boolean vacio(String valor) { return valor == null || valor.isBlank(); }

    static LocalDate fechaInicial() { return LocalDate.now(); }

    record Configuracion(boolean versionInstaladaHabilitada,
            boolean versionInstaladaObligatoria, boolean versionObjetivoObligatoria,
            boolean observacionesObligatorias, String ayuda) { }
}
