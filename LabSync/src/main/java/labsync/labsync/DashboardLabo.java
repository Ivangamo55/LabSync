package labsync.labsync;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;

public class DashboardLabo extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DashboardLabo.class.getName());
    private String nombreUsuario;
    
    public DashboardLabo(String nombreRecibido) {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        
        this.nombreUsuario = nombreRecibido;
        lbNombreUsuario.setText("Hola, " + nombreUsuario);
        
        cargarDashboard();
    }

    public DashboardLabo() {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        
        cargarDashboard();
    }
    
    private void cargarDashboard() {
        cargarEquiposConFalla();
        cargarMantenimientosPendientes();
        cargarReservacionesHoy();
        cargarUltimosMantenimiento();
        cargarUltimosRegistrosBitacora();
    }
    
    private void cargarReservacionesHoy() {
        Connection con = ConexionBD.conectar();
        
        if (con == null) {
            lbReservaciones.setText("-");
            return;
        }
        
        String sql = "SELECT COUNT(*) AS total "
            + "FROM reservas "
            + "WHERE fecha = CURDATE() "
            + "AND estado = 'Aprobada'";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                lbReservaciones.setText(String.valueOf(rs.getInt("total")));
            }
        } catch (Exception e) {
            lbReservaciones.setText("E");
        } finally {
            try {
                con.close();
            } catch (SQLException ex) {
                
            }
        }
    }
    
    private void cargarEquiposConFalla() {
        Connection con = ConexionBD.conectar();
        
        if (con == null) {
            lbNumFallaEquipos.setText("-");
            return;
        }
        
        String sql = "SELECT COUNT(*) AS total "
            + "FROM inventario "
            + "WHERE estado = 'Con falla'";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                lbNumFallaEquipos.setText(String.valueOf(rs.getInt("total")));
            }
        } catch (SQLException e) {
            lbNumFallaEquipos.setText("E");
        } finally {
            try {
                con.close();
            } catch (SQLException ex) {
                
            }
        } 
    }
    
    private void cargarMantenimientosPendientes() {
        Connection con = ConexionBD.conectar();
        
        if (con == null) {
            lbMantPendientes.setText("-");
            return;
        }
        
        String sql = "SELECT COUNT(*) AS total "
            + "FROM mantenimiento "
            + "WHERE estado IN ('Pendiente', 'En proceso')";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                lbMantPendientes.setText(String.valueOf(rs.getInt("total")));
            }
        } catch (SQLException e) {
            lbMantPendientes.setText("E");
            System.out.println("Error al cargar mantenimientos pendientes: " + e.getMessage());
        } finally {
            try {
                con.close();
            } catch (SQLException ex) {
                
            }
        }
    }
    
    private void cargarUltimosMantenimiento() {
        DefaultTableModel modelo = new DefaultTableModel();
        Connection con = ConexionBD.conectar();
        
        modelo.addColumn("Fecha");
        modelo.addColumn("Código Equipo");
        modelo.addColumn("Nombre Equipo");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Tipo");
        modelo.addColumn("Estado");
        modelo.addColumn("Responsable");
        
        String sql = "SELECT "
            + "DATE_FORMAT(fecha_programada, '%d/%m/%Y') AS fecha_programada, "
            + "codigo_equipo, "
            + "nombre_equipo, "
            + "laboratorio, "
            + "tipo_mantenimiento, "
            + "estado, "
            + "responsable "
            + "FROM mantenimiento "
            + "ORDER BY fecha_programada DESC, id_mantenimiento DESC "
            + "LIMIT 5";
        
        if (con == null) {
            tablaUltimosMants.setModel(modelo);
            return;
        }
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] fila = new Object[7];
                
                fila[0] = rs.getString("fecha_programada");
                fila[1] = rs.getString("codigo_equipo");
                fila[2] = rs.getString("nombre_equipo");
                fila[3] = rs.getString("laboratorio");
                fila[4] = rs.getString("tipo_mantenimiento");
                fila[5] = rs.getString("estado");
                fila[6] = rs.getString("responsable");
                
                modelo.addRow(fila);
            }
            
            tablaUltimosMants.setModel(modelo);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Error al cargar últimos mantenimientos: " + e.getMessage(),
                "Error SQL",
                JOptionPane.ERROR_MESSAGE
            );
        } finally {
            try {
                con.close();
            } catch (SQLException ex) {
                
            }
        }
    }
    
    private void cargarUltimosRegistrosBitacora() {
        DefaultTableModel modelo = new DefaultTableModel();
        Connection con = ConexionBD.conectar();
        
        modelo.addColumn("Hora");
        modelo.addColumn("Usuario");
        modelo.addColumn("Rol");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Actividad");
        modelo.addColumn("Estado");
        
        String sql = "SELECT "
            + "DATE_FORMAT(fecha_registro, '%H:%i') AS hora, "
            + "nombre_usuario, "
            + "rol_usuario, "
            + "laboratorio, "
            + "actividad_materia, "
            + "estado "
            + "FROM bitacora "
            + "ORDER BY fecha_registro DESC "
            + "LIMIT 5";
        
        if (con == null) {
            tablaBitacora.setModel(modelo);
            return;
        }
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] fila = new Object[6];
                
                fila[0] = rs.getString("hora");
                fila[1] = rs.getString("nombre_usuario");
                fila[2] = rs.getString("rol_usuario");
                fila[3] = rs.getString("laboratorio");
                fila[4] = rs.getString("actividad_materia");
                fila[5] = rs.getString("estado");
                
                modelo.addRow(fila);
            }
            tablaBitacora.setModel(modelo);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Error al cargar últimos registros de bitácora: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        } finally {
            try {
                con.close();
            } catch (SQLException ex) {
                
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sidebarVerde = new javax.swing.JPanel();
        imgLabSync = new javax.swing.JLabel();
        btnBitacora = new javax.swing.JButton();
        btnInventario = new javax.swing.JButton();
        btnMant = new javax.swing.JButton();
        btnReservas = new javax.swing.JButton();
        btnReporteFallas = new javax.swing.JButton();
        headerBlanco = new javax.swing.JPanel();
        imgUTJ = new javax.swing.JLabel();
        lbNombreUsuario = new javax.swing.JLabel();
        btnCerrarSesion = new javax.swing.JButton();
        panelContenedor = new javax.swing.JPanel();
        tarjeta1 = new javax.swing.JPanel();
        lbReservacionesTitulo = new javax.swing.JLabel();
        lbReservaciones = new javax.swing.JLabel();
        tarjeta2 = new javax.swing.JPanel();
        lbEquipos = new javax.swing.JLabel();
        lbNumFallaEquipos = new javax.swing.JLabel();
        tarjeta3 = new javax.swing.JPanel();
        lbMantPendientesTitulo = new javax.swing.JLabel();
        lbMantPendientes = new javax.swing.JLabel();
        panelTabla = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tablaBitacora = new javax.swing.JTable();
        lbBitacora = new javax.swing.JLabel();
        lbUltimoMants = new javax.swing.JLabel();
        panelLabs = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaUltimosMants = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LabSync - Laboratorista");
        setName("LabSync - Laboratorista"); // NOI18N
        setResizable(false);

        sidebarVerde.setBackground(new java.awt.Color(8, 173, 141));
        sidebarVerde.setForeground(new java.awt.Color(255, 255, 255));
        sidebarVerde.setPreferredSize(new java.awt.Dimension(250, 720));
        sidebarVerde.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        imgLabSync.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/labsync_blanco_200.png"))); // NOI18N
        imgLabSync.setText("jLabel1");
        sidebarVerde.add(imgLabSync, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 24, 197, -1));

        btnBitacora.setBackground(new java.awt.Color(255, 255, 255));
        btnBitacora.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnBitacora.setForeground(new java.awt.Color(6, 140, 115));
        btnBitacora.setText("Bitácora");
        btnBitacora.setBorderPainted(false);
        btnBitacora.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBitacora.setFocusPainted(false);
        btnBitacora.setPreferredSize(new java.awt.Dimension(200, 50));
        btnBitacora.addActionListener(this::btnBitacoraActionPerformed);
        sidebarVerde.add(btnBitacora, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 290, 200, -1));

        btnInventario.setBackground(new java.awt.Color(255, 255, 255));
        btnInventario.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnInventario.setForeground(new java.awt.Color(6, 140, 115));
        btnInventario.setText("Inventario");
        btnInventario.setBorderPainted(false);
        btnInventario.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnInventario.setFocusPainted(false);
        btnInventario.setPreferredSize(new java.awt.Dimension(200, 50));
        btnInventario.addActionListener(this::btnInventarioActionPerformed);
        sidebarVerde.add(btnInventario, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 350, 200, -1));

        btnMant.setBackground(new java.awt.Color(255, 255, 255));
        btnMant.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnMant.setForeground(new java.awt.Color(6, 140, 115));
        btnMant.setText("Mantenimiento");
        btnMant.setBorderPainted(false);
        btnMant.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnMant.setFocusPainted(false);
        btnMant.setPreferredSize(new java.awt.Dimension(200, 50));
        btnMant.addActionListener(this::btnMantActionPerformed);
        sidebarVerde.add(btnMant, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 410, 200, -1));

        btnReservas.setBackground(new java.awt.Color(255, 255, 255));
        btnReservas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnReservas.setForeground(new java.awt.Color(6, 140, 115));
        btnReservas.setText("Reservas");
        btnReservas.setBorderPainted(false);
        btnReservas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnReservas.setFocusPainted(false);
        btnReservas.setPreferredSize(new java.awt.Dimension(200, 50));
        btnReservas.addActionListener(this::btnReservasActionPerformed);
        sidebarVerde.add(btnReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 470, 200, -1));

        btnReporteFallas.setBackground(new java.awt.Color(255, 255, 255));
        btnReporteFallas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnReporteFallas.setForeground(new java.awt.Color(6, 140, 115));
        btnReporteFallas.setText("Reporte de Fallas");
        btnReporteFallas.setBorderPainted(false);
        btnReporteFallas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnReporteFallas.setFocusPainted(false);
        btnReporteFallas.setPreferredSize(new java.awt.Dimension(200, 50));
        btnReporteFallas.addActionListener(this::btnReporteFallasActionPerformed);
        sidebarVerde.add(btnReporteFallas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 530, 200, -1));

        headerBlanco.setBackground(new java.awt.Color(255, 255, 255));
        headerBlanco.setPreferredSize(new java.awt.Dimension(1030, 100));
        headerBlanco.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        imgUTJ.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/UTJ_color.png"))); // NOI18N
        headerBlanco.add(imgUTJ, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 25, -1, -1));

        lbNombreUsuario.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbNombreUsuario.setForeground(new java.awt.Color(8, 173, 141));
        lbNombreUsuario.setText("Hola, Usuario");
        headerBlanco.add(lbNombreUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 50, -1, -1));

        btnCerrarSesion.setBackground(new java.awt.Color(220, 53, 69));
        btnCerrarSesion.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCerrarSesion.setForeground(new java.awt.Color(255, 255, 255));
        btnCerrarSesion.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        btnCerrarSesion.setBorderPainted(false);
        btnCerrarSesion.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setLabel("Cerrar Sesión");
        btnCerrarSesion.setPreferredSize(new java.awt.Dimension(130, 36));
        btnCerrarSesion.addActionListener(this::btnCerrarSesionActionPerformed);
        headerBlanco.add(btnCerrarSesion, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 40, -1, -1));

        panelContenedor.setBackground(new java.awt.Color(245, 245, 245));
        panelContenedor.setPreferredSize(new java.awt.Dimension(1030, 650));
        panelContenedor.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tarjeta1.setBackground(new java.awt.Color(255, 255, 255));
        tarjeta1.setPreferredSize(new java.awt.Dimension(300, 140));

        lbReservacionesTitulo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbReservacionesTitulo.setForeground(new java.awt.Color(102, 102, 102));
        lbReservacionesTitulo.setText("Reservaciones para hoy");

        lbReservaciones.setFont(new java.awt.Font("Arial", 1, 55)); // NOI18N
        lbReservaciones.setForeground(new java.awt.Color(8, 173, 141));
        lbReservaciones.setText("0");

        javax.swing.GroupLayout tarjeta1Layout = new javax.swing.GroupLayout(tarjeta1);
        tarjeta1.setLayout(tarjeta1Layout);
        tarjeta1Layout.setHorizontalGroup(
            tarjeta1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tarjeta1Layout.createSequentialGroup()
                .addGroup(tarjeta1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(tarjeta1Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(lbReservacionesTitulo))
                    .addGroup(tarjeta1Layout.createSequentialGroup()
                        .addGap(131, 131, 131)
                        .addComponent(lbReservaciones)))
                .addContainerGap(113, Short.MAX_VALUE))
        );
        tarjeta1Layout.setVerticalGroup(
            tarjeta1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tarjeta1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(lbReservacionesTitulo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                .addComponent(lbReservaciones)
                .addGap(24, 24, 24))
        );

        panelContenedor.add(tarjeta1, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 40, -1, -1));

        tarjeta2.setBackground(new java.awt.Color(255, 255, 255));
        tarjeta2.setForeground(new java.awt.Color(255, 255, 255));
        tarjeta2.setPreferredSize(new java.awt.Dimension(300, 140));

        lbEquipos.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbEquipos.setForeground(new java.awt.Color(102, 102, 102));
        lbEquipos.setText("Equipos con fallas");

        lbNumFallaEquipos.setFont(new java.awt.Font("Arial", 1, 55)); // NOI18N
        lbNumFallaEquipos.setForeground(new java.awt.Color(220, 83, 86));
        lbNumFallaEquipos.setText("0");

        javax.swing.GroupLayout tarjeta2Layout = new javax.swing.GroupLayout(tarjeta2);
        tarjeta2.setLayout(tarjeta2Layout);
        tarjeta2Layout.setHorizontalGroup(
            tarjeta2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tarjeta2Layout.createSequentialGroup()
                .addGroup(tarjeta2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(tarjeta2Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(lbEquipos))
                    .addGroup(tarjeta2Layout.createSequentialGroup()
                        .addGap(131, 131, 131)
                        .addComponent(lbNumFallaEquipos)))
                .addContainerGap(139, Short.MAX_VALUE))
        );
        tarjeta2Layout.setVerticalGroup(
            tarjeta2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tarjeta2Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(lbEquipos)
                .addGap(19, 19, 19)
                .addComponent(lbNumFallaEquipos)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        panelContenedor.add(tarjeta2, new org.netbeans.lib.awtextra.AbsoluteConstraints(365, 40, -1, -1));

        tarjeta3.setBackground(new java.awt.Color(255, 255, 255));
        tarjeta3.setPreferredSize(new java.awt.Dimension(300, 140));

        lbMantPendientesTitulo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbMantPendientesTitulo.setForeground(new java.awt.Color(102, 102, 102));
        lbMantPendientesTitulo.setText("Mantenimientos pendientes");

        lbMantPendientes.setFont(new java.awt.Font("Arial", 1, 55)); // NOI18N
        lbMantPendientes.setForeground(new java.awt.Color(8, 173, 141));
        lbMantPendientes.setText("0");

        javax.swing.GroupLayout tarjeta3Layout = new javax.swing.GroupLayout(tarjeta3);
        tarjeta3.setLayout(tarjeta3Layout);
        tarjeta3Layout.setHorizontalGroup(
            tarjeta3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tarjeta3Layout.createSequentialGroup()
                .addGroup(tarjeta3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(tarjeta3Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(lbMantPendientesTitulo))
                    .addGroup(tarjeta3Layout.createSequentialGroup()
                        .addGap(131, 131, 131)
                        .addComponent(lbMantPendientes)))
                .addContainerGap(89, Short.MAX_VALUE))
        );
        tarjeta3Layout.setVerticalGroup(
            tarjeta3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tarjeta3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(lbMantPendientesTitulo)
                .addGap(19, 19, 19)
                .addComponent(lbMantPendientes)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        panelContenedor.add(tarjeta3, new org.netbeans.lib.awtextra.AbsoluteConstraints(695, 40, -1, -1));

        panelTabla.setBackground(new java.awt.Color(255, 255, 255));
        panelTabla.setPreferredSize(new java.awt.Dimension(960, 400));
        panelTabla.setVerifyInputWhenFocusTarget(false);

        tablaBitacora.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        tablaBitacora.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Hora", "Usuario", "Rol", "Laboratorio", "Actividad", "Estado"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaBitacora.setRowHeight(34);
        jScrollPane2.setViewportView(tablaBitacora);

        javax.swing.GroupLayout panelTablaLayout = new javax.swing.GroupLayout(panelTabla);
        panelTabla.setLayout(panelTablaLayout);
        panelTablaLayout.setHorizontalGroup(
            panelTablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 960, Short.MAX_VALUE)
        );
        panelTablaLayout.setVerticalGroup(
            panelTablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
        );

        panelContenedor.add(panelTabla, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 260, -1, 160));

        lbBitacora.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbBitacora.setForeground(new java.awt.Color(102, 102, 102));
        lbBitacora.setText("Últimos registros de bitácora");
        panelContenedor.add(lbBitacora, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 230, -1, -1));

        lbUltimoMants.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbUltimoMants.setForeground(new java.awt.Color(102, 102, 102));
        lbUltimoMants.setText("Ultimos mantenimientos");
        panelContenedor.add(lbUltimoMants, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 450, -1, -1));

        panelLabs.setBackground(new java.awt.Color(255, 255, 255));
        panelLabs.setPreferredSize(new java.awt.Dimension(960, 50));

        tablaUltimosMants.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        tablaUltimosMants.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Fecha", "Código Equipo", "Nombre Equipo", "Laboratorio", "Tipo", "Estado", "Responsable"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaUltimosMants.setRowHeight(27);
        jScrollPane1.setViewportView(tablaUltimosMants);

        javax.swing.GroupLayout panelLabsLayout = new javax.swing.GroupLayout(panelLabs);
        panelLabs.setLayout(panelLabsLayout);
        panelLabsLayout.setHorizontalGroup(
            panelLabsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 960, Short.MAX_VALUE)
        );
        panelLabsLayout.setVerticalGroup(
            panelLabsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );

        panelContenedor.add(panelLabs, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 480, -1, 130));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sidebarVerde, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelContenedor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(headerBlanco, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(headerBlanco, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelContenedor, javax.swing.GroupLayout.PREFERRED_SIZE, 625, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(sidebarVerde, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // BOTÓN DE CERRAR SESIÓN
    private void btnCerrarSesionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarSesionActionPerformed
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
        Reserva ventanaReserva = new Reserva(nombreUsuario);
        
        ventanaReserva.setVisible(true);
        ventanaReserva.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReservasActionPerformed

    private void btnBitacoraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBitacoraActionPerformed
        Bitacora ventanaBitacora = new Bitacora(nombreUsuario);
        
        ventanaBitacora.setVisible(true);
        ventanaBitacora.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnBitacoraActionPerformed

    private void btnInventarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInventarioActionPerformed
        Inventario ventanaInventario = new Inventario(nombreUsuario);
        
        ventanaInventario.setVisible(true);
        ventanaInventario.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnInventarioActionPerformed

    private void btnMantActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMantActionPerformed
        Mantenimiento ventanaMant = new Mantenimiento(nombreUsuario);
        
        ventanaMant.setVisible(true);
        ventanaMant.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnMantActionPerformed

    private void btnReporteFallasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReporteFallasActionPerformed
        ReporteFalla ventanaReporte = new ReporteFalla(nombreUsuario);
        
        ventanaReporte.setVisible(true);
        ventanaReporte.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReporteFallasActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new DashboardLabo().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBitacora;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnInventario;
    private javax.swing.JButton btnMant;
    private javax.swing.JButton btnReporteFallas;
    private javax.swing.JButton btnReservas;
    private javax.swing.JPanel headerBlanco;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JLabel imgUTJ;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lbBitacora;
    private javax.swing.JLabel lbEquipos;
    private javax.swing.JLabel lbMantPendientes;
    private javax.swing.JLabel lbMantPendientesTitulo;
    private javax.swing.JLabel lbNombreUsuario;
    private javax.swing.JLabel lbNumFallaEquipos;
    private javax.swing.JLabel lbReservaciones;
    private javax.swing.JLabel lbReservacionesTitulo;
    private javax.swing.JLabel lbUltimoMants;
    private javax.swing.JPanel panelContenedor;
    private javax.swing.JPanel panelLabs;
    private javax.swing.JPanel panelTabla;
    private javax.swing.JPanel sidebarVerde;
    private javax.swing.JTable tablaBitacora;
    private javax.swing.JTable tablaUltimosMants;
    private javax.swing.JPanel tarjeta1;
    private javax.swing.JPanel tarjeta2;
    private javax.swing.JPanel tarjeta3;
    // End of variables declaration//GEN-END:variables
}
