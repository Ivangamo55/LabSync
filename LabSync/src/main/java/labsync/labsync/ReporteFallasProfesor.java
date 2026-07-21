package labsync.labsync;

/**
 * Interfaz del usuario Profesor para LabSync.
 * Archivo JFrame Form compatible con el diseñador visual de NetBeans.
 * La sección initComponents se mantiene sincronizada con ReporteFallasProfesor.form.
 */
public class ReporteFallasProfesor extends javax.swing.JFrame {

    private String nombreUsuario;
    private SesionUsuario sesion;

    public ReporteFallasProfesor() {
        this("Profesor");
    }

    public ReporteFallasProfesor(String nombreRecibido) {
        this(SesionUsuario.buscarProfesor(nombreRecibido));
    }

    public ReporteFallasProfesor(SesionUsuario sesionRecibida) {
        initComponents();
        sesion = sesionRecibida == null ? SesionUsuario.buscarProfesor("Profesor") : sesionRecibida;
        nombreUsuario = sesion.getNombre();
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

    private void cargarLaboratorios(javax.swing.JComboBox<String> combo, boolean incluirTodos) {
        combo.removeAllItems();
        combo.addItem(incluirTodos ? "Todos" : "Seleccionar");
        String sql = "SELECT nombre FROM laboratorios WHERE estado = 'Disponible' ORDER BY nombre";
        try (java.sql.Connection con = ConexionBD.conectar()) {
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
        dateFecha.setMaxSelectableDate(new java.util.Date());
        ValidacionFechas.bloquearFinesDeSemana(dateFecha);
        dateFecha.setDate(ValidacionFechas.anteriorDiaHabil(new java.util.Date()));
        cargarLaboratorios(cmbLaboratorio, false);
        tablaReportes.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, new String[]{"ID", "Fecha", "Laboratorio", "Equipo", "Estado"}) { @Override public boolean isCellEditable(int row, int column) { return false; } });
        tablaReportes.setRowHeight(32); ocultarPrimeraColumna(tablaReportes); cargarReportes();
        btnLimpiar.addActionListener(e -> limpiarFormulario()); btnEnviar.addActionListener(e -> enviarReporte()); btnVerDetalle.addActionListener(e -> verDetalle());
    }

