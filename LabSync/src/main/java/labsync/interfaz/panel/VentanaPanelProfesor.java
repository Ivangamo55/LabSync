package labsync.interfaz.panel;

import labsync.interfaz.comun.ActualizacionAutomatica;
import labsync.aplicacion.AplicacionLabSync;
import labsync.configuracion.ConexionBaseDatos;
import labsync.persistencia.ConsultaEscalar;
import labsync.persistencia.ConsultaTabla;
import labsync.interfaz.comun.Recursos;
import labsync.interfaz.comun.ControlNotificacionesReserva;
import labsync.modelo.SesionUsuario;
import labsync.interfaz.bitacora.VentanaBitacoraProfesor;
import labsync.interfaz.reservas.VentanaGestionReservas;
import labsync.interfaz.autenticacion.VentanaInicioSesion;
import labsync.interfaz.reservas.VentanaMisReservasProfesor;
import labsync.interfaz.fallas.VentanaReporteFallaProfesor;
import labsync.interfaz.reservas.VentanaReservasProfesor;

/**
 * Interfaz del usuario Profesor para AplicacionLabSync.
 * Archivo JFrame Form compatible con el diseñador visual de NetBeans.
 * La sección initComponents se mantiene sincronizada con VentanaPanelProfesor.form.
 */
public class VentanaPanelProfesor extends javax.swing.JFrame {

    private String nombreUsuario;
    private SesionUsuario sesion;

    public VentanaPanelProfesor() {
        this("Profesor");
    }

    public VentanaPanelProfesor(String nombreRecibido) {
        this(SesionUsuario.buscarProfesor(nombreRecibido));
    }

    public VentanaPanelProfesor(SesionUsuario sesionRecibida) {
        initComponents();
        sesion = sesionRecibida == null ? SesionUsuario.buscarProfesor("Profesor") : sesionRecibida;
        nombreUsuario = sesion.getNombre();
        lbNombreUsuario.setText("Hola, " + nombreUsuario);
        java.net.URL icono = getClass().getResource("/images/logo_labsync_no_background.png");
        if (icono != null) setIconImage(new javax.swing.ImageIcon(icono).getImage());
        configurarNavegacion();
        configurarPantalla();
        new ControlNotificacionesReserva(this, header, sesion.getId(),
                sesion.getNombreCompleto(), "Profesor",
                () -> abrirVentana(new VentanaMisReservasProfesor(sesion)));
        iniciarActualizacionAutomatica();
        setLocationRelativeTo(null);
    }

    private record ResumenProfesor(String activas, String proxima, String laboratorio,
            String reportes, javax.swing.table.DefaultTableModel reservas) { }

    private void iniciarActualizacionAutomatica() {
        new ActualizacionAutomatica<>(this, 7_000, this::consultarResumenProfesor, datos -> {
            lbValorReservas.setText(datos.activas()); lbValorProxima.setText(datos.proxima());
            lbDescProxima.setText(datos.laboratorio()); lbValorReportes.setText(datos.reportes());
            tablaReservaciones.setModel(datos.reservas()); ocultarPrimeraColumna(tablaReservaciones);
        });
    }

