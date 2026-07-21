package labsync.labsync;

/**
 * Interfaz del usuario Profesor para LabSync.
 * Archivo JFrame Form compatible con el diseñador visual de NetBeans.
 * La sección initComponents se mantiene sincronizada con BitacoraProfesor.form.
 */
public class BitacoraProfesor extends javax.swing.JFrame {

    private String nombreUsuario;
    private SesionUsuario sesion;
    private final java.util.List<ReservaBitacora> reservasDisponibles = new java.util.ArrayList<>();
    private boolean actualizandoSelector;

    public BitacoraProfesor() {
        this("Profesor", null);
    }

    public BitacoraProfesor(String nombreRecibido) {
        this(nombreRecibido, null);
    }

    public BitacoraProfesor(String nombreRecibido, Integer idReserva) {
        this(SesionUsuario.buscarProfesor(nombreRecibido), idReserva);
    }

    public BitacoraProfesor(SesionUsuario sesionRecibida) {
        this(sesionRecibida, null);
    }

    public BitacoraProfesor(SesionUsuario sesionRecibida, Integer idReserva) {
        initComponents();
        sesion = sesionRecibida == null ? SesionUsuario.buscarProfesor("Profesor") : sesionRecibida;
        nombreUsuario = sesion.getNombre();
        idReservaInicial = idReserva;
        lbNombreUsuario.setText("Hola, " + nombreUsuario);
        java.net.URL icono = getClass().getResource("/images/logo_labsync_no_background.png");
        if (icono != null) setIconImage(new javax.swing.ImageIcon(icono).getImage());
        configurarNavegacion();
        configurarPantalla();
        setLocationRelativeTo(null);
    }

