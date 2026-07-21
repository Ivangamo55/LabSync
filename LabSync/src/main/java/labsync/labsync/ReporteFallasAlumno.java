package labsync.labsync;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 * Diseño de la interfaz ReporteFallasAlumno.
 * JFrame Form editable desde NetBeans Swing Designer.
 * Mantiene la maquetación visual del proyecto LabSync.
 */
public class ReporteFallasAlumno extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ReporteFallasAlumno.class.getName());
    private String nombreUsuario;
    private static final String PH_EQUIPO = "Ingresa el código asignado por la escuela";
    private static final String PH_DESCRIPCION = "Describe que ocurre, cuando empezo y si impide usar el equipo.";

    public ReporteFallasAlumno() {
        this("Usuario");
    }

    public ReporteFallasAlumno(String nombreRecibido) {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        this.nombreUsuario = nombreRecibido == null || nombreRecibido.isBlank() ? "Usuario" : nombreRecibido;
        lbNombreUsuario.setText("Hola, " + nombreUsuario);
        configurarDateChooser();
        configurarFormulario();
        LaboratoriosBD.cargarDisponibles(cmbLaboratorio, "Selecciona laboratorio");
        cargarReportesRecientes();
        setLocationRelativeTo(null);
    }

    private void configurarDateChooser() {
        dateFechaFalla.setDateFormatString("yyyy-MM-dd");
        dateFechaFalla.setBackground(Color.WHITE);
        dateFechaFalla.setForeground(new Color(51, 51, 51));
        dateFechaFalla.setFont(new Font("Arial", Font.PLAIN, 12));
        dateFechaFalla.setBorder(BorderFactory.createLineBorder(new Color(102, 102, 102)));

        Component editor = dateFechaFalla.getDateEditor().getUiComponent();
        editor.setBackground(Color.WHITE);
        editor.setForeground(new Color(51, 51, 51));
        editor.setFont(new Font("Arial", Font.PLAIN, 12));

        if (editor instanceof javax.swing.JTextField) {
            javax.swing.JTextField campoTexto = (javax.swing.JTextField) editor;
            campoTexto.setBackground(Color.WHITE);
            campoTexto.setForeground(new Color(51, 51, 51));
            campoTexto.setCaretColor(new Color(51, 51, 51));
            campoTexto.setBorder(null);
        }

        dateFechaFalla.getCalendarButton().setBackground(Color.WHITE);
        dateFechaFalla.getCalendarButton().setForeground(new Color(51, 51, 51));
    }

    private void configurarFormulario() {
        dateFechaFalla.setMaxSelectableDate(java.sql.Date.valueOf(LocalDate.now()));
        ValidacionFechas.bloquearFinesDeSemana(dateFechaFalla);
        dateFechaFalla.setDate(ValidacionFechas.anteriorDiaHabil(new java.util.Date()));
        txtEquipo.setText("");
        txtEquipo.setToolTipText(PH_EQUIPO);
        txtDescripcion.setText("");
        txtDescripcion.setToolTipText(PH_DESCRIPCION);

        tablaReportes.setModel(new DefaultTableModel(
            new Object[][]{}, new String[]{"ID", "Fecha", "Laboratorio", "Equipo", "Estado"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        ocultarColumnaId();
        tablaReportes.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        btnReservas.addActionListener(evt -> abrirReservas());
        btnMisReservas.addActionListener(evt -> abrirMisReservas());
        btnReporteFallas.addActionListener(evt -> txtEquipo.requestFocusInWindow());
        btnCerrarSesion.addActionListener(evt -> cerrarSesion());
        btnLimpiar.addActionListener(evt -> limpiarFormulario());
        btnEnviarReporte.addActionListener(evt -> enviarReporte());
        btnVerDetalle.addActionListener(evt -> verDetalle());
    }

    private void cargarReportesRecientes() {
        DefaultTableModel modelo = (DefaultTableModel) tablaReportes.getModel();
        modelo.setRowCount(0);
        String sql = "SELECT id_falla, DATE(fecha_reporte) fecha, laboratorio, "
            + "COALESCE(NULLIF(codigo_equipo, ''), nombre_equipo, 'Sin especificar') equipo, estado "
            + "FROM reporte_fallas WHERE SUBSTRING_INDEX(TRIM(reportado_por), ' ', 1) = ? "
            + "ORDER BY fecha_reporte DESC LIMIT 5";
        try (Connection con = ConexionBD.conectar()) {
            if (con == null) {
                return;
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                asignarUsuario(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modelo.addRow(new Object[]{rs.getInt("id_falla"), rs.getDate("fecha"),
                            rs.getString("laboratorio"), rs.getString("equipo"), rs.getString("estado")});
                    }
                }
            }
        } catch (SQLException ex) {
            mostrarErrorSQL("No se pudieron cargar tus reportes", ex);
        }
    }

    private void ocultarColumnaId() {
        if (tablaReportes.getColumnModel().getColumnCount() > 0) {
            javax.swing.table.TableColumn columna = tablaReportes.getColumnModel().getColumn(0);
            columna.setMinWidth(0);
            columna.setMaxWidth(0);
            columna.setPreferredWidth(0);
        }
    }

    private boolean validarFormulario() {
        if (cmbLaboratorio.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un laboratorio.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (txtEquipo.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa el código del equipo asignado por la escuela.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (txtEquipo.getText().trim().length() > 50) {
            JOptionPane.showMessageDialog(this, "El código del equipo no puede exceder 50 caracteres.", "Código no válido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (dateFechaFalla.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Selecciona la fecha del reporte.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        LocalDate fecha = new java.sql.Date(dateFechaFalla.getDate().getTime()).toLocalDate();
        if (fecha.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "La fecha del reporte no puede estar en el futuro.", "Fecha no valida", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (!ValidacionFechas.validarDiaHabil(this, dateFechaFalla.getDate(), "registrar fallas")) return false;
        if (cmbTipoFalla.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Selecciona el tipo de falla.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (txtDescripcion.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Describe la falla.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void enviarReporte() {
        if (!validarFormulario()) {
            return;
        }
        Connection con = ConexionBD.conectar();
        if (con == null) {
            mostrarErrorConexion();
            return;
        }
        try (con) {
            DatosUsuario usuario = obtenerDatosUsuario(con);
            if (usuario == null) {
                throw new SQLException("No se encontro el usuario que inicio sesion.");
            }
            String laboratorio = cmbLaboratorio.getSelectedItem().toString();
            String codigoEquipo = txtEquipo.getText().trim();
            LocalDate fecha = new java.sql.Date(dateFechaFalla.getDate().getTime()).toLocalDate();
            LocalTime hora = fecha.equals(LocalDate.now()) ? LocalTime.now() : LocalTime.NOON;
            String descripcion = "[Tipo: " + cmbTipoFalla.getSelectedItem() + "] " + txtDescripcion.getText().trim();
            String sql = "INSERT INTO reporte_fallas (id_usuario, codigo_equipo, nombre_equipo, "
                + "laboratorio, reportado_por, rol_reportante, descripcion_falla, estado, fecha_reporte) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'Pendiente', ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, usuario.idUsuario);
                ps.setString(2, codigoEquipo);
                ps.setNull(3, java.sql.Types.VARCHAR);
                ps.setString(4, laboratorio);
                ps.setString(5, usuario.nombreCompleto);
                ps.setString(6, usuario.rol);
                ps.setString(7, descripcion);
                ps.setTimestamp(8, Timestamp.valueOf(fecha.atTime(hora)));
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "El reporte fue enviado y quedo Pendiente de revision.",
                "Reporte registrado", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarReportesRecientes();
        } catch (SQLException ex) {
            mostrarErrorSQL("No se pudo registrar el reporte", ex);
        }
    }

    private DatosUsuario obtenerDatosUsuario(Connection con) throws SQLException {
        String sql = "SELECT id, CONCAT_WS(' ', nombre, apellido_p, apellido_m) nombre_completo, rol "
            + "FROM usuario WHERE nombre = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new DatosUsuario(rs.getInt("id"), rs.getString("nombre_completo"), rs.getString("rol"));
            }
        }
        return null;
    }

    private void limpiarFormulario() {
        cmbLaboratorio.setSelectedIndex(0);
        txtEquipo.setText("");
        dateFechaFalla.setDate(ValidacionFechas.anteriorDiaHabil(new java.util.Date()));
        cmbTipoFalla.setSelectedIndex(0);
        txtDescripcion.setText("");
    }

    private void verDetalle() {
        int fila = tablaReportes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un reporte de la tabla.", "Seleccion requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modeloFila = tablaReportes.convertRowIndexToModel(fila);
        int idFalla = ((Number) tablaReportes.getModel().getValueAt(modeloFila, 0)).intValue();
        String sql = "SELECT fecha_reporte, laboratorio, "
            + "COALESCE(NULLIF(codigo_equipo, ''), nombre_equipo, 'Sin especificar') equipo, "
            + "descripcion_falla, prioridad, estado, "
            + "COALESCE(observaciones, 'Sin observaciones') observaciones FROM reporte_fallas WHERE id_falla = ? "
            + "AND SUBSTRING_INDEX(TRIM(reportado_por), ' ', 1) = ?";
        try (Connection con = ConexionBD.conectar()) {
            if (con == null) { mostrarErrorConexion(); return; }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idFalla);
                ps.setString(2, nombreUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String detalle = "Fecha: " + rs.getTimestamp("fecha_reporte") + "\nLaboratorio: " + rs.getString("laboratorio")
                            + "\nCódigo del equipo: " + rs.getString("equipo")
                            + "\nPrioridad: " + rs.getString("prioridad") + "\nEstado: " + rs.getString("estado")
                            + "\n\nDescripcion:\n" + rs.getString("descripcion_falla") + "\n\nObservaciones:\n" + rs.getString("observaciones");
                        javax.swing.JTextArea area = new javax.swing.JTextArea(detalle, 14, 42);
                        area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true); area.setCaretPosition(0);
                        JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(area), "Detalle del reporte", JOptionPane.PLAIN_MESSAGE);
                    }
                }
            }
        } catch (SQLException ex) {
            mostrarErrorSQL("No se pudo consultar el detalle", ex);
        }
    }

    private void abrirReservas() {
        new ReservasAlumno(nombreUsuario).setVisible(true);
        dispose();
    }

    private void abrirMisReservas() {
        DashboardAlumno dashboard = new DashboardAlumno(nombreUsuario);
        dashboard.setVisible(true);
        dispose();
    }

    private void cerrarSesion() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Deseas cerrar sesion?", "Cerrar sesion", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            new Login().setVisible(true);
            dispose();
        }
    }

    private void asignarUsuario(PreparedStatement ps) throws SQLException {
        ps.setString(1, nombreUsuario);
    }

    private void mostrarErrorConexion() {
        JOptionPane.showMessageDialog(this, "No fue posible conectarse con la base de datos.", "Error de conexion", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarErrorSQL(String mensaje, SQLException ex) {
        logger.log(java.util.logging.Level.SEVERE, mensaje, ex);
        JOptionPane.showMessageDialog(this, mensaje + ":\n" + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
    }

    private static class DatosUsuario {
        final int idUsuario; final String nombreCompleto; final String rol;
        DatosUsuario(int idUsuario, String nombreCompleto, String rol) {
            this.idUsuario = idUsuario; this.nombreCompleto = nombreCompleto; this.rol = rol;
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sidebarVerde = new javax.swing.JPanel();
        imgLabSync = new javax.swing.JLabel();
        btnReservas = new javax.swing.JButton();
        btnMisReservas = new javax.swing.JButton();
        btnReporteFallas = new javax.swing.JButton();
        headerBlanco = new javax.swing.JPanel();
        imgUTJ = new javax.swing.JLabel();
        lbNombreUsuario = new javax.swing.JLabel();
        btnCerrarSesion = new javax.swing.JButton();
        panelContenedor = new javax.swing.JPanel();
        lbTituloPantalla = new javax.swing.JLabel();
        lbSubtituloPantalla = new javax.swing.JLabel();
        panelFormulario = new javax.swing.JPanel();
        lbTituloFormulario = new javax.swing.JLabel();
        lbLaboratorio = new javax.swing.JLabel();
        cmbLaboratorio = new javax.swing.JComboBox();
        lbEquipo = new javax.swing.JLabel();
        txtEquipo = new javax.swing.JTextField();
        lbFechaFalla = new javax.swing.JLabel();
        dateFechaFalla = new com.toedter.calendar.JDateChooser();
        lbTipoFalla = new javax.swing.JLabel();
        cmbTipoFalla = new javax.swing.JComboBox();
        lbDescripcion = new javax.swing.JLabel();
        scrollDescripcion = new javax.swing.JScrollPane();
        txtDescripcion = new javax.swing.JTextArea();
        btnLimpiar = new javax.swing.JButton();
        btnEnviarReporte = new javax.swing.JButton();
        panelGuia = new javax.swing.JPanel();
        lbTituloGuia = new javax.swing.JLabel();
        lbGuia1 = new javax.swing.JLabel();
        lbGuia2 = new javax.swing.JLabel();
        lbGuia3 = new javax.swing.JLabel();
        panelReportes = new javax.swing.JPanel();
        lbTituloReportes = new javax.swing.JLabel();
        jScrollPaneTabla = new javax.swing.JScrollPane();
        tablaReportes = new javax.swing.JTable();
        btnVerDetalle = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Reporte de Fallas Alumno");
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        sidebarVerde.setBackground(new java.awt.Color(8, 173, 141));
        sidebarVerde.setForeground(new java.awt.Color(255, 255, 255));
        sidebarVerde.setPreferredSize(new java.awt.Dimension(250, 720));
        sidebarVerde.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        imgLabSync.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/labsync_blanco_200.png"))); // NOI18N
        imgLabSync.setText("jLabel1");
        sidebarVerde.add(imgLabSync, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 24, 197, -1));

        btnReservas.setBackground(new java.awt.Color(255, 255, 255));
        btnReservas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnReservas.setForeground(new java.awt.Color(6, 140, 115));
        btnReservas.setText("Reservar");
        btnReservas.setBorderPainted(false);
        btnReservas.setFocusPainted(false);
        btnReservas.setPreferredSize(new java.awt.Dimension(200, 50));
        sidebarVerde.add(btnReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 340, 200, -1));

        btnMisReservas.setBackground(new java.awt.Color(255, 255, 255));
        btnMisReservas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnMisReservas.setForeground(new java.awt.Color(6, 140, 115));
        btnMisReservas.setText("Mis reservas");
        btnMisReservas.setBorderPainted(false);
        btnMisReservas.setFocusPainted(false);
        btnMisReservas.setPreferredSize(new java.awt.Dimension(200, 50));
        sidebarVerde.add(btnMisReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 270, 200, -1));

        btnReporteFallas.setBackground(new java.awt.Color(255, 255, 255));
        btnReporteFallas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnReporteFallas.setForeground(new java.awt.Color(6, 140, 115));
        btnReporteFallas.setText("Reporte de fallas");
        btnReporteFallas.setBorderPainted(false);
        btnReporteFallas.setFocusPainted(false);
        btnReporteFallas.setPreferredSize(new java.awt.Dimension(200, 50));
        sidebarVerde.add(btnReporteFallas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 410, 200, -1));

        getContentPane().add(sidebarVerde, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 250, 731));

        headerBlanco.setBackground(new java.awt.Color(255, 255, 255));
        headerBlanco.setPreferredSize(new java.awt.Dimension(1030, 100));
        headerBlanco.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        imgUTJ.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/UTJ_color.png"))); // NOI18N
        headerBlanco.add(imgUTJ, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 25, -1, -1));

        lbNombreUsuario.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbNombreUsuario.setForeground(new java.awt.Color(8, 173, 141));
        lbNombreUsuario.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lbNombreUsuario.setText("Hola, Usuario");
        headerBlanco.add(lbNombreUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(650, 40, 200, 36));

        btnCerrarSesion.setBackground(new java.awt.Color(220, 53, 69));
        btnCerrarSesion.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCerrarSesion.setForeground(new java.awt.Color(255, 255, 255));
        btnCerrarSesion.setText("Cerrar Sesión");
        btnCerrarSesion.setBorderPainted(false);
        btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setPreferredSize(new java.awt.Dimension(130, 36));
        headerBlanco.add(btnCerrarSesion, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 40, -1, -1));

        getContentPane().add(headerBlanco, new org.netbeans.lib.awtextra.AbsoluteConstraints(256, 0, 1030, 100));

        panelContenedor.setBackground(new java.awt.Color(245, 245, 245));
        panelContenedor.setPreferredSize(new java.awt.Dimension(1030, 625));
        panelContenedor.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloPantalla.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTituloPantalla.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloPantalla.setText("Reporte de fallas");
        panelContenedor.add(lbTituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 25, -1, -1));

        lbSubtituloPantalla.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        lbSubtituloPantalla.setForeground(new java.awt.Color(102, 102, 102));
        lbSubtituloPantalla.setText("Registra una incidencia de laboratorio para que pueda ser revisada.");
        panelContenedor.add(lbSubtituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 58, -1, -1));

        panelFormulario.setBackground(new java.awt.Color(255, 255, 255));
        panelFormulario.setPreferredSize(new java.awt.Dimension(640, 500));
        panelFormulario.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloFormulario.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbTituloFormulario.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloFormulario.setText("Nuevo reporte");
        panelFormulario.add(lbTituloFormulario, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, -1, -1));

        lbLaboratorio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        lbLaboratorio.setText("Laboratorio");
        panelFormulario.add(lbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 65, -1, -1));

        cmbLaboratorio.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        cmbLaboratorio.setModel(new javax.swing.DefaultComboBoxModel(
            new String[] { "Selecciona laboratorio" }));
        panelFormulario.add(cmbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 90, 270, 30));

        lbEquipo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbEquipo.setForeground(new java.awt.Color(102, 102, 102));
        lbEquipo.setText("Código del equipo");
        panelFormulario.add(lbEquipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(325, 65, -1, -1));

        txtEquipo.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtEquipo.setForeground(new java.awt.Color(51, 51, 51));
        txtEquipo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        panelFormulario.add(txtEquipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(325, 90, 270, 30));

        lbFechaFalla.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbFechaFalla.setForeground(new java.awt.Color(102, 102, 102));
        lbFechaFalla.setText("Fecha del reporte");
        panelFormulario.add(lbFechaFalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 130, -1, -1));

        dateFechaFalla.setBackground(new java.awt.Color(255, 255, 255));
        dateFechaFalla.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        dateFechaFalla.setForeground(new java.awt.Color(51, 51, 51));
        dateFechaFalla.setDateFormatString("yyyy-MM-dd");
        dateFechaFalla.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        panelFormulario.add(dateFechaFalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 155, 270, 30));

        lbTipoFalla.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbTipoFalla.setForeground(new java.awt.Color(102, 102, 102));
        lbTipoFalla.setText("Tipo de falla");
        panelFormulario.add(lbTipoFalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(325, 130, -1, -1));

        cmbTipoFalla.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        cmbTipoFalla.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Selecciona tipo", "Hardware", "Software", "Red / Internet", "Mobiliario", "Otro" }));
        panelFormulario.add(cmbTipoFalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(325, 155, 270, 30));

        lbDescripcion.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDescripcion.setForeground(new java.awt.Color(102, 102, 102));
        lbDescripcion.setText("Descripción de la falla");
        panelFormulario.add(lbDescripcion, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 220, -1, -1));

        txtDescripcion.setColumns(20);
        txtDescripcion.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setRows(5);
        txtDescripcion.setText("Describe qué ocurre, cuándo empezó y si impide usar el equipo.");
        txtDescripcion.setWrapStyleWord(true);
        scrollDescripcion.setViewportView(txtDescripcion);

        panelFormulario.add(scrollDescripcion, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 255, 570, 160));

        btnLimpiar.setBackground(new java.awt.Color(108, 117, 125));
        btnLimpiar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnLimpiar.setForeground(new java.awt.Color(255, 255, 255));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.setFocusPainted(false);
        panelFormulario.add(btnLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 445, 120, 35));

        btnEnviarReporte.setBackground(new java.awt.Color(8, 173, 141));
        btnEnviarReporte.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnEnviarReporte.setForeground(new java.awt.Color(255, 255, 255));
        btnEnviarReporte.setText("Enviar reporte");
        btnEnviarReporte.setBorderPainted(false);
        btnEnviarReporte.setFocusPainted(false);
        panelFormulario.add(btnEnviarReporte, new org.netbeans.lib.awtextra.AbsoluteConstraints(455, 445, 140, 35));

        panelContenedor.add(panelFormulario, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 95, 640, 500));

        panelGuia.setBackground(new java.awt.Color(255, 255, 255));
        panelGuia.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelGuia.setPreferredSize(new java.awt.Dimension(290, 205));
        panelGuia.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloGuia.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbTituloGuia.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloGuia.setText("Guía rápida");
        panelGuia.add(lbTituloGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, -1, -1));

        lbGuia1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbGuia1.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia1.setText("<html>1. Selecciona laboratorio, código del equipo y fecha del reporte.</html>");
        panelGuia.add(lbGuia1, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 55, 240, 42));

        lbGuia2.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbGuia2.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia2.setText("<html>2. Elige el tipo y describe qué ocurrió y si impide utilizarlo.</html>");
        panelGuia.add(lbGuia2, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 100, 240, 42));

        lbGuia3.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbGuia3.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia3.setText("<html>3. Al enviar, tu usuario, rol y el estado Pendiente se registran automáticamente.</html>");
        panelGuia.add(lbGuia3, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 145, 240, 48));

        panelContenedor.add(panelGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(705, 95, 290, 205));

        panelReportes.setBackground(new java.awt.Color(255, 255, 255));
        panelReportes.setPreferredSize(new java.awt.Dimension(290, 285));
        panelReportes.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloReportes.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbTituloReportes.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloReportes.setText("Mis reportes recientes");
        panelReportes.add(lbTituloReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, -1, -1));

        tablaReportes.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        tablaReportes.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Fecha", "Laboratorio", "Equipo", "Estado"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaReportes.setRowHeight(30);
        tablaReportes.setSelectionBackground(new java.awt.Color(224, 247, 241));
        tablaReportes.setShowVerticalLines(false);
        jScrollPaneTabla.setViewportView(tablaReportes);

        panelReportes.add(jScrollPaneTabla, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, 250, 155));

        btnVerDetalle.setBackground(new java.awt.Color(8, 173, 141));
        btnVerDetalle.setFont(new java.awt.Font("Arial", 1, 13)); // NOI18N
        btnVerDetalle.setForeground(new java.awt.Color(255, 255, 255));
        btnVerDetalle.setText("Ver detalle");
        btnVerDetalle.setBorderPainted(false);
        btnVerDetalle.setFocusPainted(false);
        panelReportes.add(btnVerDetalle, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 230, 250, 32));

        panelContenedor.add(panelReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(705, 325, 290, 270));

        getContentPane().add(panelContenedor, new org.netbeans.lib.awtextra.AbsoluteConstraints(256, 106, 1030, 625));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> {
            new ReporteFallasAlumno().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnEnviarReporte;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnMisReservas;
    private javax.swing.JButton btnReporteFallas;
    private javax.swing.JButton btnReservas;
    private javax.swing.JButton btnVerDetalle;
    private javax.swing.JComboBox cmbLaboratorio;
    private javax.swing.JComboBox cmbTipoFalla;
    private com.toedter.calendar.JDateChooser dateFechaFalla;
    private javax.swing.JPanel headerBlanco;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JLabel imgUTJ;
    private javax.swing.JScrollPane jScrollPaneTabla;
    private javax.swing.JLabel lbDescripcion;
    private javax.swing.JLabel lbEquipo;
    private javax.swing.JLabel lbFechaFalla;
    private javax.swing.JLabel lbGuia1;
    private javax.swing.JLabel lbGuia2;
    private javax.swing.JLabel lbGuia3;
    private javax.swing.JLabel lbLaboratorio;
    private javax.swing.JLabel lbNombreUsuario;
    private javax.swing.JLabel lbSubtituloPantalla;
    private javax.swing.JLabel lbTipoFalla;
    private javax.swing.JLabel lbTituloFormulario;
    private javax.swing.JLabel lbTituloGuia;
    private javax.swing.JLabel lbTituloPantalla;
    private javax.swing.JLabel lbTituloReportes;
    private javax.swing.JPanel panelContenedor;
    private javax.swing.JPanel panelFormulario;
    private javax.swing.JPanel panelGuia;
    private javax.swing.JPanel panelReportes;
    private javax.swing.JScrollPane scrollDescripcion;
    private javax.swing.JPanel sidebarVerde;
    private javax.swing.JTable tablaReportes;
    private javax.swing.JTextArea txtDescripcion;
    private javax.swing.JTextField txtEquipo;
    // End of variables declaration//GEN-END:variables
}
