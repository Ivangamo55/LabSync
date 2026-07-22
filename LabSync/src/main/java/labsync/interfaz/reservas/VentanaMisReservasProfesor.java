package labsync.interfaz.reservas;

import labsync.interfaz.comun.ActualizacionAutomatica;
import labsync.aplicacion.AplicacionLabSync;
import labsync.configuracion.ConexionBaseDatos;
import labsync.persistencia.ConsultaTabla;
import labsync.interfaz.comun.Recursos;
import labsync.modelo.SesionUsuario;
import labsync.interfaz.bitacora.VentanaBitacoraProfesor;
import labsync.interfaz.reservas.VentanaGestionReservas;
import labsync.interfaz.autenticacion.VentanaInicioSesion;
import labsync.interfaz.panel.VentanaPanelProfesor;
import labsync.interfaz.fallas.VentanaReporteFallaProfesor;
import labsync.interfaz.reservas.VentanaReservasProfesor;

/**
 * Interfaz del usuario Profesor para AplicacionLabSync.
 * Archivo JFrame Form compatible con el diseñador visual de NetBeans.
 * La sección initComponents se mantiene sincronizada con VentanaMisReservasProfesor.form.
 */
public class VentanaMisReservasProfesor extends javax.swing.JFrame {

    private static final String PLACEHOLDER_BUSQUEDA =
            "Buscar por fecha, horario, laboratorio, carrera, grupo o actividad";
    private static final java.awt.Color COLOR_PLACEHOLDER = new java.awt.Color(140, 140, 140);
    private static final java.awt.Color COLOR_TEXTO = new java.awt.Color(51, 51, 51);

    private String nombreUsuario;
    private SesionUsuario sesion;

    public VentanaMisReservasProfesor() {
        this("Profesor");
    }

    public VentanaMisReservasProfesor(String nombreRecibido) {
        this(SesionUsuario.buscarProfesor(nombreRecibido));
    }

    public VentanaMisReservasProfesor(SesionUsuario sesionRecibida) {
        initComponents();
        sesion = sesionRecibida == null ? SesionUsuario.buscarProfesor("Profesor") : sesionRecibida;
        nombreUsuario = sesion.getNombre();
        lbNombreUsuario.setText("Hola, " + nombreUsuario);
        java.net.URL icono = getClass().getResource("/images/logo_labsync_no_background.png");
        if (icono != null) setIconImage(new javax.swing.ImageIcon(icono).getImage());
        configurarNavegacion();
        configurarPantalla();
        labsync.interfaz.comun.NotificacionesGlobales.profesor(this, header, sesion);
        iniciarActualizacionAutomatica();
        setLocationRelativeTo(null);
    }

    private void iniciarActualizacionAutomatica() {
        new ActualizacionAutomatica<>(this, 7_000, () -> ConsultaTabla.ejecutar(
                "SELECT id_reserva, DATE_FORMAT(fecha, '%d/%m/%Y') fecha, horario, laboratorio, SUBSTRING_INDEX(carrera, ' - ', 1) carrera, CONCAT(grado, grupo) grupo, actividad, estado FROM reservas WHERE (id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ?)) AND rol_solicitante = 'Profesor' ORDER BY fecha DESC, horario",
                new String[]{"ID", "Fecha", "Horario", "Laboratorio", "Carrera", "Grupo", "Actividad", "Estado"},
                new String[]{"id_reserva", "fecha", "horario", "laboratorio", "carrera", "grupo", "actividad", "estado"},
                ps -> { ps.setInt(1, sesion.getId()); ps.setString(2, sesion.getNombreCompleto()); }),
                modelo -> { tablaReservas.setModel(modelo); aplicarFiltro(); });
    }