    private void configurarNavegacion() {
        btnInicio.addActionListener(e -> abrirVentana(new DashboardProfesor(sesion)));
        btnReservar.addActionListener(e -> abrirVentana(new ReservasProfesor(sesion)));
        btnMisReservas.addActionListener(e -> abrirVentana(new MisReservasProfesor(sesion)));
        btnBitacora.addActionListener(e -> abrirVentana(new BitacoraProfesor(sesion)));
        btnReporteFallas.addActionListener(e -> abrirVentana(new ReporteFallasProfesor(sesion)));
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
            abrirVentana(new Login());
        }
    }


    private void ocultarPrimeraColumna(javax.swing.JTable tabla) {
        if (tabla.getColumnModel().getColumnCount() > 0) {
            tabla.getColumnModel().getColumn(0).setMinWidth(0);
            tabla.getColumnModel().getColumn(0).setMaxWidth(0);
            tabla.getColumnModel().getColumn(0).setWidth(0);
        }
    }

    private Integer idReservaInicial;

    private void configurarPantalla() {
        txtProfesor.setText(sesion.getNombreCompleto());
        txtRol.setText(sesion.getRol());
        configurarCamposAutomaticos();
        cmbReservas.addActionListener(e -> seleccionarReserva());
        btnLimpiar.addActionListener(e -> cmbReservas.setSelectedIndex(0));
        btnCargarReserva.addActionListener(e -> cargarReservasAprobadas(idReservaSeleccionada()));
        btnGuardar.addActionListener(e -> guardarBitacora());
        cargarReservasAprobadas(idReservaInicial);
    }

    private void configurarCamposAutomaticos() {
        dateFecha.setEnabled(false);
        cmbLaboratorio.setEnabled(false);
        txtHorario.setEditable(false);
        cmbTurno.setEnabled(false);
        txtCarrera.setEditable(false);
        cmbGrado.setEnabled(false);
        cmbGrupo.setEnabled(false);
        txtActividad.setEditable(false);
        txtTotalUsuarios.setEditable(false);
        btnGuardar.setEnabled(false);
    }

    private void seleccionarCombo(javax.swing.JComboBox<String> combo, String valor) {
        if (valor == null) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (valor.equals(combo.getItemAt(i))) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.addItem(valor);
        combo.setSelectedItem(valor);
    }

    private void cargarReservasAprobadas(Integer idReservaPreferida) {
        String sql = "SELECT id_reserva, laboratorio, actividad, carrera, grado, grupo, turno, fecha, horario, cantidad_alumnos FROM reservas "
                + "WHERE (id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ?)) "
                + "AND rol_solicitante = 'Profesor' AND estado = 'Aprobada' "
                + "AND fecha <= CURRENT_DATE ORDER BY fecha DESC, horario, id_reserva";
        actualizandoSelector = true;
        reservasDisponibles.clear();
        cmbReservas.removeAllItems();
        cmbReservas.addItem("Selecciona una reserva aprobada");
        try (java.sql.Connection con = ConexionBD.conectar()) {
            if (con == null) throw new java.sql.SQLException("No hay conexión con la base de datos.");
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, sesion.getId());
                ps.setString(2, sesion.getNombreCompleto());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ReservaBitacora reserva = new ReservaBitacora(rs.getInt("id_reserva"), rs.getDate("fecha"),
                                rs.getString("horario"), rs.getString("laboratorio"), rs.getString("actividad"),
                                rs.getString("carrera"), rs.getString("grado"), rs.getString("grupo"), rs.getString("turno"),
                                rs.getInt("cantidad_alumnos"));
                        reservasDisponibles.add(reserva);
                        cmbReservas.addItem(reserva.descripcion());
                    }
                }
            }
            seleccionarReservaPreferida(idReservaPreferida);
            if (reservasDisponibles.isEmpty()) mostrarSinReservas();
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudieron consultar las reservas:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE);
        } finally {
            actualizandoSelector = false;
        }
        seleccionarReserva();
    }

    private void seleccionarReservaPreferida(Integer idReservaPreferida) {
        if (idReservaPreferida == null) return;
        for (int i = 0; i < reservasDisponibles.size(); i++) {
            if (reservasDisponibles.get(i).id() == idReservaPreferida) {
                cmbReservas.setSelectedIndex(i + 1);
                return;
            }
        }
    }

    private void seleccionarReserva() {
        if (actualizandoSelector) return;
        int indice = cmbReservas.getSelectedIndex() - 1;
        if (indice < 0 || indice >= reservasDisponibles.size()) {
            limpiarDetalle();
            return;
        }
        ReservaBitacora reserva = reservasDisponibles.get(indice);
        dateFecha.setDate(reserva.fecha());
        seleccionarCombo(cmbLaboratorio, reserva.laboratorio());
        txtHorario.setText(reserva.horario());
        txtActividad.setText(reserva.actividad());
        txtCarrera.setText(reserva.carrera());
        seleccionarCombo(cmbGrado, reserva.grado());
        seleccionarCombo(cmbGrupo, reserva.grupo());
        seleccionarCombo(cmbTurno, reserva.turno());
        txtTotalUsuarios.setText(String.valueOf(reserva.totalUsuarios()));
        txtObservaciones.setText("");
        btnGuardar.setEnabled(true);
    }

    private void limpiarDetalle() {
        dateFecha.setDate(null);
        cmbLaboratorio.setSelectedIndex(0);
        txtHorario.setText("");
        cmbTurno.setSelectedIndex(0);
        cmbGrado.setSelectedIndex(0);
        cmbGrupo.setSelectedIndex(0);
        txtActividad.setText("");
        txtCarrera.setText("");
        txtTotalUsuarios.setText("");
        txtObservaciones.setText("");
        btnGuardar.setEnabled(false);
    }

    private void mostrarSinReservas() {
        javax.swing.JOptionPane.showMessageDialog(this,
                "No tienes reservas aprobadas pendientes de registrar.\nLas reservas futuras aparecerán el día programado.",
                "Sin reservas disponibles", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    private Integer idReservaSeleccionada() {
        int indice = cmbReservas.getSelectedIndex() - 1;
        return indice >= 0 && indice < reservasDisponibles.size() ? reservasDisponibles.get(indice).id() : null;
    }

    private void guardarBitacora() {
        Integer idReserva = idReservaSeleccionada();
        if (idReserva == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Selecciona una reserva aprobada.", "Reserva requerida", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!sesion.estaIdentificada()) { javax.swing.JOptionPane.showMessageDialog(this, "La sesión no está identificada. Inicia sesión nuevamente.", "Sesión inválida", javax.swing.JOptionPane.ERROR_MESSAGE); return; }
        String sql = "INSERT INTO bitacora (fecha, nombre_usuario, rol_usuario, carrera_dependencia, grado, grupo, laboratorio, actividad_materia, turno, horario, total_usuarios, estado, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection con = ConexionBD.conectar()) {
            if (con == null) throw new java.sql.SQLException("No hay conexión con la base de datos.");
            con.setAutoCommit(false);
            if (!marcarReservaFinalizada(con, idReserva)) {
                con.rollback();
                javax.swing.JOptionPane.showMessageDialog(this, "La reserva ya fue registrada o dejó de estar aprobada.", "Reserva no disponible", javax.swing.JOptionPane.WARNING_MESSAGE);
                cargarReservasAprobadas(null);
                return;
            }
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, new java.sql.Date(dateFecha.getDate().getTime())); ps.setString(2, sesion.getNombreCompleto()); ps.setString(3, sesion.getRol()); ps.setString(4, txtCarrera.getText().trim()); ps.setString(5, cmbGrado.getSelectedItem().toString()); ps.setString(6, cmbGrupo.getSelectedItem().toString()); ps.setString(7, cmbLaboratorio.getSelectedItem().toString()); ps.setString(8, txtActividad.getText().trim()); ps.setString(9, cmbTurno.getSelectedItem().toString()); ps.setString(10, txtHorario.getText().trim()); ps.setInt(11, Integer.parseInt(txtTotalUsuarios.getText())); ps.setString(12, "Registrado"); String obs=txtObservaciones.getText().trim(); if(obs.isEmpty()) ps.setNull(13,java.sql.Types.VARCHAR); else ps.setString(13,obs); ps.executeUpdate();
            }
            con.commit();
            javax.swing.JOptionPane.showMessageDialog(this, "La reserva se registró en la bitácora.", "Registro guardado", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            cargarReservasAprobadas(null);
        } catch (java.sql.SQLException ex) { javax.swing.JOptionPane.showMessageDialog(this, "No se pudo guardar la bitácora:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE); }
    }

    private boolean marcarReservaFinalizada(java.sql.Connection con, int idReserva) throws java.sql.SQLException {
        String sql = "UPDATE reservas SET estado = 'Finalizada' WHERE id_reserva = ? AND estado = 'Aprobada' "
                + "AND rol_solicitante = 'Profesor' AND (id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ?))";
        try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            ps.setInt(2, sesion.getId());
            ps.setString(3, sesion.getNombreCompleto());
            return ps.executeUpdate() == 1;
        }
    }

    private record ReservaBitacora(int id, java.sql.Date fecha, String horario, String laboratorio,
            String actividad, String carrera, String grado, String grupo, String turno, int totalUsuarios) {
        private String descripcion() {
            return String.format("#%d | %s | %s | %s", id, fecha, horario, laboratorio);
        }
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
        panelFormulario = new javax.swing.JPanel();
        lbTituloDatos = new javax.swing.JLabel();
        cmbReservas = new javax.swing.JComboBox<String>();
        btnCargarReserva = new javax.swing.JButton();
        lbFecha = new javax.swing.JLabel();
        dateFecha = new com.toedter.calendar.JDateChooser();
        lbLaboratorio = new javax.swing.JLabel();
        cmbLaboratorio = new javax.swing.JComboBox<String>();
        lbHorario = new javax.swing.JLabel();
        txtHorario = new javax.swing.JTextField();
        lbProfesor = new javax.swing.JLabel();
        txtProfesor = new javax.swing.JTextField();
        lbRol = new javax.swing.JLabel();
        txtRol = new javax.swing.JTextField();
        lbTurno = new javax.swing.JLabel();
        cmbTurno = new javax.swing.JComboBox<String>();
        lbCarrera = new javax.swing.JLabel();
        txtCarrera = new javax.swing.JTextField();
        lbGrado = new javax.swing.JLabel();
        cmbGrado = new javax.swing.JComboBox<String>();
        lbGrupo = new javax.swing.JLabel();
        cmbGrupo = new javax.swing.JComboBox<String>();
        lbActividad = new javax.swing.JLabel();
        txtActividad = new javax.swing.JTextField();
        lbTotal = new javax.swing.JLabel();
        txtTotalUsuarios = new javax.swing.JTextField();
        lbObservaciones = new javax.swing.JLabel();
        scrollObservaciones = new javax.swing.JScrollPane();
        txtObservaciones = new javax.swing.JTextArea();
        btnLimpiar = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        panelGuia = new javax.swing.JPanel();
        lbTituloGuia = new javax.swing.JLabel();
        lbGuia1 = new javax.swing.JLabel();
        lbGuia2 = new javax.swing.JLabel();
        lbGuia3 = new javax.swing.JLabel();
        panelCampos = new javax.swing.JPanel();
        lbTituloCampos = new javax.swing.JLabel();
        lbCampo1 = new javax.swing.JLabel();
        lbCampo2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LabSync - Bitácora");
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
        lbTituloPantalla.setText("Bitácora");
        body.add(lbTituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 15, 420, 30));
        lbSubtituloPantalla.setFont(new java.awt.Font("Arial", 0, 13));
        lbSubtituloPantalla.setForeground(new java.awt.Color(100, 100, 100));
        lbSubtituloPantalla.setText("Elige una reserva aprobada; sus datos se cargarán automáticamente.");
        body.add(lbSubtituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 45, 850, 22));
        panelFormulario.setBackground(new java.awt.Color(255, 255, 255));
        panelFormulario.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelFormulario.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloDatos.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloDatos.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloDatos.setText("Selecciona una reserva");
        panelFormulario.add(lbTituloDatos, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 15, 225, 28));
        cmbReservas.setFont(new java.awt.Font("Arial", 0, 12));
        cmbReservas.setForeground(new java.awt.Color(51, 51, 51));
        cmbReservas.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Selecciona una reserva aprobada"}));
        cmbReservas.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 15, 260, 30));
        btnCargarReserva.setBackground(new java.awt.Color(6, 140, 115));
        btnCargarReserva.setFont(new java.awt.Font("Arial", 1, 14));
        btnCargarReserva.setForeground(new java.awt.Color(255, 255, 255));
        btnCargarReserva.setText("Actualizar");
        btnCargarReserva.setBorderPainted(false);
        btnCargarReserva.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCargarReserva.setFocusPainted(false);
        panelFormulario.add(btnCargarReserva, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 14, 95, 32));
        lbFecha.setFont(new java.awt.Font("Arial", 1, 12));
        lbFecha.setForeground(new java.awt.Color(90, 90, 90));
        lbFecha.setText("Fecha");
        panelFormulario.add(lbFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 58, 100, 18));
        dateFecha.setBackground(new java.awt.Color(255, 255, 255));
        dateFecha.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        dateFecha.setForeground(new java.awt.Color(51, 51, 51));
        dateFecha.setDateFormatString("yyyy-MM-dd");
        dateFecha.setFont(new java.awt.Font("Arial", 0, 12));
        panelFormulario.add(dateFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 80, 175, 28));
        lbLaboratorio.setFont(new java.awt.Font("Arial", 1, 12));
        lbLaboratorio.setForeground(new java.awt.Color(90, 90, 90));
        lbLaboratorio.setText("Laboratorio");
        panelFormulario.add(lbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 58, 120, 18));
        cmbLaboratorio.setFont(new java.awt.Font("Arial", 0, 12));
        cmbLaboratorio.setForeground(new java.awt.Color(51, 51, 51));
        cmbLaboratorio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar"}));
        cmbLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 80, 180, 28));
        lbHorario.setFont(new java.awt.Font("Arial", 1, 12));
        lbHorario.setForeground(new java.awt.Color(90, 90, 90));
        lbHorario.setText("Horario");
        panelFormulario.add(lbHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 58, 120, 18));
        txtHorario.setBackground(new java.awt.Color(255, 255, 255));
        txtHorario.setFont(new java.awt.Font("Arial", 0, 12));
        txtHorario.setForeground(new java.awt.Color(51, 51, 51));
        txtHorario.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(txtHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 80, 195, 28));
        lbProfesor.setFont(new java.awt.Font("Arial", 1, 12));
        lbProfesor.setForeground(new java.awt.Color(90, 90, 90));
        lbProfesor.setText("Profesor");
        panelFormulario.add(lbProfesor, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 125, 120, 18));
        txtProfesor.setBackground(new java.awt.Color(255, 255, 255));
        txtProfesor.setFont(new java.awt.Font("Arial", 0, 12));
        txtProfesor.setForeground(new java.awt.Color(51, 51, 51));
        txtProfesor.setEditable(false);
        txtProfesor.setText("Profesor");
        txtProfesor.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(txtProfesor, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 147, 275, 28));
        lbRol.setFont(new java.awt.Font("Arial", 1, 12));
        lbRol.setForeground(new java.awt.Color(90, 90, 90));
        lbRol.setText("Rol");
        panelFormulario.add(lbRol, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 125, 90, 18));
        txtRol.setBackground(new java.awt.Color(255, 255, 255));
        txtRol.setFont(new java.awt.Font("Arial", 0, 12));
        txtRol.setForeground(new java.awt.Color(51, 51, 51));
        txtRol.setEditable(false);
        txtRol.setText("Profesor");
        txtRol.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(txtRol, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 147, 120, 28));
        lbTurno.setFont(new java.awt.Font("Arial", 1, 12));
        lbTurno.setForeground(new java.awt.Color(90, 90, 90));
        lbTurno.setText("Turno");
        panelFormulario.add(lbTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 125, 100, 18));
        cmbTurno.setFont(new java.awt.Font("Arial", 0, 12));
        cmbTurno.setForeground(new java.awt.Color(51, 51, 51));
        cmbTurno.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar", "Matutino", "Vespertino"}));
        cmbTurno.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 147, 155, 28));
        lbCarrera.setFont(new java.awt.Font("Arial", 1, 12));
        lbCarrera.setForeground(new java.awt.Color(90, 90, 90));
        lbCarrera.setText("Carrera");
        panelFormulario.add(lbCarrera, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 192, 180, 18));
        txtCarrera.setBackground(new java.awt.Color(255, 255, 255));
        txtCarrera.setFont(new java.awt.Font("Arial", 0, 12));
        txtCarrera.setForeground(new java.awt.Color(51, 51, 51));
        txtCarrera.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(txtCarrera, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 214, 275, 28));
        lbGrado.setFont(new java.awt.Font("Arial", 1, 12));
        lbGrado.setForeground(new java.awt.Color(90, 90, 90));
        lbGrado.setText("Grado");
        panelFormulario.add(lbGrado, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 192, 90, 18));
        cmbGrado.setFont(new java.awt.Font("Arial", 0, 12));
        cmbGrado.setForeground(new java.awt.Color(51, 51, 51));
        cmbGrado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar", "1°", "2°", "3°", "4°", "5°", "6°", "N/A"}));
        cmbGrado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbGrado, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 214, 120, 28));
        lbGrupo.setFont(new java.awt.Font("Arial", 1, 12));
        lbGrupo.setForeground(new java.awt.Color(90, 90, 90));
        lbGrupo.setText("Grupo");
        panelFormulario.add(lbGrupo, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 192, 90, 18));
        cmbGrupo.setFont(new java.awt.Font("Arial", 0, 12));
        cmbGrupo.setForeground(new java.awt.Color(51, 51, 51));
        cmbGrupo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar", "A", "B"}));
        cmbGrupo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbGrupo, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 214, 155, 28));
        lbActividad.setFont(new java.awt.Font("Arial", 1, 12));
        lbActividad.setForeground(new java.awt.Color(90, 90, 90));
        lbActividad.setText("Actividad o materia");
        panelFormulario.add(lbActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 259, 180, 18));
        txtActividad.setBackground(new java.awt.Color(255, 255, 255));
        txtActividad.setFont(new java.awt.Font("Arial", 0, 12));
        txtActividad.setForeground(new java.awt.Color(51, 51, 51));
        txtActividad.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(txtActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 281, 390, 28));
        lbTotal.setFont(new java.awt.Font("Arial", 1, 12));
        lbTotal.setForeground(new java.awt.Color(90, 90, 90));
        lbTotal.setText("Total de usuarios");
        panelFormulario.add(lbTotal, new org.netbeans.lib.awtextra.AbsoluteConstraints(435, 259, 150, 18));
        txtTotalUsuarios.setBackground(new java.awt.Color(255, 255, 255));
        txtTotalUsuarios.setFont(new java.awt.Font("Arial", 0, 12));
        txtTotalUsuarios.setForeground(new java.awt.Color(51, 51, 51));
        txtTotalUsuarios.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(txtTotalUsuarios, new org.netbeans.lib.awtextra.AbsoluteConstraints(435, 281, 180, 28));
        lbObservaciones.setFont(new java.awt.Font("Arial", 1, 12));
        lbObservaciones.setForeground(new java.awt.Color(90, 90, 90));
        lbObservaciones.setText("Observaciones");
        panelFormulario.add(lbObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 326, 180, 18));
        txtObservaciones.setColumns(20);
        txtObservaciones.setFont(new java.awt.Font("Arial", 0, 12));
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setRows(5);
        txtObservaciones.setWrapStyleWord(true);
        scrollObservaciones.setViewportView(txtObservaciones);
        panelFormulario.add(scrollObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 348, 590, 75));
        btnLimpiar.setBackground(new java.awt.Color(230, 230, 230));
        btnLimpiar.setFont(new java.awt.Font("Arial", 1, 14));
        btnLimpiar.setForeground(new java.awt.Color(70, 70, 70));
        btnLimpiar.setText("Deseleccionar");
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnLimpiar.setFocusPainted(false);
        panelFormulario.add(btnLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 442, 120, 38));
        btnGuardar.setBackground(new java.awt.Color(8, 173, 141));
        btnGuardar.setFont(new java.awt.Font("Arial", 1, 14));
        btnGuardar.setForeground(new java.awt.Color(255, 255, 255));
        btnGuardar.setText("Guardar en bitácora");
        btnGuardar.setBorderPainted(false);
        btnGuardar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnGuardar.setFocusPainted(false);
        panelFormulario.add(btnGuardar, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 442, 175, 38));
        body.add(panelFormulario, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 85, 650, 500));
        panelGuia.setBackground(new java.awt.Color(255, 255, 255));
        panelGuia.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelGuia.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloGuia.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloGuia.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloGuia.setText("Guía rápida");
        panelGuia.add(lbTituloGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, 220, 26));
        lbGuia1.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia1.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia1.setText("<html>1. Selecciona una reserva aprobada pendiente de registrar.</html>");
        panelGuia.add(lbGuia1, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 60, 240, 45));
        lbGuia2.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia2.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia2.setText("<html>2. Verifica los datos cargados automáticamente.</html>");
        panelGuia.add(lbGuia2, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 112, 240, 45));
        lbGuia3.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia3.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia3.setText("<html>3. Agrega observaciones opcionales y guarda.</html>");
        panelGuia.add(lbGuia3, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 164, 240, 50));
        body.add(panelGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(705, 85, 290, 250));
        panelCampos.setBackground(new java.awt.Color(255, 255, 255));
        panelCampos.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelCampos.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloCampos.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloCampos.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloCampos.setText("Datos automáticos");
        panelCampos.add(lbTituloCampos, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, 235, 26));
        lbCampo1.setFont(new java.awt.Font("Arial", 0, 12));
        lbCampo1.setForeground(new java.awt.Color(90, 90, 90));
        lbCampo1.setText("<html>El nombre y rol se obtienen del usuario que inició sesión.</html>");
        panelCampos.add(lbCampo1, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 60, 240, 45));
        lbCampo2.setFont(new java.awt.Font("Arial", 0, 12));
        lbCampo2.setForeground(new java.awt.Color(90, 90, 90));
        lbCampo2.setText("<html>La carrera, fecha, laboratorio, actividad, grupo, turno, horario y usuarios provienen de la reserva.</html>");
        panelCampos.add(lbCampo2, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 112, 240, 75));
        body.add(panelCampos, new org.netbeans.lib.awtextra.AbsoluteConstraints(705, 355, 290, 230));

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
            java.util.logging.Logger.getLogger(BitacoraProfesor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new BitacoraProfesor("Profesor").setVisible(true));
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
    private javax.swing.JPanel panelFormulario;
    private javax.swing.JLabel lbTituloDatos;
    private javax.swing.JComboBox<String> cmbReservas;
    private javax.swing.JButton btnCargarReserva;
    private javax.swing.JLabel lbFecha;
    private com.toedter.calendar.JDateChooser dateFecha;
    private javax.swing.JLabel lbLaboratorio;
    private javax.swing.JComboBox<String> cmbLaboratorio;
    private javax.swing.JLabel lbHorario;
    private javax.swing.JTextField txtHorario;
    private javax.swing.JLabel lbProfesor;
    private javax.swing.JTextField txtProfesor;
    private javax.swing.JLabel lbRol;
    private javax.swing.JTextField txtRol;
    private javax.swing.JLabel lbTurno;
    private javax.swing.JComboBox<String> cmbTurno;
    private javax.swing.JLabel lbCarrera;
    private javax.swing.JTextField txtCarrera;
    private javax.swing.JLabel lbGrado;
    private javax.swing.JComboBox<String> cmbGrado;
    private javax.swing.JLabel lbGrupo;
    private javax.swing.JComboBox<String> cmbGrupo;
    private javax.swing.JLabel lbActividad;
    private javax.swing.JTextField txtActividad;
    private javax.swing.JLabel lbTotal;
    private javax.swing.JTextField txtTotalUsuarios;
    private javax.swing.JLabel lbObservaciones;
    private javax.swing.JScrollPane scrollObservaciones;
    private javax.swing.JTextArea txtObservaciones;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JPanel panelGuia;
    private javax.swing.JLabel lbTituloGuia;
    private javax.swing.JLabel lbGuia1;
    private javax.swing.JLabel lbGuia2;
    private javax.swing.JLabel lbGuia3;
    private javax.swing.JPanel panelCampos;
    private javax.swing.JLabel lbTituloCampos;
    private javax.swing.JLabel lbCampo1;
    private javax.swing.JLabel lbCampo2;
    // End of variables declaration//GEN-END:variables
}