    private ResumenProfesor consultarResumenProfesor() {
        ConsultaTabla.Parametros usuario = ps -> { ps.setInt(1, sesion.getId()); ps.setString(2, sesion.getNombreCompleto()); };
        String filtro = "(id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ?)) AND rol_solicitante = 'Profesor'";
        Object activas = ConsultaEscalar.ejecutar("SELECT COUNT(*) total FROM reservas WHERE " + filtro + " AND estado IN ('Pendiente','Aprobada') AND fecha >= CURDATE()", "total", usuario);
        Object proxima = ConsultaEscalar.ejecutar("SELECT DATE_FORMAT(MIN(fecha),'%d/%m/%Y') proxima FROM reservas WHERE " + filtro + " AND estado IN ('Pendiente','Aprobada') AND fecha >= CURDATE()", "proxima", usuario);
        Object laboratorio = ConsultaEscalar.ejecutar("SELECT laboratorio FROM reservas WHERE " + filtro + " AND estado IN ('Pendiente','Aprobada') AND fecha >= CURDATE() ORDER BY fecha, horario LIMIT 1", "laboratorio", usuario);
        ConsultaTabla.Parametros reporte = ps -> { ps.setInt(1, sesion.getId()); ps.setString(2, sesion.getNombreCompleto()); };
        Object reportes = ConsultaEscalar.ejecutar("SELECT COUNT(*) total FROM reporte_fallas WHERE (id_usuario = ? OR (id_usuario IS NULL AND reportado_por = ?)) AND rol_reportante = 'Profesor' AND estado IN ('Pendiente', 'En revisión')", "total", reporte);
        javax.swing.table.DefaultTableModel tabla = ConsultaTabla.ejecutar("SELECT id_reserva, DATE_FORMAT(fecha, '%d/%m/%Y') fecha, horario, laboratorio, CONCAT(grado, grupo) grupo, actividad, estado FROM reservas WHERE " + filtro + " AND fecha >= CURDATE() AND estado IN ('Pendiente','Aprobada') ORDER BY fecha, horario LIMIT 5", new String[]{"ID","Fecha","Horario","Laboratorio","Grupo","Actividad","Estado"}, new String[]{"id_reserva","fecha","horario","laboratorio","grupo","actividad","estado"}, usuario);
        return new ResumenProfesor(String.valueOf(activas == null ? "-" : activas), proxima == null ? "Sin reservas" : proxima.toString(), laboratorio == null ? "No tienes reservas próximas" : laboratorio.toString(), String.valueOf(reportes == null ? "-" : reportes), tabla);
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
        String sql = "SELECT nombre FROM laboratorios WHERE estado = 'Disponible' AND nombre NOT IN ('PB-05', 'M-19') ORDER BY nombre";
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
        tablaReservaciones.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"ID", "Fecha", "Horario", "Laboratorio", "Grupo", "Actividad", "Estado"}) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        tablaReservaciones.setRowHeight(32);
        ocultarPrimeraColumna(tablaReservaciones);
        lbVerTodas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lbVerTodas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent evt) {
                abrirVentana(new VentanaMisReservasProfesor(sesion));
            }
        });
        cargarResumen();
        cargarReservasProximas();
    }

    private void cargarResumen() {
        String filtro = "(id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ?)) "
                + "AND rol_solicitante = 'Profesor'";
        try (java.sql.Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) return;
            String sqlReservas = "SELECT SUM(CASE WHEN estado IN ('Pendiente','Aprobada') AND fecha >= CURDATE() THEN 1 ELSE 0 END) activas, "
                    + "MIN(CASE WHEN estado IN ('Pendiente','Aprobada') AND fecha >= CURDATE() THEN fecha END) proxima FROM reservas WHERE " + filtro;
            try (java.sql.PreparedStatement ps = con.prepareStatement(sqlReservas)) {
                ps.setInt(1, sesion.getId());
                ps.setString(2, sesion.getNombreCompleto());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lbValorReservas.setText(String.valueOf(rs.getInt("activas")));
                        java.sql.Date proxima = rs.getDate("proxima");
                        lbValorProxima.setText(proxima == null ? "Sin reservas" : new java.text.SimpleDateFormat("dd/MM/yyyy").format(proxima));
                    }
                }
            }
            String sqlDetalle = "SELECT laboratorio FROM reservas WHERE " + filtro
                    + " AND estado IN ('Pendiente','Aprobada') AND fecha >= CURDATE() ORDER BY fecha, horario LIMIT 1";
            try (java.sql.PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                ps.setInt(1, sesion.getId());
                ps.setString(2, sesion.getNombreCompleto());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    lbDescProxima.setText(rs.next() ? rs.getString("laboratorio") : "No tienes reservas próximas");
                }
            }
            String sqlReportes = "SELECT COUNT(*) total FROM reporte_fallas "
                    + "WHERE (id_usuario = ? OR (id_usuario IS NULL AND reportado_por = ?)) "
                    + "AND rol_reportante = 'Profesor' AND estado IN ('Pendiente', 'En revisión')";
            try (java.sql.PreparedStatement ps = con.prepareStatement(sqlReportes)) {
                ps.setInt(1, sesion.getId());
                ps.setString(2, sesion.getNombreCompleto());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) lbValorReportes.setText(String.valueOf(rs.getInt("total")));
                }
            }
        } catch (java.sql.SQLException ex) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "No se cargó el resumen", ex);
        }
    }

    private void cargarReservasProximas() {
        javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) tablaReservaciones.getModel();
        modelo.setRowCount(0);
        String sql = "SELECT id_reserva, DATE_FORMAT(fecha, '%d/%m/%Y') fecha, horario, laboratorio, "
                + "CONCAT(grado, grupo) grupo, actividad, estado FROM reservas "
                + "WHERE (id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ?)) "
                + "AND rol_solicitante = 'Profesor' "
                + "AND fecha >= CURDATE() AND estado IN ('Pendiente','Aprobada') ORDER BY fecha, horario LIMIT 5";
        try (java.sql.Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) return;
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, sesion.getId());
                ps.setString(2, sesion.getNombreCompleto());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) modelo.addRow(new Object[]{rs.getInt("id_reserva"), rs.getString("fecha"), rs.getString("horario"), rs.getString("laboratorio"), rs.getString("grupo"), rs.getString("actividad"), rs.getString("estado")});
                }
            }
        } catch (java.sql.SQLException ex) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "No se cargaron las reservas", ex);
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
        tarjetaReservas = new javax.swing.JPanel();
        lbTituloReservas = new javax.swing.JLabel();
        lbValorReservas = new javax.swing.JLabel();
        lbDescReservas = new javax.swing.JLabel();
        tarjetaProxima = new javax.swing.JPanel();
        lbTituloProxima = new javax.swing.JLabel();
        lbValorProxima = new javax.swing.JLabel();
        lbDescProxima = new javax.swing.JLabel();
        tarjetaReportes = new javax.swing.JPanel();
        lbTituloReportes = new javax.swing.JLabel();
        lbValorReportes = new javax.swing.JLabel();
        lbDescReportes = new javax.swing.JLabel();
        panelReservaciones = new javax.swing.JPanel();
        lbTituloReservaciones = new javax.swing.JLabel();
        scrollReservaciones = new javax.swing.JScrollPane();
        tablaReservaciones = new javax.swing.JTable();
        lbVerTodas = new javax.swing.JLabel();
        panelInformacion = new javax.swing.JPanel();
        lbTituloInformacion = new javax.swing.JLabel();
        lbInfo1Titulo = new javax.swing.JLabel();
        lbInfo1Texto = new javax.swing.JLabel();
        lbInfo2Titulo = new javax.swing.JLabel();
        lbInfo2Texto = new javax.swing.JLabel();
        lbInfo3Titulo = new javax.swing.JLabel();
        lbInfo3Texto = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LabSync - Panel de Profesor");
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
        lbTituloPantalla.setText("Panel del profesor");
        body.add(lbTituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 15, 420, 30));
        lbSubtituloPantalla.setFont(new java.awt.Font("Arial", 0, 13));
        lbSubtituloPantalla.setForeground(new java.awt.Color(100, 100, 100));
        lbSubtituloPantalla.setText("Consulta tus reservas, registros de bitácora y reportes.");
        body.add(lbSubtituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 45, 650, 22));
        tarjetaReservas.setBackground(new java.awt.Color(255, 255, 255));
        tarjetaReservas.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        tarjetaReservas.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloReservas.setFont(new java.awt.Font("Arial", 1, 15));
        lbTituloReservas.setForeground(new java.awt.Color(70, 70, 70));
        lbTituloReservas.setText("Reservas activas");
        tarjetaReservas.add(lbTituloReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, 220, 24));
        lbValorReservas.setFont(new java.awt.Font("Arial", 1, 42));
        lbValorReservas.setForeground(new java.awt.Color(8, 173, 141));
        lbValorReservas.setText("0");
        lbValorReservas.setHorizontalAlignment(0);
        tarjetaReservas.add(lbValorReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(105, 47, 90, 50));
        lbDescReservas.setFont(new java.awt.Font("Arial", 0, 12));
        lbDescReservas.setForeground(new java.awt.Color(90, 90, 90));
        lbDescReservas.setText("Pendientes o aprobadas");
        lbDescReservas.setHorizontalAlignment(0);
        tarjetaReservas.add(lbDescReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 100, 230, 20));
        body.add(tarjetaReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 80, 300, 130));
        tarjetaProxima.setBackground(new java.awt.Color(255, 255, 255));
        tarjetaProxima.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        tarjetaProxima.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloProxima.setFont(new java.awt.Font("Arial", 1, 15));
        lbTituloProxima.setForeground(new java.awt.Color(70, 70, 70));
        lbTituloProxima.setText("Próxima reserva");
        tarjetaProxima.add(lbTituloProxima, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, 220, 24));
        lbValorProxima.setFont(new java.awt.Font("Arial", 1, 25));
        lbValorProxima.setForeground(new java.awt.Color(220, 53, 69));
        lbValorProxima.setText("Sin reservas");
        lbValorProxima.setHorizontalAlignment(0);
        tarjetaProxima.add(lbValorProxima, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 52, 250, 38));
        lbDescProxima.setFont(new java.awt.Font("Arial", 0, 12));
        lbDescProxima.setForeground(new java.awt.Color(90, 90, 90));
        lbDescProxima.setText("No tienes reservas próximas");
        lbDescProxima.setHorizontalAlignment(0);
        tarjetaProxima.add(lbDescProxima, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 100, 250, 20));
        body.add(tarjetaProxima, new org.netbeans.lib.awtextra.AbsoluteConstraints(365, 80, 300, 130));
        tarjetaReportes.setBackground(new java.awt.Color(255, 255, 255));
        tarjetaReportes.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        tarjetaReportes.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloReportes.setFont(new java.awt.Font("Arial", 1, 15));
        lbTituloReportes.setForeground(new java.awt.Color(70, 70, 70));
        lbTituloReportes.setText("Reportes pendientes");
        tarjetaReportes.add(lbTituloReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, 230, 24));
        lbValorReportes.setFont(new java.awt.Font("Arial", 1, 42));
        lbValorReportes.setForeground(new java.awt.Color(8, 173, 141));
        lbValorReportes.setText("0");
        lbValorReportes.setHorizontalAlignment(0);
        tarjetaReportes.add(lbValorReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(105, 47, 90, 50));
        lbDescReportes.setFont(new java.awt.Font("Arial", 0, 12));
        lbDescReportes.setForeground(new java.awt.Color(90, 90, 90));
        lbDescReportes.setText("En revisión por laboratorista");
        lbDescReportes.setHorizontalAlignment(0);
        tarjetaReportes.add(lbDescReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 100, 240, 20));
        body.add(tarjetaReportes, new org.netbeans.lib.awtextra.AbsoluteConstraints(695, 80, 300, 130));
        panelReservaciones.setBackground(new java.awt.Color(255, 255, 255));
        panelReservaciones.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelReservaciones.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloReservaciones.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloReservaciones.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloReservaciones.setText("Próximas reservaciones");
        panelReservaciones.add(lbTituloReservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 16, 310, 28));
        tablaReservaciones.setFont(new java.awt.Font("Arial", 0, 12));
        tablaReservaciones.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][] {
                        
                    },
                    new String[] {"ID", "Fecha", "Horario", "Laboratorio", "Grupo", "Actividad", "Estado"}
                ) {
                    boolean[] canEdit = new boolean[] {false, false, false, false, false, false, false};
                    @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return canEdit[columnIndex]; }
                });
        tablaReservaciones.setRowHeight(32);
        tablaReservaciones.setSelectionBackground(new java.awt.Color(224, 247, 241));
        tablaReservaciones.setShowVerticalLines(false);
        scrollReservaciones.setViewportView(tablaReservaciones);
        panelReservaciones.add(scrollReservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 58, 570, 215));
        lbVerTodas.setFont(new java.awt.Font("Arial", 1, 13));
        lbVerTodas.setForeground(new java.awt.Color(6, 140, 115));
        lbVerTodas.setText("Ver todas mis reservas  >");
        lbVerTodas.setHorizontalAlignment(4);
        panelReservaciones.add(lbVerTodas, new org.netbeans.lib.awtextra.AbsoluteConstraints(365, 290, 230, 25));
        body.add(panelReservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 235, 620, 340));
        panelInformacion.setBackground(new java.awt.Color(255, 255, 255));
        panelInformacion.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelInformacion.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloInformacion.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloInformacion.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloInformacion.setText("Guía del profesor");
        panelInformacion.add(lbTituloInformacion, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 16, 250, 28));
        lbInfo1Titulo.setFont(new java.awt.Font("Arial", 1, 13));
        lbInfo1Titulo.setForeground(new java.awt.Color(70, 70, 70));
        lbInfo1Titulo.setText("1. Reserva con anticipación");
        panelInformacion.add(lbInfo1Titulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 65, 250, 20));
        lbInfo1Texto.setFont(new java.awt.Font("Arial", 0, 12));
        lbInfo1Texto.setForeground(new java.awt.Color(90, 90, 90));
        lbInfo1Texto.setText("<html>Selecciona fecha, horario y laboratorio; después envía la solicitud.</html>");
        panelInformacion.add(lbInfo1Texto, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 88, 250, 52));
        lbInfo2Titulo.setFont(new java.awt.Font("Arial", 1, 13));
        lbInfo2Titulo.setForeground(new java.awt.Color(70, 70, 70));
        lbInfo2Titulo.setText("2. Consulta el estado");
        panelInformacion.add(lbInfo2Titulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 160, 250, 20));
        lbInfo2Texto.setFont(new java.awt.Font("Arial", 0, 12));
        lbInfo2Texto.setForeground(new java.awt.Color(90, 90, 90));
        lbInfo2Texto.setText("<html>Revisa en Mis reservas si la solicitud fue aprobada, rechazada o cancelada.</html>");
        panelInformacion.add(lbInfo2Texto, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 183, 250, 55));
        lbInfo3Titulo.setFont(new java.awt.Font("Arial", 1, 13));
        lbInfo3Titulo.setForeground(new java.awt.Color(70, 70, 70));
        lbInfo3Titulo.setText("3. Registra en bitácora");
        panelInformacion.add(lbInfo3Titulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 255, 250, 20));
        lbInfo3Texto.setFont(new java.awt.Font("Arial", 0, 12));
        lbInfo3Texto.setForeground(new java.awt.Color(90, 90, 90));
        lbInfo3Texto.setText("<html>Después de usar el laboratorio, captura los datos reales de la clase.</html>");
        panelInformacion.add(lbInfo3Texto, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 278, 250, 45));
        body.add(panelInformacion, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 235, 305, 340));

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
            java.util.logging.Logger.getLogger(VentanaPanelProfesor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new VentanaPanelProfesor("Profesor").setVisible(true));
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
    private javax.swing.JPanel tarjetaReservas;
    private javax.swing.JLabel lbTituloReservas;
    private javax.swing.JLabel lbValorReservas;
    private javax.swing.JLabel lbDescReservas;
    private javax.swing.JPanel tarjetaProxima;
    private javax.swing.JLabel lbTituloProxima;
    private javax.swing.JLabel lbValorProxima;
    private javax.swing.JLabel lbDescProxima;
    private javax.swing.JPanel tarjetaReportes;
    private javax.swing.JLabel lbTituloReportes;
    private javax.swing.JLabel lbValorReportes;
    private javax.swing.JLabel lbDescReportes;
    private javax.swing.JPanel panelReservaciones;
    private javax.swing.JLabel lbTituloReservaciones;
    private javax.swing.JScrollPane scrollReservaciones;
    private javax.swing.JTable tablaReservaciones;
    private javax.swing.JLabel lbVerTodas;
    private javax.swing.JPanel panelInformacion;
    private javax.swing.JLabel lbTituloInformacion;
    private javax.swing.JLabel lbInfo1Titulo;
    private javax.swing.JLabel lbInfo1Texto;
    private javax.swing.JLabel lbInfo2Titulo;
    private javax.swing.JLabel lbInfo2Texto;
    private javax.swing.JLabel lbInfo3Titulo;
    private javax.swing.JLabel lbInfo3Texto;
    // End of variables declaration//GEN-END:variables
}