    private void configurarNavegacion() {
        btnInicio.addActionListener(e -> abrirVentana(new VentanaPanelProfesor(sesion)));
        btnReservar.addActionListener(e -> abrirVentana(new VentanaReservasProfesor(sesion)));
        btnMisReservas.addActionListener(e -> abrirVentana(new VentanaMisReservasProfesor(sesion)));
        btnBitacora.addActionListener(e -> abrirVentana(new VentanaBitacoraProfesor(sesion)));
        btnReporteFallas.addActionListener(e -> abrirVentana(new VentanaReporteFallaProfesor(sesion)));
        btnCerrarSesion.addActionListener(e -> cerrarSesion());
    }

    private void abrirVentana(javax.swing.JFrame ventana) {
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
        dispose();
    }

    private void cerrarSesion() {
        int opcion = javax.swing.JOptionPane.showConfirmDialog(
                this, "¿Deseas cerrar sesión?", "Cerrar sesión",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
        if (opcion == javax.swing.JOptionPane.YES_OPTION) {
            abrirVentana(new VentanaInicioSesion());
        }
    }


    private void ocultarPrimeraColumna(javax.swing.JTable tabla) {
        if (tabla.getColumnModel().getColumnCount() > 0) {
            tabla.getColumnModel().getColumn(0).setMinWidth(0);
            tabla.getColumnModel().getColumn(0).setMaxWidth(0);
            tabla.getColumnModel().getColumn(0).setWidth(0);
        }
    }

    private void cargarLaboratorios(javax.swing.JComboBox<String> combo, boolean incluirTodos) {
        combo.removeAllItems();
        combo.addItem(incluirTodos ? "Todos" : "Seleccionar");
        String sql = "SELECT nombre FROM laboratorios WHERE estado = 'Disponible' ORDER BY nombre";
        try (java.sql.Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) return;
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql);
                 java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) combo.addItem(rs.getString("nombre"));
            }
        } catch (java.sql.SQLException ex) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "No se cargaron los laboratorios", ex);
        }
    }

    private void configurarPantalla() {
        configurarPlaceholderBusqueda();
        tablaReservas.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, new String[]{"ID", "Fecha", "Horario", "Laboratorio", "Carrera", "Grupo", "Actividad", "Estado"}) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        tablaReservas.setRowHeight(32);
        ocultarPrimeraColumna(tablaReservas);
        cargarLaboratorios(cmbLaboratorio, true);
        cargarReservas();
        btnFiltrar.addActionListener(e -> aplicarFiltro());
        btnVerDetalle.addActionListener(e -> verDetalle());
        btnCancelar.addActionListener(e -> cancelarReserva());
        btnIrBitacora.addActionListener(e -> abrirBitacora());
    }

    private void configurarPlaceholderBusqueda() {
        mostrarPlaceholderBusqueda();
        txtBuscar.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (esPlaceholderBusqueda()) {
                    txtBuscar.setText("");
                    txtBuscar.setForeground(COLOR_TEXTO);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtBuscar.getText().trim().isEmpty()) {
                    mostrarPlaceholderBusqueda();
                }
            }
        });
    }

    private void mostrarPlaceholderBusqueda() {
        txtBuscar.setText(PLACEHOLDER_BUSQUEDA);
        txtBuscar.setForeground(COLOR_PLACEHOLDER);
    }

    private boolean esPlaceholderBusqueda() {
        return PLACEHOLDER_BUSQUEDA.equals(txtBuscar.getText())
                && COLOR_PLACEHOLDER.equals(txtBuscar.getForeground());
    }

    private void cargarReservas() {
        javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) tablaReservas.getModel();
        modelo.setRowCount(0);
        String sql = "SELECT id_reserva, DATE_FORMAT(fecha, '%d/%m/%Y') fecha, horario, laboratorio, "
                + "SUBSTRING_INDEX(carrera, ' - ', 1) carrera, CONCAT(grado, grupo) grupo, actividad, estado FROM reservas "
                + "WHERE (id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ?)) "
                + "AND rol_solicitante = 'Profesor' ORDER BY fecha DESC, horario";
        try (java.sql.Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) return;
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, sesion.getId());
                ps.setString(2, sesion.getNombreCompleto());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) modelo.addRow(new Object[]{rs.getInt("id_reserva"), rs.getString("fecha"), rs.getString("horario"), rs.getString("laboratorio"), rs.getString("carrera"), rs.getString("grupo"), rs.getString("actividad"), rs.getString("estado")});
                }
            }
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudieron cargar las reservas:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarFiltro() {
        javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter = new javax.swing.table.TableRowSorter<>(tablaReservas.getModel());
        tablaReservas.setRowSorter(sorter);
        java.util.List<javax.swing.RowFilter<Object, Object>> filtros = new java.util.ArrayList<>();
        String texto = esPlaceholderBusqueda() ? "" : txtBuscar.getText().trim();
        if (!texto.isEmpty()) filtros.add(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(texto)));
        if (cmbEstado.getSelectedIndex() > 0) filtros.add(javax.swing.RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(cmbEstado.getSelectedItem().toString()) + "$", 7));
        if (cmbLaboratorio.getSelectedIndex() > 0) filtros.add(javax.swing.RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(cmbLaboratorio.getSelectedItem().toString()) + "$", 3));
        sorter.setRowFilter(filtros.isEmpty() ? null : javax.swing.RowFilter.andFilter(filtros));
    }

    private int idSeleccionado() {
        int fila = tablaReservas.getSelectedRow();
        if (fila < 0) { javax.swing.JOptionPane.showMessageDialog(this, "Selecciona una reservación.", "Sin selección", javax.swing.JOptionPane.WARNING_MESSAGE); return -1; }
        fila = tablaReservas.convertRowIndexToModel(fila);
        return ((Number) tablaReservas.getModel().getValueAt(fila, 0)).intValue();
    }

    private void verDetalle() {
        int id = idSeleccionado(); if (id < 0) return;
        String sql = "SELECT nombre_solicitante, rol_solicitante, laboratorio, actividad, carrera, grado, grupo, turno, DATE_FORMAT(fecha,'%d/%m/%Y') fecha, horario, cantidad_alumnos, estado, IFNULL(observaciones,'Sin observaciones') observaciones FROM reservas "
                + "WHERE id_reserva = ? AND (id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ?))";
        try (java.sql.Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) throw new java.sql.SQLException("No hay conexión con la base de datos.");
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setInt(2, sesion.getId());
                ps.setString(3, sesion.getNombreCompleto());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String detalle = "Solicitante: " + rs.getString("nombre_solicitante") + "\nRol: " + rs.getString("rol_solicitante") + "\nFecha: " + rs.getString("fecha") + "\nHorario: " + rs.getString("horario") + "\nLaboratorio: " + rs.getString("laboratorio") + "\nCarrera: " + rs.getString("carrera") + "\nActividad: " + rs.getString("actividad") + "\nGrado y grupo: " + rs.getString("grado") + rs.getString("grupo") + "\nTurno: " + rs.getString("turno") + "\nCantidad de alumnos: " + rs.getInt("cantidad_alumnos") + "\nEstado: " + rs.getString("estado") + "\nObservaciones: " + rs.getString("observaciones");
                        javax.swing.JOptionPane.showMessageDialog(this, detalle, "Detalle de la reservación", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        } catch (java.sql.SQLException ex) { javax.swing.JOptionPane.showMessageDialog(this, "No se pudo consultar el detalle:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE); }
    }

    private void cancelarReserva() {
        int id = idSeleccionado(); if (id < 0) return;
        int fila = tablaReservas.convertRowIndexToModel(tablaReservas.getSelectedRow());
        String estado = String.valueOf(tablaReservas.getModel().getValueAt(fila, 7));
        if (!("Pendiente".equals(estado) || "Aprobada".equals(estado))) { javax.swing.JOptionPane.showMessageDialog(this, "Solo se pueden cancelar reservas Pendientes o Aprobadas."); return; }
        if (javax.swing.JOptionPane.showConfirmDialog(this, "¿Deseas cancelar la reservación seleccionada?", "Confirmar", javax.swing.JOptionPane.YES_NO_OPTION) != javax.swing.JOptionPane.YES_OPTION) return;
        try (java.sql.Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) throw new java.sql.SQLException("No hay conexión con la base de datos.");
            String sql = "UPDATE reservas SET estado = 'Cancelada' WHERE id_reserva = ? "
                    + "AND (id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ?)) "
                    + "AND estado IN ('Pendiente','Aprobada') AND fecha >= CURDATE()";
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setInt(2, sesion.getId());
                ps.setString(3, sesion.getNombreCompleto());
                if (ps.executeUpdate() == 0) {
                    javax.swing.JOptionPane.showMessageDialog(this, "La reserva ya no puede cancelarse.", "Sin cambios", javax.swing.JOptionPane.WARNING_MESSAGE);
                }
                cargarReservas();
            }
        } catch (java.sql.SQLException ex) { javax.swing.JOptionPane.showMessageDialog(this, "No se pudo cancelar la reserva:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE); }
    }

    private void abrirBitacora() {
        int id = idSeleccionado(); if (id < 0) return;
        int fila = tablaReservas.convertRowIndexToModel(tablaReservas.getSelectedRow());
        if (!"Aprobada".equals(String.valueOf(tablaReservas.getModel().getValueAt(fila, 7)))) {
            javax.swing.JOptionPane.showMessageDialog(this, "Selecciona una reserva Aprobada para cargarla en Bitácora.", "Reserva no aprobada", javax.swing.JOptionPane.WARNING_MESSAGE); return;
        }
        VentanaBitacoraProfesor ventana = new VentanaBitacoraProfesor(sesion, id);
        ventana.setVisible(true); dispose();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sidebar = new javax.swing.JPanel();
        imgLabSync = new javax.swing.JLabel();
        btnInicio = new javax.swing.JButton();
        btnReservar = new javax.swing.JButton();
        btnMisReservas = new javax.swing.JButton();
        btnBitacora = new javax.swing.JButton();
        btnReporteFallas = new javax.swing.JButton();
        header = new javax.swing.JPanel();
        imgUTJ = new javax.swing.JLabel();
        lbNombreUsuario = new javax.swing.JLabel();
        btnCerrarSesion = new javax.swing.JButton();
        body = new javax.swing.JPanel();
        lbTituloPantalla = new javax.swing.JLabel();
        lbSubtituloPantalla = new javax.swing.JLabel();
        panelGuia = new javax.swing.JPanel();
        lbTituloGuia = new javax.swing.JLabel();
        lbGuia1 = new javax.swing.JLabel();
        lbGuia2 = new javax.swing.JLabel();
        lbGuia3 = new javax.swing.JLabel();
        panelFiltros = new javax.swing.JPanel();
        lbBuscar = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        lbFiltroEstado = new javax.swing.JLabel();
        cmbEstado = new javax.swing.JComboBox<String>();
        lbFiltroLaboratorio = new javax.swing.JLabel();
        cmbLaboratorio = new javax.swing.JComboBox<String>();
        btnFiltrar = new javax.swing.JButton();
        panelTabla = new javax.swing.JPanel();
        lbTituloTabla = new javax.swing.JLabel();
        scrollReservas = new javax.swing.JScrollPane();
        tablaReservas = new javax.swing.JTable();
        panelAcciones = new javax.swing.JPanel();
        btnVerDetalle = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnIrBitacora = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LabSync - Mis reservas");
        setResizable(false);

        sidebar.setBackground(new java.awt.Color(8, 173, 141));
        sidebar.setPreferredSize(new java.awt.Dimension(250, 720));
        sidebar.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        imgLabSync.setIcon(Recursos.icono("/images/labsync_blanco_200.png"));
        sidebar.add(imgLabSync, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 24, 197, 200));
        btnInicio.setBackground(new java.awt.Color(255, 255, 255));
        btnInicio.setFont(new java.awt.Font("Arial", 1, 14));
        btnInicio.setForeground(new java.awt.Color(6, 140, 115));
        btnInicio.setText("Inicio");
        btnInicio.setBorderPainted(false);
        btnInicio.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnInicio.setFocusPainted(false);
        sidebar.add(btnInicio, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 255, 210, 45));
        btnReservar.setBackground(new java.awt.Color(255, 255, 255));
        btnReservar.setFont(new java.awt.Font("Arial", 1, 14));
        btnReservar.setForeground(new java.awt.Color(6, 140, 115));
        btnReservar.setText("Reservar");
        btnReservar.setBorderPainted(false);
        btnReservar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnReservar.setFocusPainted(false);
        sidebar.add(btnReservar, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 315, 210, 45));
        btnMisReservas.setBackground(new java.awt.Color(255, 255, 255));
        btnMisReservas.setFont(new java.awt.Font("Arial", 1, 14));
        btnMisReservas.setForeground(new java.awt.Color(6, 140, 115));
        btnMisReservas.setText("Mis reservas");
        btnMisReservas.setBorderPainted(false);
        btnMisReservas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnMisReservas.setFocusPainted(false);
        sidebar.add(btnMisReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 375, 210, 45));
        btnBitacora.setBackground(new java.awt.Color(255, 255, 255));
        btnBitacora.setFont(new java.awt.Font("Arial", 1, 14));
        btnBitacora.setForeground(new java.awt.Color(6, 140, 115));
        btnBitacora.setText("Bitácora");
        btnBitacora.setBorderPainted(false);
        btnBitacora.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBitacora.setFocusPainted(false);
        sidebar.add(btnBitacora, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 435, 210, 45));
        btnReporteFallas.setBackground(new java.awt.Color(255, 255, 255));
        btnReporteFallas.setFont(new java.awt.Font("Arial", 1, 14));
        btnReporteFallas.setForeground(new java.awt.Color(6, 140, 115));
        btnReporteFallas.setText("Reportar falla");
        btnReporteFallas.setBorderPainted(false);
        btnReporteFallas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnReporteFallas.setFocusPainted(false);
        sidebar.add(btnReporteFallas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 495, 210, 45));

        header.setBackground(new java.awt.Color(255, 255, 255));
        header.setPreferredSize(new java.awt.Dimension(1030, 100));
        header.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        imgUTJ.setIcon(Recursos.icono("/images/UTJ_color.png"));
        header.add(imgUTJ, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 25, 366, 50));
        lbNombreUsuario.setFont(new java.awt.Font("Arial", 1, 16));
        lbNombreUsuario.setForeground(new java.awt.Color(8, 173, 141));
        lbNombreUsuario.setText("Hola, Profesor");
        lbNombreUsuario.setHorizontalAlignment(4);
        header.add(lbNombreUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(675, 42, 170, 30));
        btnCerrarSesion.setBackground(new java.awt.Color(220, 53, 69));
        btnCerrarSesion.setFont(new java.awt.Font("Arial", 1, 14));
        btnCerrarSesion.setForeground(new java.awt.Color(255, 255, 255));
        btnCerrarSesion.setText("Cerrar Sesión");
        btnCerrarSesion.setBorderPainted(false);
        btnCerrarSesion.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCerrarSesion.setFocusPainted(false);
        header.add(btnCerrarSesion, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 35, 130, 42));

        body.setBackground(new java.awt.Color(245, 245, 245));
        body.setPreferredSize(new java.awt.Dimension(1030, 625));
        body.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloPantalla.setFont(new java.awt.Font("Arial", 1, 24));
        lbTituloPantalla.setForeground(new java.awt.Color(70, 70, 70));
        lbTituloPantalla.setText("Mis reservas");
        body.add(lbTituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 15, 420, 30));
        lbSubtituloPantalla.setFont(new java.awt.Font("Arial", 0, 13));
        lbSubtituloPantalla.setForeground(new java.awt.Color(100, 100, 100));
        lbSubtituloPantalla.setText("Consulta, filtra y administra las solicitudes registradas a tu nombre.");
        body.add(lbSubtituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 45, 800, 22));
        panelGuia.setBackground(new java.awt.Color(255, 255, 255));
        panelGuia.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelGuia.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloGuia.setFont(new java.awt.Font("Arial", 1, 16));
        lbTituloGuia.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloGuia.setText("Guía rápida");
        panelGuia.add(lbTituloGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 12, 130, 24));
        lbGuia1.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia1.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia1.setText("<html>1. Usa los filtros para localizar una reserva.</html>");
        panelGuia.add(lbGuia1, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 12, 230, 55));
        lbGuia2.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia2.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia2.setText("<html>2. Selecciona una fila para ver detalles o cancelar.</html>");
        panelGuia.add(lbGuia2, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 12, 235, 55));
        lbGuia3.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia3.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia3.setText("<html>3. En una reserva aprobada, abre Bitácora para registrar el uso.</html>");
        panelGuia.add(lbGuia3, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 12, 270, 55));
        body.add(panelGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 80, 960, 90));
        panelFiltros.setBackground(new java.awt.Color(255, 255, 255));
        panelFiltros.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelFiltros.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbBuscar.setFont(new java.awt.Font("Arial", 1, 12));
        lbBuscar.setForeground(new java.awt.Color(90, 90, 90));
        lbBuscar.setText("Buscar");
        panelFiltros.add(lbBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, 100, 18));
        txtBuscar.setBackground(new java.awt.Color(255, 255, 255));
        txtBuscar.setFont(new java.awt.Font("Arial", 0, 12));
        txtBuscar.setForeground(new java.awt.Color(140, 140, 140));
        txtBuscar.setText("Buscar por fecha, horario, laboratorio, carrera, grupo o actividad");
        txtBuscar.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFiltros.add(txtBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 31, 360, 30));
        lbFiltroEstado.setFont(new java.awt.Font("Arial", 1, 12));
        lbFiltroEstado.setForeground(new java.awt.Color(90, 90, 90));
        lbFiltroEstado.setText("Estado");
        panelFiltros.add(lbFiltroEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 10, 100, 18));
        cmbEstado.setFont(new java.awt.Font("Arial", 0, 12));
        cmbEstado.setForeground(new java.awt.Color(51, 51, 51));
        cmbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Todos", "Pendiente", "Aprobada", "Rechazada", "Cancelada", "Finalizada"}));
        cmbEstado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFiltros.add(cmbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 31, 170, 30));
        lbFiltroLaboratorio.setFont(new java.awt.Font("Arial", 1, 12));
        lbFiltroLaboratorio.setForeground(new java.awt.Color(90, 90, 90));
        lbFiltroLaboratorio.setText("Laboratorio");
        panelFiltros.add(lbFiltroLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 10, 120, 18));
        cmbLaboratorio.setFont(new java.awt.Font("Arial", 0, 12));
        cmbLaboratorio.setForeground(new java.awt.Color(51, 51, 51));
        cmbLaboratorio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Todos"}));
        cmbLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFiltros.add(cmbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 31, 180, 30));
        btnFiltrar.setBackground(new java.awt.Color(8, 173, 141));
        btnFiltrar.setFont(new java.awt.Font("Arial", 1, 14));
        btnFiltrar.setForeground(new java.awt.Color(255, 255, 255));
        btnFiltrar.setText("Filtrar");
        btnFiltrar.setBorderPainted(false);
        btnFiltrar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnFiltrar.setFocusPainted(false);
        panelFiltros.add(btnFiltrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(790, 29, 140, 34));
        body.add(panelFiltros, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 185, 960, 82));
        panelTabla.setBackground(new java.awt.Color(255, 255, 255));
        panelTabla.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelTabla.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloTabla.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloTabla.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloTabla.setText("Historial de reservaciones");
        panelTabla.add(lbTituloTabla, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 12, 300, 25));
        tablaReservas.setFont(new java.awt.Font("Arial", 0, 12));
        tablaReservas.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][] {
                        
                    },
                    new String[] {"ID", "Fecha", "Horario", "Laboratorio", "Carrera", "Grupo", "Actividad", "Estado"}
                ) {
                    boolean[] canEdit = new boolean[] {false, false, false, false, false, false, false, false};
                    @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return canEdit[columnIndex]; }
                });
        tablaReservas.setRowHeight(32);
        tablaReservas.setSelectionBackground(new java.awt.Color(224, 247, 241));
        tablaReservas.setShowVerticalLines(false);
        scrollReservas.setViewportView(tablaReservas);
        panelTabla.add(scrollReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 45, 910, 190));
        body.add(panelTabla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 282, 960, 255));
        panelAcciones.setBackground(new java.awt.Color(245, 245, 245));
        panelAcciones.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        btnVerDetalle.setBackground(new java.awt.Color(90, 90, 90));
        btnVerDetalle.setFont(new java.awt.Font("Arial", 1, 14));
        btnVerDetalle.setForeground(new java.awt.Color(255, 255, 255));
        btnVerDetalle.setText("Ver detalle");
        btnVerDetalle.setBorderPainted(false);
        btnVerDetalle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnVerDetalle.setFocusPainted(false);
        panelAcciones.add(btnVerDetalle, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 7, 140, 40));
        btnCancelar.setBackground(new java.awt.Color(220, 53, 69));
        btnCancelar.setFont(new java.awt.Font("Arial", 1, 14));
        btnCancelar.setForeground(new java.awt.Color(255, 255, 255));
        btnCancelar.setText("Cancelar reserva");
        btnCancelar.setBorderPainted(false);
        btnCancelar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCancelar.setFocusPainted(false);
        panelAcciones.add(btnCancelar, new org.netbeans.lib.awtextra.AbsoluteConstraints(165, 7, 160, 40));
        btnIrBitacora.setBackground(new java.awt.Color(8, 173, 141));
        btnIrBitacora.setFont(new java.awt.Font("Arial", 1, 14));
        btnIrBitacora.setForeground(new java.awt.Color(255, 255, 255));
        btnIrBitacora.setText("Ir a bitácora");
        btnIrBitacora.setBorderPainted(false);
        btnIrBitacora.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnIrBitacora.setFocusPainted(false);
        panelAcciones.add(btnIrBitacora, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 7, 165, 40));
        body.add(panelAcciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 550, 960, 55));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sidebar, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, 1030, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(body, javax.swing.GroupLayout.PREFERRED_SIZE, 1030, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sidebar, javax.swing.GroupLayout.PREFERRED_SIZE, 731, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(body, javax.swing.GroupLayout.PREFERRED_SIZE, 625, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VentanaMisReservasProfesor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new VentanaMisReservasProfesor("Profesor").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel sidebar;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JButton btnInicio;
    private javax.swing.JButton btnReservar;
    private javax.swing.JButton btnMisReservas;
    private javax.swing.JButton btnBitacora;
    private javax.swing.JButton btnReporteFallas;
    private javax.swing.JPanel header;
    private javax.swing.JLabel imgUTJ;
    private javax.swing.JLabel lbNombreUsuario;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JPanel body;
    private javax.swing.JLabel lbTituloPantalla;
    private javax.swing.JLabel lbSubtituloPantalla;
    private javax.swing.JPanel panelGuia;
    private javax.swing.JLabel lbTituloGuia;
    private javax.swing.JLabel lbGuia1;
    private javax.swing.JLabel lbGuia2;
    private javax.swing.JLabel lbGuia3;
    private javax.swing.JPanel panelFiltros;
    private javax.swing.JLabel lbBuscar;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JLabel lbFiltroEstado;
    private javax.swing.JComboBox<String> cmbEstado;
    private javax.swing.JLabel lbFiltroLaboratorio;
    private javax.swing.JComboBox<String> cmbLaboratorio;
    private javax.swing.JButton btnFiltrar;
    private javax.swing.JPanel panelTabla;
    private javax.swing.JLabel lbTituloTabla;
    private javax.swing.JScrollPane scrollReservas;
    private javax.swing.JTable tablaReservas;
    private javax.swing.JPanel panelAcciones;
    private javax.swing.JButton btnVerDetalle;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnIrBitacora;
    // End of variables declaration//GEN-END:variables
}
