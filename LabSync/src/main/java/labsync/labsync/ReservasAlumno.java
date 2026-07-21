package labsync.labsync;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.text.SimpleDateFormat;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 * Diseño de la interfaz ReservasAlumno.
 * JFrame Form editable desde NetBeans Swing Designer.
 * Mantiene la maquetación visual del proyecto LabSync.
 */
public class ReservasAlumno extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ReservasAlumno.class.getName());
    private String nombreUsuario;

    public ReservasAlumno() {
        this("Usuario");
    }

    public ReservasAlumno(String nombreRecibido) {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        this.nombreUsuario = nombreRecibido == null || nombreRecibido.isBlank() ? "Usuario" : nombreRecibido;
        lbNombreUsuario.setText("Hola, " + nombreUsuario);
        configurarDateChooser();
        configurarFormulario();
        setLocationRelativeTo(null);
    }

    private void configurarDateChooser() {
        dateFechaReserva.setDateFormatString("yyyy-MM-dd");
        dateFechaReserva.setBackground(Color.WHITE);
        dateFechaReserva.setForeground(new Color(51, 51, 51));
        dateFechaReserva.setFont(new Font("Arial", Font.PLAIN, 12));
        dateFechaReserva.setBorder(BorderFactory.createLineBorder(new Color(102, 102, 102)));

        Component editor = dateFechaReserva.getDateEditor().getUiComponent();
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

        dateFechaReserva.getCalendarButton().setBackground(Color.WHITE);
        dateFechaReserva.getCalendarButton().setForeground(new Color(51, 51, 51));
    }


    private void configurarCursorBotones() {
        java.awt.Cursor cursorManita = new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR);

        btnReservas.setCursor(cursorManita);
        btnMisReservas.setCursor(cursorManita);
        btnReporteFallas.setCursor(cursorManita);
        btnCerrarSesion.setCursor(cursorManita);
        btnBuscarDisponibilidad.setCursor(cursorManita);
        btnSolicitarReserva.setCursor(cursorManita);
    }

    private void configurarFormulario() {
        dateFechaReserva.setMinSelectableDate(Date.valueOf(LocalDate.now()));
        ValidacionFechas.bloquearFinesDeSemana(dateFechaReserva);
        dateFechaReserva.setDate(ValidacionFechas.siguienteDiaHabil(new java.util.Date()));
        txtResumenLaboratorio.setText("");
        txtResumenFecha.setText("");
        txtResumenHorario.setText("");
        txtActividad.setText("");
        txtActividad.setToolTipText("Ej. practica, proyecto o exposicion");
        btnSolicitarReserva.setEnabled(false);
        btnReservas.addActionListener(this::btnReservasActionPerformed);
        btnMisReservas.addActionListener(this::btnMisReservasActionPerformed);
        btnReporteFallas.addActionListener(event -> abrirReporteFallas());

        configurarCursorBotones();

        tablaDisponibilidad.setModel(new DefaultTableModel(
            new Object[][]{},
            new String[]{"Laboratorio", "Horario", "Disponibles", "Estado", "Accion"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        tablaDisponibilidad.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                seleccionarLaboratorioDisponible();
            }
        });
        cmbHorario.addActionListener(event -> limpiarResumen());
        cmbLaboratorio.addActionListener(event -> limpiarResumen());
        dateFechaReserva.addPropertyChangeListener("date", event -> limpiarResumen());

        cargarLaboratoriosDesdeBD();
    }

    private boolean validarBusqueda() {
        if (dateFechaReserva.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una fecha.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        LocalDate fecha = new Date(dateFechaReserva.getDate().getTime()).toLocalDate();
        if (fecha.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "La fecha de la reserva no puede estar en el pasado.", "Fecha no valida", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (!ValidacionFechas.validarDiaHabil(this, dateFechaReserva.getDate(), "realizar reservas")) return false;
        if (cmbHorario.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un horario.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void cargarLaboratoriosDesdeBD() {
        cmbLaboratorio.removeAllItems();
        cmbLaboratorio.addItem("Selecciona laboratorio");

        String sql = "SELECT nombre "
            + "FROM laboratorios "
            + "WHERE estado = 'Disponible' "
            + "ORDER BY nombre ASC";

        try (Connection con = ConexionBD.conectar()) {
            if (con == null) {
                mostrarErrorConexion();
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    cmbLaboratorio.addItem(rs.getString("nombre"));
                }
            }

        } catch (SQLException ex) {
            mostrarErrorSQL("No se pudieron cargar los laboratorios", ex);
        }
    }

    private void buscarDisponibilidad() {
        if (!validarBusqueda()) {
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) tablaDisponibilidad.getModel();
        modelo.setRowCount(0);
        limpiarResumen();

        Date fecha = new Date(dateFechaReserva.getDate().getTime());
        String horario = cmbHorario.getSelectedItem().toString();

        String laboratorioSeleccionado = cmbLaboratorio.getSelectedIndex() > 0
            ? cmbLaboratorio.getSelectedItem().toString()
            : "";

        String sqlLaboratorios = "SELECT nombre, total_equipos "
            + "FROM laboratorios "
            + "WHERE estado = 'Disponible' ";

        java.util.ArrayList<String> parametrosLaboratorios = new java.util.ArrayList<>();

        if (!laboratorioSeleccionado.isEmpty()) {
            sqlLaboratorios += "AND nombre = ? ";
            parametrosLaboratorios.add(laboratorioSeleccionado);
        }

        sqlLaboratorios += "ORDER BY nombre ASC";

        String sqlReservas = "SELECT COUNT(*) AS total "
            + "FROM reservas "
            + "WHERE laboratorio = ? "
            + "AND fecha = ? "
            + "AND horario = ? "
            + "AND estado IN ('Pendiente', 'Aprobada')";

        try (Connection con = ConexionBD.conectar()) {
            if (con == null) {
                mostrarErrorConexion();
                return;
            }

            try (PreparedStatement psLaboratorios = con.prepareStatement(sqlLaboratorios)) {
                for (int i = 0; i < parametrosLaboratorios.size(); i++) {
                    psLaboratorios.setString(i + 1, parametrosLaboratorios.get(i));
                }

                try (ResultSet rsLaboratorios = psLaboratorios.executeQuery()) {
                    while (rsLaboratorios.next()) {
                        String laboratorioActual = rsLaboratorios.getString("nombre");
                        int totalEquipos = rsLaboratorios.getInt("total_equipos");
                        int reservasActivas = 0;

                        try (PreparedStatement psReservas = con.prepareStatement(sqlReservas)) {
                            psReservas.setString(1, laboratorioActual);
                            psReservas.setDate(2, fecha);
                            psReservas.setString(3, horario);

                            try (ResultSet rsReservas = psReservas.executeQuery()) {
                                if (rsReservas.next()) {
                                    reservasActivas = rsReservas.getInt("total");
                                }
                            }
                        }

                        int equiposDisponibles = totalEquipos - reservasActivas;

                        if (equiposDisponibles < 0) {
                            equiposDisponibles = 0;
                        }

                        String estado = equiposDisponibles > 0 ? "Disponible" : "Ocupado";

                        modelo.addRow(new Object[]{
                            laboratorioActual,
                            horario,
                            equiposDisponibles,
                            estado,
                            equiposDisponibles > 0 ? "Seleccionar" : "--"
                        });
                    }
                }
            }

            if (modelo.getRowCount() == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "No se encontraron laboratorios con ese filtro.",
                    "Sin resultados",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (SQLException ex) {
            mostrarErrorSQL("No se pudo consultar la disponibilidad", ex);
        }
    }

    private int capacidadLaboratorio(String laboratorio) {
        return laboratorio.startsWith("5-") ? 25 : 30;
    }

    private void seleccionarLaboratorioDisponible() {
        int fila = tablaDisponibilidad.getSelectedRow();
        if (fila < 0) {
            return;
        }
        int modeloFila = tablaDisponibilidad.convertRowIndexToModel(fila);
        if (!"Disponible".equals(tablaDisponibilidad.getModel().getValueAt(modeloFila, 3))) {
            limpiarResumen();
            return;
        }
        txtResumenLaboratorio.setText(tablaDisponibilidad.getModel().getValueAt(modeloFila, 0).toString());
        txtResumenHorario.setText(tablaDisponibilidad.getModel().getValueAt(modeloFila, 1).toString());
        txtResumenFecha.setText(new SimpleDateFormat("yyyy-MM-dd").format(dateFechaReserva.getDate()));
        btnSolicitarReserva.setEnabled(true);
    }


    private boolean usuarioTieneReservaActiva(Connection con, DatosUsuario usuario) throws SQLException {
        String sql = "SELECT id_reserva "
            + "FROM reservas "
            + "WHERE (id_usuario = ? OR (id_usuario IS NULL AND nombre_solicitante = ? AND rol_solicitante = ?)) "
            + "AND estado NOT IN ('Cancelada', 'Finalizada', 'Rechazada') "
            + "LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuario.idUsuario);
            ps.setString(2, usuario.nombreCompleto);
            ps.setString(3, usuario.rol);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void solicitarReserva() {
        String actividad = txtActividad.getText().trim();

        if (txtResumenLaboratorio.getText().isBlank()) {
            JOptionPane.showMessageDialog(
                this,
                "Selecciona un laboratorio disponible de la tabla.",
                "Seleccion requerida",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (actividad.isBlank()) {
            JOptionPane.showMessageDialog(
                this,
                "Escribe la actividad o motivo de la reserva.",
                "Dato requerido",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Connection con = ConexionBD.conectar();

        if (con == null) {
            mostrarErrorConexion();
            return;
        }

        try (con) {
            con.setAutoCommit(false);

            DatosUsuario usuario = obtenerDatosUsuario(con);

            if (usuario == null) {
                throw new SQLException("No se encontro el usuario que inicio sesion.");
            }

            if (usuarioTieneReservaActiva(con, usuario)) {
                con.rollback();

                JOptionPane.showMessageDialog(
                    this,
                    "Ya tienes una reservacion registrada. Solo puedes tener una reservacion a la vez.\n"
                        + "Podras solicitar otra cuando tu reservacion actual este cancelada o terminada.",
                    "Reservacion existente",
                    JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            String laboratorio = txtResumenLaboratorio.getText();
            String horario = txtResumenHorario.getText();
            Date fecha = Date.valueOf(txtResumenFecha.getText());

            String sqlTotalEquipos = "SELECT total_equipos "
                + "FROM laboratorios "
                + "WHERE nombre = ? "
                + "AND estado = 'Disponible' "
                + "FOR UPDATE";

            String sqlReservasActivas = "SELECT id_reserva "
                + "FROM reservas "
                + "WHERE laboratorio = ? "
                + "AND fecha = ? "
                + "AND horario = ? "
                + "AND estado IN ('Pendiente', 'Aprobada') "
                + "FOR UPDATE";

            int totalEquipos = 0;
            int reservasActivas = 0;

            try (PreparedStatement ps = con.prepareStatement(sqlTotalEquipos)) {
                ps.setString(1, laboratorio);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalEquipos = rs.getInt("total_equipos");
                    } else {
                        con.rollback();
                        JOptionPane.showMessageDialog(
                            this,
                            "El laboratorio seleccionado no esta registrado o no esta disponible.",
                            "Laboratorio no disponible",
                            JOptionPane.WARNING_MESSAGE
                        );
                        buscarDisponibilidad();
                        return;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlReservasActivas)) {
                ps.setString(1, laboratorio);
                ps.setDate(2, fecha);
                ps.setString(3, horario);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        reservasActivas++;
                    }
                }
            }

            if (reservasActivas >= totalEquipos) {
                con.rollback();

                JOptionPane.showMessageDialog(
                    this,
                    "Ya no hay equipos disponibles en este laboratorio para ese horario.",
                    "Sin disponibilidad",
                    JOptionPane.WARNING_MESSAGE
                );

                buscarDisponibilidad();
                return;
            }

            String sqlInsert = "INSERT INTO reservas (id_usuario, nombre_solicitante, rol_solicitante, "
                + "laboratorio, actividad, grado, grupo, turno, fecha, horario, cantidad_alumnos, estado, observaciones) "
                + "VALUES (?, ?, ?, ?, ?, 'N/A', 'N/A', ?, ?, ?, 1, 'Pendiente', NULL)";

            try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
                ps.setInt(1, usuario.idUsuario);
                ps.setString(2, usuario.nombreCompleto);
                ps.setString(3, usuario.rol);
                ps.setString(4, laboratorio);
                ps.setString(5, actividad);
                ps.setString(6, usuario.turno);
                ps.setDate(7, fecha);
                ps.setString(8, horario);
                ps.executeUpdate();
            }

            con.commit();

            JOptionPane.showMessageDialog(
                this,
                "La reserva fue enviada y quedo Pendiente de aprobacion.",
                "Reserva registrada",
                JOptionPane.INFORMATION_MESSAGE
            );

            limpiarFormularioDespuesRegistro();

        } catch (SQLException ex) {
            try {
                con.rollback();
            } catch (SQLException ignored) {
            }

            mostrarErrorSQL("No se pudo registrar la reserva", ex);
        }
    }

    private DatosUsuario obtenerDatosUsuario(Connection con) throws SQLException {
        String sql = "SELECT u.id, CONCAT_WS(' ', u.nombre, u.apellido_p, u.apellido_m) AS nombre_completo, "
            + "u.rol, COALESCE(e.turno, 'No especificado') AS turno FROM usuario u "
            + "LEFT JOIN estudiante e ON e.id_usuario = u.id "
            + "WHERE u.nombre = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DatosUsuario(rs.getInt("id"), rs.getString("nombre_completo"), rs.getString("rol"), rs.getString("turno"));
                }
            }
        }
        return null;
    }


    private void limpiarFormularioDespuesRegistro() {
        txtActividad.setText("");

        if (cmbHorario.getItemCount() > 0) {
            cmbHorario.setSelectedIndex(0);
        }

        if (cmbLaboratorio.getItemCount() > 0) {
            cmbLaboratorio.setSelectedIndex(0);
        }

        dateFechaReserva.setDate(ValidacionFechas.siguienteDiaHabil(new java.util.Date()));

        limpiarResumen();
        limpiarTablaDisponibilidad();
    }

    private void limpiarTablaDisponibilidad() {
        DefaultTableModel modelo = (DefaultTableModel) tablaDisponibilidad.getModel();
        modelo.setRowCount(0);
        tablaDisponibilidad.clearSelection();
    }

    private void limpiarResumen() {
        txtResumenLaboratorio.setText("");
        txtResumenFecha.setText("");
        txtResumenHorario.setText("");
        btnSolicitarReserva.setEnabled(false);
    }

    private void abrirReporteFallas() {
        ReporteFallasAlumno reportes = new ReporteFallasAlumno(nombreUsuario);
        reportes.setVisible(true);
        dispose();
    }

    private void mostrarErrorConexion() {
        JOptionPane.showMessageDialog(this, "No fue posible conectarse con la base de datos.", "Error de conexion", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarErrorSQL(String mensaje, SQLException ex) {
        logger.log(java.util.logging.Level.SEVERE, mensaje, ex);
        JOptionPane.showMessageDialog(this, mensaje + ":\n" + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
    }

    private static class DatosUsuario {
        final int idUsuario;
        final String nombreCompleto;
        final String rol;
        final String turno;

        DatosUsuario(int idUsuario, String nombreCompleto, String rol, String turno) {
            this.idUsuario = idUsuario;
            this.nombreCompleto = nombreCompleto;
            this.rol = rol;
            this.turno = turno;
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
        lbFecha = new javax.swing.JLabel();
        dateFechaReserva = new com.toedter.calendar.JDateChooser();
        lbHorario = new javax.swing.JLabel();
        cmbHorario = new javax.swing.JComboBox();
        lbLaboratorio = new javax.swing.JLabel();
        cmbLaboratorio = new javax.swing.JComboBox();
        lbActividad = new javax.swing.JLabel();
        txtActividad = new javax.swing.JTextField();
        btnBuscarDisponibilidad = new javax.swing.JButton();
        panelGuia = new javax.swing.JPanel();
        lbTituloGuia = new javax.swing.JLabel();
        lbGuia1 = new javax.swing.JLabel();
        lbGuia2 = new javax.swing.JLabel();
        lbGuia3 = new javax.swing.JLabel();
        panelResultados = new javax.swing.JPanel();
        lbTituloResultados = new javax.swing.JLabel();
        jScrollPaneTabla = new javax.swing.JScrollPane();
        tablaDisponibilidad = new javax.swing.JTable();
        panelResumen = new javax.swing.JPanel();
        lbTituloResumen = new javax.swing.JLabel();
        lbResumenLaboratorio = new javax.swing.JLabel();
        txtResumenLaboratorio = new javax.swing.JTextField();
        lbResumenFecha = new javax.swing.JLabel();
        txtResumenFecha = new javax.swing.JTextField();
        lbResumenHorario = new javax.swing.JLabel();
        txtResumenHorario = new javax.swing.JTextField();
        btnSolicitarReserva = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Reservas Alumno");
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
        btnCerrarSesion.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        btnCerrarSesion.setBorderPainted(false);
        btnCerrarSesion.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setPreferredSize(new java.awt.Dimension(130, 36));
        btnCerrarSesion.addActionListener(this::btnCerrarSesionActionPerformed);
        headerBlanco.add(btnCerrarSesion, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 40, -1, -1));

        getContentPane().add(headerBlanco, new org.netbeans.lib.awtextra.AbsoluteConstraints(256, 0, 1030, 100));

        panelContenedor.setBackground(new java.awt.Color(245, 245, 245));
        panelContenedor.setPreferredSize(new java.awt.Dimension(1030, 625));
        panelContenedor.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloPantalla.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTituloPantalla.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloPantalla.setText("Solicitar reservación");
        panelContenedor.add(lbTituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 25, -1, -1));

        lbSubtituloPantalla.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        lbSubtituloPantalla.setForeground(new java.awt.Color(102, 102, 102));
        lbSubtituloPantalla.setText("Consulta disponibilidad y solicita un laboratorio para tus actividades académicas.");
        panelContenedor.add(lbSubtituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 58, -1, -1));

        panelFormulario.setBackground(new java.awt.Color(255, 255, 255));
        panelFormulario.setPreferredSize(new java.awt.Dimension(650, 220));
        panelFormulario.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloFormulario.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbTituloFormulario.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloFormulario.setText("Datos de la solicitud");
        panelFormulario.add(lbTituloFormulario, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, -1, -1));

        lbFecha.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbFecha.setForeground(new java.awt.Color(102, 102, 102));
        lbFecha.setText("Fecha");
        panelFormulario.add(lbFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 65, -1, -1));

        dateFechaReserva.setBackground(new java.awt.Color(255, 255, 255));
        dateFechaReserva.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        dateFechaReserva.setForeground(new java.awt.Color(51, 51, 51));
        dateFechaReserva.setDateFormatString("yyyy-MM-dd");
        dateFechaReserva.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        panelFormulario.add(dateFechaReserva, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 90, 175, 30));

        lbHorario.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbHorario.setForeground(new java.awt.Color(102, 102, 102));
        lbHorario.setText("Horario");
        panelFormulario.add(lbHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 65, -1, -1));

        cmbHorario.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        cmbHorario.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Selecciona horario", "7:00 - 7:50", "7:50 - 8:40", "9:10 - 10:00", "10:00 - 10:50", "10:50 - 11:40", "11:40 - 12:30", "12:30 - 13:20", "13:20 - 14:10", "14:10 - 15:00", "15:00 - 15:50", "15:50 - 16:40", "16:40 - 17:30", "18:00 - 18:50", "18:50 - 19:40", "19:40 - 20:30" }));
        panelFormulario.add(cmbHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 90, 175, 30));

        lbLaboratorio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        lbLaboratorio.setText("Laboratorio");
        panelFormulario.add(lbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(415, 65, -1, -1));

        cmbLaboratorio.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        cmbLaboratorio.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Selecciona laboratorio" }));
        panelFormulario.add(cmbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(415, 90, 210, 30));

        lbActividad.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbActividad.setForeground(new java.awt.Color(102, 102, 102));
        lbActividad.setText("Actividad / motivo");
        panelFormulario.add(lbActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 140, -1, -1));

        txtActividad.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtActividad.setForeground(new java.awt.Color(51, 51, 51));
        txtActividad.setText("Ej. práctica, proyecto, exposición...");
        txtActividad.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        panelFormulario.add(txtActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 165, 380, 30));

        btnBuscarDisponibilidad.setBackground(new java.awt.Color(8, 173, 141));
        btnBuscarDisponibilidad.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnBuscarDisponibilidad.setForeground(new java.awt.Color(255, 255, 255));
        btnBuscarDisponibilidad.setText("Buscar disponibilidad");
        btnBuscarDisponibilidad.setBorderPainted(false);
        btnBuscarDisponibilidad.setFocusPainted(false);
        btnBuscarDisponibilidad.addActionListener(this::btnBuscarDisponibilidadActionPerformed);
        panelFormulario.add(btnBuscarDisponibilidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 165, 195, 30));

        panelContenedor.add(panelFormulario, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 95, 650, 220));

        panelGuia.setBackground(new java.awt.Color(255, 255, 255));
        panelGuia.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelGuia.setPreferredSize(new java.awt.Dimension(290, 220));
        panelGuia.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloGuia.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbTituloGuia.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloGuia.setText("Guía rápida");
        panelGuia.add(lbTituloGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, 220, 26));

        lbGuia1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbGuia1.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia1.setText("<html>1. Selecciona fecha, horario y un laboratorio disponible.</html>");
        panelGuia.add(lbGuia1, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 55, 240, 42));

        lbGuia2.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbGuia2.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia2.setText("<html>2. Escribe la actividad y pulsa Buscar disponibilidad.</html>");
        panelGuia.add(lbGuia2, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 100, 240, 42));

        lbGuia3.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        lbGuia3.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia3.setText("<html>3. Elige una fila y solicita. Tu usuario, turno y el estado Pendiente se guardan automáticamente.</html>");
        panelGuia.add(lbGuia3, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 145, 240, 62));

        panelContenedor.add(panelGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(705, 95, 290, 220));

        panelResultados.setBackground(new java.awt.Color(255, 255, 255));
        panelResultados.setPreferredSize(new java.awt.Dimension(625, 255));
        panelResultados.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloResultados.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbTituloResultados.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloResultados.setText("Laboratorios disponibles");
        panelResultados.add(lbTituloResultados, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, -1, -1));

        tablaDisponibilidad.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        tablaDisponibilidad.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Laboratorio", "Horario", "Capacidad", "Estado", "Acción"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaDisponibilidad.setRowHeight(34);
        tablaDisponibilidad.setSelectionBackground(new java.awt.Color(224, 247, 241));
        tablaDisponibilidad.setShowVerticalLines(false);
        jScrollPaneTabla.setViewportView(tablaDisponibilidad);

        panelResultados.add(jScrollPaneTabla, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 55, 595, 180));

        panelContenedor.add(panelResultados, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 330, 645, 255));

        panelResumen.setBackground(new java.awt.Color(255, 255, 255));
        panelResumen.setPreferredSize(new java.awt.Dimension(290, 255));
        panelResumen.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloResumen.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        lbTituloResumen.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloResumen.setText("Resumen");
        panelResumen.add(lbTituloResumen, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, -1, -1));

        lbResumenLaboratorio.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbResumenLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        lbResumenLaboratorio.setText("Laboratorio");
        panelResumen.add(lbResumenLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 58, -1, -1));

        txtResumenLaboratorio.setEditable(false);
        txtResumenLaboratorio.setBackground(new java.awt.Color(255, 255, 255));
        txtResumenLaboratorio.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtResumenLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        panelResumen.add(txtResumenLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 78, 240, 26));

        lbResumenFecha.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbResumenFecha.setForeground(new java.awt.Color(102, 102, 102));
        lbResumenFecha.setText("Fecha");
        panelResumen.add(lbResumenFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 112, -1, -1));

        txtResumenFecha.setEditable(false);
        txtResumenFecha.setBackground(new java.awt.Color(255, 255, 255));
        txtResumenFecha.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtResumenFecha.setText("2026-07-15");
        txtResumenFecha.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        panelResumen.add(txtResumenFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 132, 240, 26));

        lbResumenHorario.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbResumenHorario.setForeground(new java.awt.Color(102, 102, 102));
        lbResumenHorario.setText("Horario");
        panelResumen.add(lbResumenHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 166, -1, -1));

        txtResumenHorario.setEditable(false);
        txtResumenHorario.setBackground(new java.awt.Color(255, 255, 255));
        txtResumenHorario.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtResumenHorario.setText("11:00 - 13:00");
        txtResumenHorario.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        panelResumen.add(txtResumenHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 186, 240, 26));

        btnSolicitarReserva.setBackground(new java.awt.Color(8, 173, 141));
        btnSolicitarReserva.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnSolicitarReserva.setForeground(new java.awt.Color(255, 255, 255));
        btnSolicitarReserva.setText("Solicitar reserva");
        btnSolicitarReserva.setBorderPainted(false);
        btnSolicitarReserva.setFocusPainted(false);
        btnSolicitarReserva.addActionListener(this::btnSolicitarReservaActionPerformed);
        panelResumen.add(btnSolicitarReserva, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 225, 240, 35));

        panelContenedor.add(panelResumen, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 330, 285, 285));

        getContentPane().add(panelContenedor, new org.netbeans.lib.awtextra.AbsoluteConstraints(256, 106, 1030, 625));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnReservasActionPerformed(java.awt.event.ActionEvent evt) {
        dateFechaReserva.requestFocusInWindow();
    }

    private void btnMisReservasActionPerformed(java.awt.event.ActionEvent evt) {
        DashboardAlumno dashboard = new DashboardAlumno(nombreUsuario);
        dashboard.setLocationRelativeTo(null);
        dashboard.setVisible(true);
        dispose();
    }

    private void btnCerrarSesionActionPerformed(java.awt.event.ActionEvent evt) {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Deseas cerrar sesion?", "Cerrar sesion", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            Login login = new Login();
            
            login.setVisible(true);
            login.setLocationRelativeTo(null);
            dispose();
        }
    }

    private void btnBuscarDisponibilidadActionPerformed(java.awt.event.ActionEvent evt) {
        buscarDisponibilidad();
    }

    private void btnSolicitarReservaActionPerformed(java.awt.event.ActionEvent evt) {
        solicitarReserva();
    }

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            ReservasAlumno reservas = new ReservasAlumno("Alumno");
            reservas.setLocationRelativeTo(null);
            reservas.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscarDisponibilidad;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnMisReservas;
    private javax.swing.JButton btnReporteFallas;
    private javax.swing.JButton btnReservas;
    private javax.swing.JButton btnSolicitarReserva;
    private javax.swing.JComboBox cmbHorario;
    private javax.swing.JComboBox cmbLaboratorio;
    private com.toedter.calendar.JDateChooser dateFechaReserva;
    private javax.swing.JPanel headerBlanco;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JLabel imgUTJ;
    private javax.swing.JScrollPane jScrollPaneTabla;
    private javax.swing.JLabel lbActividad;
    private javax.swing.JLabel lbFecha;
    private javax.swing.JLabel lbGuia1;
    private javax.swing.JLabel lbGuia2;
    private javax.swing.JLabel lbGuia3;
    private javax.swing.JLabel lbHorario;
    private javax.swing.JLabel lbLaboratorio;
    private javax.swing.JLabel lbNombreUsuario;
    private javax.swing.JLabel lbResumenFecha;
    private javax.swing.JLabel lbResumenHorario;
    private javax.swing.JLabel lbResumenLaboratorio;
    private javax.swing.JLabel lbSubtituloPantalla;
    private javax.swing.JLabel lbTituloFormulario;
    private javax.swing.JLabel lbTituloGuia;
    private javax.swing.JLabel lbTituloPantalla;
    private javax.swing.JLabel lbTituloResultados;
    private javax.swing.JLabel lbTituloResumen;
    private javax.swing.JPanel panelContenedor;
    private javax.swing.JPanel panelFormulario;
    private javax.swing.JPanel panelGuia;
    private javax.swing.JPanel panelResultados;
    private javax.swing.JPanel panelResumen;
    private javax.swing.JPanel sidebarVerde;
    private javax.swing.JTable tablaDisponibilidad;
    private javax.swing.JTextField txtActividad;
    private javax.swing.JTextField txtResumenFecha;
    private javax.swing.JTextField txtResumenHorario;
    private javax.swing.JTextField txtResumenLaboratorio;
    // End of variables declaration//GEN-END:variables
}