    private void cargarReportes() {
        javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) tablaReportes.getModel();
        modelo.setRowCount(0);
        String sql = "SELECT id_falla, DATE_FORMAT(fecha_reporte,'%d/%m/%Y') fecha, laboratorio, "
                + "COALESCE(NULLIF(codigo_equipo,''), nombre_equipo, 'Sin especificar') equipo, estado "
                + "FROM reporte_fallas WHERE (id_usuario = ? OR (id_usuario IS NULL AND reportado_por = ?)) "
                + "AND rol_reportante = 'Profesor' ORDER BY fecha_reporte DESC LIMIT 8";
        try (java.sql.Connection con = ConexionBD.conectar()) {
            if (con == null) return;
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, sesion.getId());
                ps.setString(2, sesion.getNombreCompleto());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) modelo.addRow(new Object[]{rs.getInt("id_falla"), rs.getString("fecha"), rs.getString("laboratorio"), rs.getString("equipo"), rs.getString("estado")});
                }
            }
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudieron cargar los reportes:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario(){dateFecha.setDate(ValidacionFechas.anteriorDiaHabil(new java.util.Date()));if(cmbLaboratorio.getItemCount()>0)cmbLaboratorio.setSelectedIndex(0);txtCodigo.setText("");cmbTipo.setSelectedIndex(0);txtDescripcion.setText("");}

    private void enviarReporte(){
        if(dateFecha.getDate()==null||cmbLaboratorio.getSelectedIndex()==0||cmbTipo.getSelectedIndex()==0||txtDescripcion.getText().isBlank()){javax.swing.JOptionPane.showMessageDialog(this,"Selecciona fecha, laboratorio, tipo y describe la falla.","Datos requeridos",javax.swing.JOptionPane.WARNING_MESSAGE);return;}
        if (!ValidacionFechas.validarDiaHabil(this, dateFecha.getDate(), "registrar fallas")) return;
        java.time.LocalDate fecha = new java.sql.Date(dateFecha.getDate().getTime()).toLocalDate();
        if (fecha.isAfter(java.time.LocalDate.now())) { javax.swing.JOptionPane.showMessageDialog(this,"La fecha de la falla no puede ser futura.","Fecha no válida",javax.swing.JOptionPane.WARNING_MESSAGE);return; }
        if (!sesion.estaIdentificada()) { javax.swing.JOptionPane.showMessageDialog(this,"La sesión no está identificada. Inicia sesión nuevamente.","Sesión inválida",javax.swing.JOptionPane.ERROR_MESSAGE);return; }
        String sql="INSERT INTO reporte_fallas (id_usuario, codigo_equipo, nombre_equipo, laboratorio, reportado_por, rol_reportante, descripcion_falla, estado, fecha_reporte) VALUES (?, ?, ?, ?, ?, ?, ?, 'Pendiente', ?)";
        try(java.sql.Connection con=ConexionBD.conectar()){if(con==null)throw new java.sql.SQLException("No hay conexión con la base de datos.");try(java.sql.PreparedStatement ps=con.prepareStatement(sql)){ps.setInt(1,sesion.getId());String codigo=txtCodigo.getText().trim();if(codigo.isEmpty())ps.setNull(2,java.sql.Types.VARCHAR);else ps.setString(2,codigo);ps.setNull(3,java.sql.Types.VARCHAR);ps.setString(4,cmbLaboratorio.getSelectedItem().toString());ps.setString(5,sesion.getNombreCompleto());ps.setString(6,sesion.getRol());ps.setString(7,"[Tipo: "+cmbTipo.getSelectedItem()+"] "+txtDescripcion.getText().trim());java.time.LocalTime hora=fecha.equals(java.time.LocalDate.now())?java.time.LocalTime.now():java.time.LocalTime.NOON;ps.setTimestamp(8,java.sql.Timestamp.valueOf(fecha.atTime(hora)));ps.executeUpdate();}javax.swing.JOptionPane.showMessageDialog(this,"El reporte fue enviado con estado Pendiente.","Reporte registrado",javax.swing.JOptionPane.INFORMATION_MESSAGE);limpiarFormulario();cargarReportes();}catch(java.sql.SQLException ex){javax.swing.JOptionPane.showMessageDialog(this,"No se pudo registrar el reporte:\n"+ex.getMessage(),"Error SQL",javax.swing.JOptionPane.ERROR_MESSAGE);}
    }

    private void verDetalle(){int fila=tablaReportes.getSelectedRow();if(fila<0){javax.swing.JOptionPane.showMessageDialog(this,"Selecciona un reporte.","Sin selección",javax.swing.JOptionPane.WARNING_MESSAGE);return;}fila=tablaReportes.convertRowIndexToModel(fila);int id=((Number)tablaReportes.getModel().getValueAt(fila,0)).intValue();String sql="SELECT fecha_reporte,laboratorio,COALESCE(NULLIF(codigo_equipo,''),nombre_equipo,'Sin especificar') equipo,descripcion_falla,estado,COALESCE(observaciones,'Sin observaciones') observaciones FROM reporte_fallas WHERE id_falla=? AND (id_usuario=? OR (id_usuario IS NULL AND reportado_por=?))";try(java.sql.Connection con=ConexionBD.conectar()){if(con==null)throw new java.sql.SQLException("No hay conexión con la base de datos.");try(java.sql.PreparedStatement ps=con.prepareStatement(sql)){ps.setInt(1,id);ps.setInt(2,sesion.getId());ps.setString(3,sesion.getNombreCompleto());try(java.sql.ResultSet rs=ps.executeQuery()){if(rs.next()){String d="Fecha: "+rs.getTimestamp("fecha_reporte")+"\nLaboratorio: "+rs.getString("laboratorio")+"\nEquipo: "+rs.getString("equipo")+"\nEstado: "+rs.getString("estado")+"\n\nDescripción:\n"+rs.getString("descripcion_falla")+"\n\nObservaciones:\n"+rs.getString("observaciones");javax.swing.JOptionPane.showMessageDialog(this,d,"Detalle del reporte",javax.swing.JOptionPane.INFORMATION_MESSAGE);}}}}catch(java.sql.SQLException ex){javax.swing.JOptionPane.showMessageDialog(this,"No se pudo consultar el detalle:\n"+ex.getMessage(),"Error SQL",javax.swing.JOptionPane.ERROR_MESSAGE);}}

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
        lbTituloFormulario = new javax.swing.JLabel();
        lbFecha = new javax.swing.JLabel();
        dateFecha = new com.toedter.calendar.JDateChooser();
        lbLaboratorio = new javax.swing.JLabel();
        cmbLaboratorio = new javax.swing.JComboBox<String>();
        lbCodigo = new javax.swing.JLabel();
        txtCodigo = new javax.swing.JTextField();
        lbTipo = new javax.swing.JLabel();
        cmbTipo = new javax.swing.JComboBox<String>();
        lbDescripcion = new javax.swing.JLabel();
        scrollDescripcion = new javax.swing.JScrollPane();
        txtDescripcion = new javax.swing.JTextArea();
        btnLimpiar = new javax.swing.JButton();
        btnEnviar = new javax.swing.JButton();
        panelGuia = new javax.swing.JPanel();
        lbTituloGuia = new javax.swing.JLabel();
        lbGuia1 = new javax.swing.JLabel();
        lbGuia2 = new javax.swing.JLabel();
        lbGuia3 = new javax.swing.JLabel();
        panelTabla = new javax.swing.JPanel();
        lbTituloTabla = new javax.swing.JLabel();
        scrollReportes = new javax.swing.JScrollPane();
        tablaReportes = new javax.swing.JTable();
        btnVerDetalle = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LabSync - Reportar falla");
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
        lbTituloPantalla.setText("Reportar falla");
        body.add(lbTituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 15, 420, 30));
        lbSubtituloPantalla.setFont(new java.awt.Font("Arial", 0, 13));
        lbSubtituloPantalla.setForeground(new java.awt.Color(100, 100, 100));
        lbSubtituloPantalla.setText("Registra una falla encontrada en un equipo o instalación del laboratorio.");
        body.add(lbSubtituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 45, 800, 22));
        panelFormulario.setBackground(new java.awt.Color(255, 255, 255));
        panelFormulario.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelFormulario.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloFormulario.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloFormulario.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloFormulario.setText("Nuevo reporte");
        panelFormulario.add(lbTituloFormulario, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 15, 250, 28));
        lbFecha.setFont(new java.awt.Font("Arial", 1, 12));
        lbFecha.setForeground(new java.awt.Color(90, 90, 90));
        lbFecha.setText("Fecha del reporte");
        panelFormulario.add(lbFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 58, 150, 18));
        dateFecha.setBackground(new java.awt.Color(255, 255, 255));
        dateFecha.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        dateFecha.setForeground(new java.awt.Color(51, 51, 51));
        dateFecha.setDateFormatString("yyyy-MM-dd");
        dateFecha.setFont(new java.awt.Font("Arial", 0, 12));
        panelFormulario.add(dateFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 80, 175, 28));
        lbLaboratorio.setFont(new java.awt.Font("Arial", 1, 12));
        lbLaboratorio.setForeground(new java.awt.Color(90, 90, 90));
        lbLaboratorio.setText("Laboratorio");
        panelFormulario.add(lbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 58, 140, 18));
        cmbLaboratorio.setFont(new java.awt.Font("Arial", 0, 12));
        cmbLaboratorio.setForeground(new java.awt.Color(51, 51, 51));
        cmbLaboratorio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar"}));
        cmbLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 80, 180, 28));
        lbCodigo.setFont(new java.awt.Font("Arial", 1, 12));
        lbCodigo.setForeground(new java.awt.Color(90, 90, 90));
        lbCodigo.setText("Código del equipo");
        panelFormulario.add(lbCodigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 128, 160, 18));
        txtCodigo.setBackground(new java.awt.Color(255, 255, 255));
        txtCodigo.setFont(new java.awt.Font("Arial", 0, 12));
        txtCodigo.setForeground(new java.awt.Color(51, 51, 51));
        txtCodigo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(txtCodigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 150, 175, 28));
        lbTipo.setFont(new java.awt.Font("Arial", 1, 12));
        lbTipo.setForeground(new java.awt.Color(90, 90, 90));
        lbTipo.setText("Tipo de falla");
        panelFormulario.add(lbTipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 128, 140, 18));
        cmbTipo.setFont(new java.awt.Font("Arial", 0, 12));
        cmbTipo.setForeground(new java.awt.Color(51, 51, 51));
        cmbTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar", "Hardware", "Software", "Red o Internet", "Energía", "Limpieza", "Mobiliario", "Otro"}));
        cmbTipo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbTipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 150, 180, 28));
        lbDescripcion.setFont(new java.awt.Font("Arial", 1, 12));
        lbDescripcion.setForeground(new java.awt.Color(90, 90, 90));
        lbDescripcion.setText("Descripción de la falla");
        panelFormulario.add(lbDescripcion, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 205, 200, 18));
        txtDescripcion.setColumns(20);
        txtDescripcion.setFont(new java.awt.Font("Arial", 0, 12));
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setRows(5);
        txtDescripcion.setWrapStyleWord(true);
        scrollDescripcion.setViewportView(txtDescripcion);
        panelFormulario.add(scrollDescripcion, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 227, 375, 155));
        btnLimpiar.setBackground(new java.awt.Color(230, 230, 230));
        btnLimpiar.setFont(new java.awt.Font("Arial", 1, 14));
        btnLimpiar.setForeground(new java.awt.Color(70, 70, 70));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnLimpiar.setFocusPainted(false);
        panelFormulario.add(btnLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(115, 425, 120, 38));
        btnEnviar.setBackground(new java.awt.Color(8, 173, 141));
        btnEnviar.setFont(new java.awt.Font("Arial", 1, 14));
        btnEnviar.setForeground(new java.awt.Color(255, 255, 255));
        btnEnviar.setText("Enviar reporte");
        btnEnviar.setBorderPainted(false);
        btnEnviar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnEnviar.setFocusPainted(false);
        panelFormulario.add(btnEnviar, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 425, 150, 38));
        body.add(panelFormulario, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 85, 430, 500));
        panelGuia.setBackground(new java.awt.Color(255, 255, 255));
        panelGuia.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelGuia.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloGuia.setFont(new java.awt.Font("Arial", 1, 17));
        lbTituloGuia.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloGuia.setText("Guía rápida");
        panelGuia.add(lbTituloGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 15, 150, 24));
        lbGuia1.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia1.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia1.setText("<html>1. Selecciona el laboratorio y escribe el código del equipo si está disponible.</html>");
        panelGuia.add(lbGuia1, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 52, 140, 75));
        lbGuia2.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia2.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia2.setText("<html>2. Elige el tipo de falla y describe claramente lo ocurrido.</html>");
        panelGuia.add(lbGuia2, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 52, 140, 75));
        lbGuia3.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia3.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia3.setText("<html>3. Envía el reporte; su estado inicial será Pendiente.</html>");
        panelGuia.add(lbGuia3, new org.netbeans.lib.awtextra.AbsoluteConstraints(335, 52, 140, 75));
        body.add(panelGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 85, 505, 155));
        panelTabla.setBackground(new java.awt.Color(255, 255, 255));
        panelTabla.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelTabla.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloTabla.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloTabla.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloTabla.setText("Mis reportes recientes");
        panelTabla.add(lbTituloTabla, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 15, 300, 28));
        tablaReportes.setFont(new java.awt.Font("Arial", 0, 12));
        tablaReportes.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][] {
                        
                    },
                    new String[] {"ID", "Fecha", "Laboratorio", "Equipo", "Estado"}
                ) {
                    boolean[] canEdit = new boolean[] {false, false, false, false, false};
                    @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return canEdit[columnIndex]; }
                });
        tablaReportes.setRowHeight(32);
        tablaReportes.setSelectionBackground(new java.awt.Color(224, 247, 241));
        tablaReportes.setShowVerticalLines(false);
        scrollReportes.setViewportView(tablaReportes);
        panelTabla.add(scrollReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 55, 455, 215));
        btnVerDetalle.setBackground(new java.awt.Color(90, 90, 90));
        btnVerDetalle.setFont(new java.awt.Font("Arial", 1, 14));
        btnVerDetalle.setForeground(new java.awt.Color(255, 255, 255));
        btnVerDetalle.setText("Ver detalle");
        btnVerDetalle.setBorderPainted(false);
        btnVerDetalle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnVerDetalle.setFocusPainted(false);
        panelTabla.add(btnVerDetalle, new org.netbeans.lib.awtextra.AbsoluteConstraints(325, 280, 155, 38));
        body.add(panelTabla, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 255, 505, 330));

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
            java.util.logging.Logger.getLogger(ReporteFallasProfesor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new ReporteFallasProfesor("Profesor").setVisible(true));
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
    private javax.swing.JLabel lbTituloFormulario;
    private javax.swing.JLabel lbFecha;
    private com.toedter.calendar.JDateChooser dateFecha;
    private javax.swing.JLabel lbLaboratorio;
    private javax.swing.JComboBox<String> cmbLaboratorio;
    private javax.swing.JLabel lbCodigo;
    private javax.swing.JTextField txtCodigo;
    private javax.swing.JLabel lbTipo;
    private javax.swing.JComboBox<String> cmbTipo;
    private javax.swing.JLabel lbDescripcion;
    private javax.swing.JScrollPane scrollDescripcion;
    private javax.swing.JTextArea txtDescripcion;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnEnviar;
    private javax.swing.JPanel panelGuia;
    private javax.swing.JLabel lbTituloGuia;
    private javax.swing.JLabel lbGuia1;
    private javax.swing.JLabel lbGuia2;
    private javax.swing.JLabel lbGuia3;
    private javax.swing.JPanel panelTabla;
    private javax.swing.JLabel lbTituloTabla;
    private javax.swing.JScrollPane scrollReportes;
    private javax.swing.JTable tablaReportes;
    private javax.swing.JButton btnVerDetalle;
    // End of variables declaration//GEN-END:variables
}
