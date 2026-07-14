package labsync.labsync;

/**
 * JFrame Form editable desde NetBeans Swing Designer.
 * Diseño únicamente visual para la interfaz de Alumno.
 * No contiene funciones de navegación, base de datos ni carga de iconos.
 */
public class DashboardAlumno extends javax.swing.JFrame {

    private String nombreUsuario;
    private int idUsuario;

    public DashboardAlumno() {
        this(0, "Usuario");
    }

    public DashboardAlumno(String nombreRecibido) {
        this(0, nombreRecibido);
    }

    public DashboardAlumno(int idUsuario, String nombreRecibido) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreRecibido;
        initComponents();
        lbNombreUsuario.setText("Hola, " + nombreUsuario);
        configurarNavegacion();
        cargarDashboardAlumno();
        configurarTablaReservaciones();
        setLocationRelativeTo(null);
    }

    private void configurarTablaReservaciones() {
        // La tabla solo presenta un resumen; no hay una accion asociada a seleccionar filas.
        tablaReservaciones.setRowSelectionAllowed(false);
        tablaReservaciones.setCellSelectionEnabled(false);
        tablaReservaciones.setFocusable(false);
        tablaReservaciones.clearSelection();
    }

    private void configurarNavegacion() {
        btnMisReservas.addActionListener(evt -> abrirMisReservas());
        btnReporteFallas.addActionListener(evt -> abrirReporteFallas());
        lbVerTodasReservas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lbVerTodasReservas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                abrirMisReservas();
            }
        });
    }

    /**
     * Abre la vista de todas las reservas del alumno.
     * Este es el punto unico de entrada para la opcion "Mis reservas",
     * tanto desde el dashboard como desde la pantalla para reservar.
     */
    public void abrirMisReservas() {
        mostrarTodasReservas();
    }

    private void abrirReporteFallas() {
        ReporteFallasAlumno reportes = new ReporteFallasAlumno(idUsuario, nombreUsuario);
        reportes.setVisible(true);
        dispose();
    }

    private void cargarDashboardAlumno() {
        javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) tablaReservaciones.getModel();
        modelo.setRowCount(0);
        java.sql.Connection con = ConexionBD.conectar();
        if (con == null) {
            lbValorReservasActivas.setText("-");
            lbValorReportes.setText("-");
            return;
        }

        String filtro = "(id_usuario = ? OR (? = 0 AND nombre_solicitante = ?))";
        try (con) {
            String sqlResumen = "SELECT SUM(CASE WHEN estado IN ('Pendiente','Aprobada') AND fecha >= CURDATE() THEN 1 ELSE 0 END) activas, "
                + "MIN(CASE WHEN estado IN ('Pendiente','Aprobada') AND fecha >= CURDATE() THEN fecha END) proxima "
                + "FROM reservas WHERE " + filtro;
            try (java.sql.PreparedStatement ps = con.prepareStatement(sqlResumen)) {
                asignarUsuario(ps);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lbValorReservasActivas.setText(String.valueOf(rs.getInt("activas")));
                        java.sql.Date proxima = rs.getDate("proxima");
                        lbValorProximaReserva.setText(proxima == null ? "Sin reservas" : proxima.toString());
                        lbDescProximaReserva.setText(proxima == null ? "No tienes reservas proximas" : "Siguiente fecha reservada");
                    }
                }
            }

            String sqlTabla = "SELECT fecha, horario, laboratorio, actividad, estado FROM reservas WHERE " + filtro
                + " AND fecha >= CURDATE() ORDER BY fecha, horario LIMIT 5";
            try (java.sql.PreparedStatement ps = con.prepareStatement(sqlTabla)) {
                asignarUsuario(ps);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modelo.addRow(new Object[]{rs.getDate("fecha"), rs.getString("horario"), rs.getString("laboratorio"), rs.getString("actividad"), rs.getString("estado")});
                    }
                }
            }

            String sqlReportes = "SELECT COUNT(*) total FROM reporte_fallas WHERE id_usuario = ? OR (? = 0 AND reportado_por = ?)";
            try (java.sql.PreparedStatement ps = con.prepareStatement(sqlReportes)) {
                asignarUsuario(ps);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lbValorReportes.setText(String.valueOf(rs.getInt("total")));
                    }
                }
            }
        } catch (java.sql.SQLException ex) {
            java.util.logging.Logger.getLogger(DashboardAlumno.class.getName()).log(java.util.logging.Level.SEVERE, "Error al cargar el dashboard del alumno", ex);
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudieron cargar tus reservas:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void asignarUsuario(java.sql.PreparedStatement ps) throws java.sql.SQLException {
        ps.setInt(1, idUsuario);
        ps.setInt(2, idUsuario);
        ps.setString(3, nombreUsuario);
    }

    private void mostrarTodasReservas() {
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel(
            new Object[][]{}, new String[]{"Fecha", "Horario", "Laboratorio", "Actividad", "Estado"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String sql = "SELECT fecha, horario, laboratorio, actividad, estado FROM reservas "
            + "WHERE (id_usuario = ? OR (? = 0 AND nombre_solicitante = ?)) ORDER BY fecha DESC, horario";
        try (java.sql.Connection con = ConexionBD.conectar()) {
            if (con == null) {
                javax.swing.JOptionPane.showMessageDialog(this, "No fue posible conectarse con la base de datos.", "Error de conexion", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                asignarUsuario(ps);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modelo.addRow(new Object[]{rs.getDate("fecha"), rs.getString("horario"), rs.getString("laboratorio"), rs.getString("actividad"), rs.getString("estado")});
                    }
                }
            }
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudieron consultar tus reservas:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        javax.swing.JTable tabla = new javax.swing.JTable(modelo);
        tabla.setRowHeight(30);
        tabla.setAutoCreateRowSorter(true);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tabla);
        scroll.setPreferredSize(new java.awt.Dimension(850, 360));
        javax.swing.JOptionPane.showMessageDialog(this, scroll, "Todas mis reservas", javax.swing.JOptionPane.PLAIN_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sidebarVerde = new javax.swing.JPanel();
        lbLogoLabSync = new javax.swing.JLabel();
        btnReservas = new javax.swing.JButton();
        btnMisReservas = new javax.swing.JButton();
        btnReporteFallas = new javax.swing.JButton();
        headerBlanco = new javax.swing.JPanel();
        lbLogoUTJ = new javax.swing.JLabel();
        lbNombreUsuario = new javax.swing.JLabel();
        btnCerrarSesion = new javax.swing.JButton();
        panelContenedor = new javax.swing.JPanel();
        tarjetaReservasActivas = new javax.swing.JPanel();
        lbTituloReservasActivas = new javax.swing.JLabel();
        lbValorReservasActivas = new javax.swing.JLabel();
        lbDescReservasActivas = new javax.swing.JLabel();
        tarjetaProximaReserva = new javax.swing.JPanel();
        lbTituloProximaReserva = new javax.swing.JLabel();
        lbValorProximaReserva = new javax.swing.JLabel();
        lbDescProximaReserva = new javax.swing.JLabel();
        tarjetaReportes = new javax.swing.JPanel();
        lbTituloReportes = new javax.swing.JLabel();
        lbValorReportes = new javax.swing.JLabel();
        lbDescReportes = new javax.swing.JLabel();
        panelTablaReservaciones = new javax.swing.JPanel();
        lbTituloTablaReservaciones = new javax.swing.JLabel();
        jScrollPaneReservaciones = new javax.swing.JScrollPane();
        tablaReservaciones = new javax.swing.JTable();
        lbVerTodasReservas = new javax.swing.JLabel();
        panelAvisos = new javax.swing.JPanel();
        lbTituloAvisos = new javax.swing.JLabel();
        lbAviso1Titulo = new javax.swing.JLabel();
        lbAviso1Texto = new javax.swing.JLabel();
        separadorAviso1 = new javax.swing.JSeparator();
        lbAviso2Titulo = new javax.swing.JLabel();
        lbAviso2Texto = new javax.swing.JLabel();
        separadorAviso2 = new javax.swing.JSeparator();
        lbAviso3Titulo = new javax.swing.JLabel();
        lbAviso3Texto = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LabSync - Panel de Alumno");
        setResizable(false);

        sidebarVerde.setBackground(new java.awt.Color(8, 173, 141));
        sidebarVerde.setPreferredSize(new java.awt.Dimension(250, 720));
        sidebarVerde.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbLogoLabSync.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/labsync_blanco_200.png"))); // NOI18N
        sidebarVerde.add(lbLogoLabSync, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 24, 197, -1));

        btnReservas.setBackground(new java.awt.Color(255, 255, 255));
        btnReservas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnReservas.setForeground(new java.awt.Color(6, 140, 115));
        btnReservas.setText("Reservas");
        btnReservas.setBorderPainted(false);
        btnReservas.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnReservas.setFocusPainted(false);
        btnReservas.setPreferredSize(new java.awt.Dimension(200, 50));
        btnReservas.addActionListener(this::btnReservasActionPerformed);
        sidebarVerde.add(btnReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 290, 200, 50));

        btnMisReservas.setBackground(new java.awt.Color(255, 255, 255));
        btnMisReservas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnMisReservas.setForeground(new java.awt.Color(6, 140, 115));
        btnMisReservas.setText("Mis reservas");
        btnMisReservas.setBorderPainted(false);
        btnMisReservas.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnMisReservas.setFocusPainted(false);
        btnMisReservas.setPreferredSize(new java.awt.Dimension(200, 50));
        sidebarVerde.add(btnMisReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 350, 200, 50));

        btnReporteFallas.setBackground(new java.awt.Color(255, 255, 255));
        btnReporteFallas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnReporteFallas.setForeground(new java.awt.Color(6, 140, 115));
        btnReporteFallas.setText("Reporte de fallas");
        btnReporteFallas.setBorderPainted(false);
        btnReporteFallas.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnReporteFallas.setFocusPainted(false);
        btnReporteFallas.setPreferredSize(new java.awt.Dimension(200, 50));
        sidebarVerde.add(btnReporteFallas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 410, 200, 50));

        headerBlanco.setBackground(new java.awt.Color(255, 255, 255));
        headerBlanco.setPreferredSize(new java.awt.Dimension(1030, 100));
        headerBlanco.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbLogoUTJ.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/UTJ_color.png"))); // NOI18N
        headerBlanco.add(lbLogoUTJ, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 25, -1, -1));

        lbNombreUsuario.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbNombreUsuario.setForeground(new java.awt.Color(8, 173, 141));
        lbNombreUsuario.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lbNombreUsuario.setText("Hola, Usuario");
        headerBlanco.add(lbNombreUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(675, 42, 170, 30));

        btnCerrarSesion.setBackground(new java.awt.Color(220, 53, 69));
        btnCerrarSesion.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCerrarSesion.setForeground(new java.awt.Color(255, 255, 255));
        btnCerrarSesion.setText("Cerrar Sesión");
        btnCerrarSesion.setBorderPainted(false);
        btnCerrarSesion.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setPreferredSize(new java.awt.Dimension(130, 36));
        btnCerrarSesion.addActionListener(this::btnCerrarSesionActionPerformed);
        headerBlanco.add(btnCerrarSesion, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 35, 130, 42));

        panelContenedor.setBackground(new java.awt.Color(245, 245, 245));
        panelContenedor.setPreferredSize(new java.awt.Dimension(1030, 625));
        panelContenedor.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tarjetaReservasActivas.setBackground(new java.awt.Color(255, 255, 255));
        tarjetaReservasActivas.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        tarjetaReservasActivas.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloReservasActivas.setFont(new java.awt.Font("Arial", 1, 15)); // NOI18N
        lbTituloReservasActivas.setForeground(new java.awt.Color(70, 70, 70));
        lbTituloReservasActivas.setText("Mis reservas activas");
        tarjetaReservasActivas.add(lbTituloReservasActivas, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 25, 220, 25));

        lbValorReservasActivas.setFont(new java.awt.Font("Arial", 1, 42)); // NOI18N
        lbValorReservasActivas.setForeground(new java.awt.Color(8, 173, 141));
        lbValorReservasActivas.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbValorReservasActivas.setText("0");
        tarjetaReservasActivas.add(lbValorReservasActivas, new org.netbeans.lib.awtextra.AbsoluteConstraints(105, 55, 90, 50));

        lbDescReservasActivas.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbDescReservasActivas.setForeground(new java.awt.Color(90, 90, 90));
        lbDescReservasActivas.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbDescReservasActivas.setText("Reservas en curso");
        tarjetaReservasActivas.add(lbDescReservasActivas, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 108, 220, 20));

        panelContenedor.add(tarjetaReservasActivas, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 40, 300, 140));

        tarjetaProximaReserva.setBackground(new java.awt.Color(255, 255, 255));
        tarjetaProximaReserva.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        tarjetaProximaReserva.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloProximaReserva.setFont(new java.awt.Font("Arial", 1, 15)); // NOI18N
        lbTituloProximaReserva.setForeground(new java.awt.Color(70, 70, 70));
        lbTituloProximaReserva.setText("Próxima reserva");
        tarjetaProximaReserva.add(lbTituloProximaReserva, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 25, 220, 25));

        lbValorProximaReserva.setFont(new java.awt.Font("Arial", 1, 30)); // NOI18N
        lbValorProximaReserva.setForeground(new java.awt.Color(220, 53, 69));
        lbValorProximaReserva.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbValorProximaReserva.setText("Sin reservas");
        tarjetaProximaReserva.add(lbValorProximaReserva, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 58, 240, 45));

        lbDescProximaReserva.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbDescProximaReserva.setForeground(new java.awt.Color(90, 90, 90));
        lbDescProximaReserva.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbDescProximaReserva.setText("No tienes reservas próximas");
        tarjetaProximaReserva.add(lbDescProximaReserva, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 108, 230, 20));

        panelContenedor.add(tarjetaProximaReserva, new org.netbeans.lib.awtextra.AbsoluteConstraints(365, 40, 300, 140));

        tarjetaReportes.setBackground(new java.awt.Color(255, 255, 255));
        tarjetaReportes.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        tarjetaReportes.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloReportes.setFont(new java.awt.Font("Arial", 1, 15)); // NOI18N
        lbTituloReportes.setForeground(new java.awt.Color(70, 70, 70));
        lbTituloReportes.setText("Reportes enviados");
        tarjetaReportes.add(lbTituloReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 25, 220, 25));

        lbValorReportes.setFont(new java.awt.Font("Arial", 1, 42)); // NOI18N
        lbValorReportes.setForeground(new java.awt.Color(8, 173, 141));
        lbValorReportes.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbValorReportes.setText("0");
        tarjetaReportes.add(lbValorReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(105, 55, 90, 50));

        lbDescReportes.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbDescReportes.setForeground(new java.awt.Color(90, 90, 90));
        lbDescReportes.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbDescReportes.setText("Reportes registrados");
        tarjetaReportes.add(lbDescReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 108, 220, 20));

        panelContenedor.add(tarjetaReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(695, 40, 300, 140));

        panelTablaReservaciones.setBackground(new java.awt.Color(255, 255, 255));
        panelTablaReservaciones.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelTablaReservaciones.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloTablaReservaciones.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbTituloTablaReservaciones.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloTablaReservaciones.setText("Mis próximas reservaciones");
        panelTablaReservaciones.add(lbTituloTablaReservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, 320, 30));

        tablaReservaciones.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        tablaReservaciones.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Fecha", "Horario", "Laboratorio", "Actividad", "Estado"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaReservaciones.setGridColor(new java.awt.Color(230, 230, 230));
        tablaReservaciones.setRowHeight(34);
        tablaReservaciones.setSelectionBackground(new java.awt.Color(224, 247, 241));
        tablaReservaciones.setShowVerticalLines(false);
        tablaReservaciones.getTableHeader().setReorderingAllowed(false);
        jScrollPaneReservaciones.setViewportView(tablaReservaciones);

        panelTablaReservaciones.add(jScrollPaneReservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 65, 570, 190));

        lbVerTodasReservas.setFont(new java.awt.Font("Arial", 1, 13)); // NOI18N
        lbVerTodasReservas.setForeground(new java.awt.Color(6, 140, 115));
        lbVerTodasReservas.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lbVerTodasReservas.setText("Ver todas mis reservas  >");
        panelTablaReservaciones.add(lbVerTodasReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(385, 280, 210, 25));

        panelContenedor.add(panelTablaReservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 220, 620, 340));

        panelAvisos.setBackground(new java.awt.Color(255, 255, 255));
        panelAvisos.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelAvisos.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloAvisos.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbTituloAvisos.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloAvisos.setText("Avisos");
        panelAvisos.add(lbTituloAvisos, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 18, 220, 30));

        lbAviso1Titulo.setFont(new java.awt.Font("Arial", 1, 13)); // NOI18N
        lbAviso1Titulo.setForeground(new java.awt.Color(70, 70, 70));
        lbAviso1Titulo.setText("Llega a tiempo");
        panelAvisos.add(lbAviso1Titulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 70, 240, 20));

        lbAviso1Texto.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbAviso1Texto.setForeground(new java.awt.Color(90, 90, 90));
        lbAviso1Texto.setText("<html>Sé puntual para aprovechar al máximo tu reservación.</html>");
        panelAvisos.add(lbAviso1Texto, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 92, 245, 35));
        panelAvisos.add(separadorAviso1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 140, 245, 10));

        lbAviso2Titulo.setFont(new java.awt.Font("Arial", 1, 13)); // NOI18N
        lbAviso2Titulo.setForeground(new java.awt.Color(70, 70, 70));
        lbAviso2Titulo.setText("Cuida el laboratorio");
        panelAvisos.add(lbAviso2Titulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 155, 240, 20));

        lbAviso2Texto.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbAviso2Texto.setForeground(new java.awt.Color(90, 90, 90));
        lbAviso2Texto.setText("<html>Respeta las normas y mantén el espacio limpio.</html>");
        panelAvisos.add(lbAviso2Texto, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 177, 245, 35));
        panelAvisos.add(separadorAviso2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 225, 245, 10));

        lbAviso3Titulo.setFont(new java.awt.Font("Arial", 1, 13)); // NOI18N
        lbAviso3Titulo.setForeground(new java.awt.Color(70, 70, 70));
        lbAviso3Titulo.setText("Reporta cualquier falla");
        panelAvisos.add(lbAviso3Titulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 240, 240, 20));

        lbAviso3Texto.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbAviso3Texto.setForeground(new java.awt.Color(90, 90, 90));
        lbAviso3Texto.setText("<html>Tu reporte ayuda a mantener los laboratorios en buen estado.</html>");
        panelAvisos.add(lbAviso3Texto, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 262, 245, 45));

        panelContenedor.add(panelAvisos, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 220, 305, 340));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sidebarVerde, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(headerBlanco, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelContenedor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sidebarVerde, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(headerBlanco, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelContenedor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCerrarSesionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarSesionActionPerformed
        // TODO add your handling code here:
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro que deseas cerrar sesión?",
            "Cerrar Sesión",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        
        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            Login ventanaLogin = new Login();
            ventanaLogin.setVisible(true);
            ventanaLogin.setLocationRelativeTo(null);
            this.dispose();
        }
    }//GEN-LAST:event_btnCerrarSesionActionPerformed

    private void btnReservasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReservasActionPerformed
        ReservasAlumno ventanaReservasAlumno = new ReservasAlumno(idUsuario, nombreUsuario);
        ventanaReservasAlumno.setVisible(true);
        ventanaReservasAlumno.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReservasActionPerformed

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DashboardAlumno.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            new DashboardAlumno("Usuario").setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnMisReservas;
    private javax.swing.JButton btnReporteFallas;
    private javax.swing.JButton btnReservas;
    private javax.swing.JPanel headerBlanco;
    private javax.swing.JScrollPane jScrollPaneReservaciones;
    private javax.swing.JLabel lbAviso1Texto;
    private javax.swing.JLabel lbAviso1Titulo;
    private javax.swing.JLabel lbAviso2Texto;
    private javax.swing.JLabel lbAviso2Titulo;
    private javax.swing.JLabel lbAviso3Texto;
    private javax.swing.JLabel lbAviso3Titulo;
    private javax.swing.JLabel lbDescProximaReserva;
    private javax.swing.JLabel lbDescReportes;
    private javax.swing.JLabel lbDescReservasActivas;
    private javax.swing.JLabel lbLogoLabSync;
    private javax.swing.JLabel lbLogoUTJ;
    private javax.swing.JLabel lbNombreUsuario;
    private javax.swing.JLabel lbTituloAvisos;
    private javax.swing.JLabel lbTituloProximaReserva;
    private javax.swing.JLabel lbTituloReportes;
    private javax.swing.JLabel lbTituloReservasActivas;
    private javax.swing.JLabel lbTituloTablaReservaciones;
    private javax.swing.JLabel lbValorProximaReserva;
    private javax.swing.JLabel lbValorReportes;
    private javax.swing.JLabel lbValorReservasActivas;
    private javax.swing.JLabel lbVerTodasReservas;
    private javax.swing.JPanel panelAvisos;
    private javax.swing.JPanel panelContenedor;
    private javax.swing.JPanel panelTablaReservaciones;
    private javax.swing.JSeparator separadorAviso1;
    private javax.swing.JSeparator separadorAviso2;
    private javax.swing.JPanel sidebarVerde;
    private javax.swing.JTable tablaReservaciones;
    private javax.swing.JPanel tarjetaProximaReserva;
    private javax.swing.JPanel tarjetaReportes;
    private javax.swing.JPanel tarjetaReservasActivas;
    // End of variables declaration//GEN-END:variables
}
