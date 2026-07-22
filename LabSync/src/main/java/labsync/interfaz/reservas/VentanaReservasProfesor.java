package labsync.interfaz.reservas;

import labsync.aplicacion.AplicacionLabSync;
import labsync.configuracion.ConexionBaseDatos;
import labsync.interfaz.comun.Recursos;
import labsync.servicio.ServicioDisponibilidad;
import labsync.modelo.SesionUsuario;
import labsync.interfaz.comun.ValidacionFechas;
import labsync.interfaz.bitacora.VentanaBitacoraProfesor;
import labsync.interfaz.reservas.VentanaGestionReservas;
import labsync.interfaz.autenticacion.VentanaInicioSesion;
import labsync.interfaz.reservas.VentanaMisReservasProfesor;
import labsync.interfaz.panel.VentanaPanelProfesor;
import labsync.interfaz.fallas.VentanaReporteFallaProfesor;

/**
 * Interfaz del usuario Profesor para AplicacionLabSync.
 * Archivo JFrame Form compatible con el diseñador visual de NetBeans.
 * La sección initComponents se mantiene sincronizada con VentanaReservasProfesor.form.
 */
public class VentanaReservasProfesor extends javax.swing.JFrame {

    private String nombreUsuario;
    private SesionUsuario sesion;

    public VentanaReservasProfesor() {
        this("Profesor");
    }

    public VentanaReservasProfesor(String nombreRecibido) {
        this(SesionUsuario.buscarProfesor(nombreRecibido));
    }

    public VentanaReservasProfesor(SesionUsuario sesionRecibida) {
        initComponents();
        sesion = sesionRecibida == null ? SesionUsuario.buscarProfesor("Profesor") : sesionRecibida;
        nombreUsuario = sesion.getNombre();
        lbNombreUsuario.setText("Hola, " + nombreUsuario);
        java.net.URL icono = getClass().getResource("/images/logo_labsync_no_background.png");
        if (icono != null) setIconImage(new javax.swing.ImageIcon(icono).getImage());
        configurarNavegacion();
        configurarPantalla();
        labsync.interfaz.comun.NotificacionesGlobales.profesor(this, header, sesion);
        setLocationRelativeTo(null);
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

    private boolean disponibilidadConfirmada;
    private int capacidadLaboratorio;

    private void configurarPantalla() {
        dateFecha.setMinSelectableDate(new java.util.Date());
        ValidacionFechas.bloquearFinesDeSemana(dateFecha);
        dateFecha.setDate(ValidacionFechas.siguienteDiaHabil(new java.util.Date()));
        cargarLaboratorios(cmbLaboratorio, false);
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnConsultar.addActionListener(e -> consultarDisponibilidad());
        btnEnviar.addActionListener(e -> enviarSolicitud());
        dateFecha.addPropertyChangeListener("date", e -> invalidarConsulta());
        cmbHorario.addActionListener(e -> {
            seleccionarTurnoPorHorario();
            invalidarConsulta();
        });
        cmbLaboratorio.addActionListener(e -> invalidarConsulta());
        cmbTurno.setEnabled(false);
    }

    private void invalidarConsulta() {
        disponibilidadConfirmada = false;
        capacidadLaboratorio = 0;
        lbEstadoDisponibilidad.setText("Disponibilidad: pendiente de consulta");
        lbEstadoDisponibilidad.setForeground(new java.awt.Color(100, 100, 100));
    }

    private void limpiarFormulario() {
        dateFecha.setDate(ValidacionFechas.siguienteDiaHabil(new java.util.Date()));
        cmbHorario.setSelectedIndex(0);
        if (cmbLaboratorio.getItemCount() > 0) cmbLaboratorio.setSelectedIndex(0);
        txtActividad.setText("");
        cmbTurno.setSelectedIndex(0);
        cmbCarrera.setSelectedIndex(0);
        cmbGrado.setSelectedIndex(0);
        cmbGrupo.setSelectedIndex(0);
        txtAlumnos.setText("");
        txtObservaciones.setText("");
        invalidarConsulta();
    }

    private boolean validarDatosBase() {
        if (dateFecha.getDate() == null || cmbHorario.getSelectedIndex() == 0 || cmbLaboratorio.getSelectedIndex() == 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "Selecciona fecha, horario y laboratorio.", "Datos requeridos", javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }
        java.time.LocalDate fecha = new java.sql.Date(dateFecha.getDate().getTime()).toLocalDate();
        if (!ValidacionFechas.validarDiaHabil(this, dateFecha.getDate(), "realizar reservas")) return false;
        if (fecha.isBefore(java.time.LocalDate.now())) {
            javax.swing.JOptionPane.showMessageDialog(this, "La fecha de la reserva no puede estar en el pasado.", "Fecha no válida", javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return ValidacionFechas.validarHorarioFuturo(
                this, fecha, cmbHorario.getSelectedItem().toString());
    }

    private void consultarDisponibilidad() {
        if (!validarDatosBase()) return;
        try (java.sql.Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) throw new java.sql.SQLException("No hay conexión con la base de datos.");
            ServicioDisponibilidad.ResultadoDisponibilidad resultado
                    = new ServicioDisponibilidad().consultarParaProfesor(
                            con,
                            cmbLaboratorio.getSelectedItem().toString(),
                            new java.sql.Date(dateFecha.getDate().getTime()).toLocalDate(),
                            cmbHorario.getSelectedItem().toString(),
                            false);
            capacidadLaboratorio = resultado.getCapacidad();
            disponibilidadConfirmada = resultado.estaDisponible();
            String estadoDisponibilidad = resultado.estaBloqueadoPorMantenimiento()
                    ? "no disponible por mantenimiento"
                    : "horario ocupado";
            lbEstadoDisponibilidad.setText(disponibilidadConfirmada
                    ? "Disponibilidad: laboratorio disponible"
                    : "Disponibilidad: " + estadoDisponibilidad);
            lbEstadoDisponibilidad.setForeground(disponibilidadConfirmada ? new java.awt.Color(8,173,141) : new java.awt.Color(220,53,69));
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudo consultar la disponibilidad:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enviarSolicitud() {
        if (!validarDatosBase()) return;
        if (!disponibilidadConfirmada) {
            javax.swing.JOptionPane.showMessageDialog(this, "Consulta nuevamente la disponibilidad antes de enviar.", "Consulta requerida", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (txtActividad.getText().isBlank() || cmbCarrera.getSelectedIndex() == 0 || cmbTurno.getSelectedIndex() == 0 || cmbGrado.getSelectedIndex() == 0 || cmbGrupo.getSelectedIndex() == 0 || txtAlumnos.getText().isBlank()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Completa actividad, carrera, grado, grupo y cantidad de alumnos.", "Datos requeridos", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        int cantidad;
        try { cantidad = Integer.parseInt(txtAlumnos.getText().trim()); }
        catch (NumberFormatException ex) { javax.swing.JOptionPane.showMessageDialog(this, "La cantidad de alumnos debe ser numérica."); return; }
        if (cantidad <= 0) { javax.swing.JOptionPane.showMessageDialog(this, "La cantidad de alumnos debe ser mayor a cero."); return; }
        if (cantidad > capacidadLaboratorio) {
            javax.swing.JOptionPane.showMessageDialog(this, "La cantidad supera la capacidad del laboratorio (" + capacidadLaboratorio + ").", "Capacidad excedida", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!sesion.estaIdentificada()) {
            javax.swing.JOptionPane.showMessageDialog(this, "La sesión no está identificada. Inicia sesión nuevamente.", "Sesión inválida", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        String sql = "INSERT INTO reservas (id_usuario, nombre_solicitante, rol_solicitante, laboratorio, actividad, carrera, grado, grupo, turno, fecha, horario, cantidad_alumnos, estado, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pendiente', ?)";
        try (java.sql.Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) throw new java.sql.SQLException("No hay conexión con la base de datos.");
            con.setAutoCommit(false);
            try {
                ServicioDisponibilidad.ResultadoDisponibilidad disponibilidad
                        = new ServicioDisponibilidad().consultarParaProfesor(
                                con,
                                cmbLaboratorio.getSelectedItem().toString(),
                                new java.sql.Date(dateFecha.getDate().getTime()).toLocalDate(),
                                cmbHorario.getSelectedItem().toString(),
                                true);
                if (!disponibilidad.estaDisponible()) {
                    con.rollback();
                    disponibilidadConfirmada = false;
                    javax.swing.JOptionPane.showMessageDialog(this, disponibilidad.getMensaje(), "Horario ocupado", javax.swing.JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, sesion.getId());
                    ps.setString(2, sesion.getNombreCompleto());
                    ps.setString(3, sesion.getRol());
                    ps.setString(4, cmbLaboratorio.getSelectedItem().toString());
                    ps.setString(5, txtActividad.getText().trim());
                    ps.setString(6, cmbCarrera.getSelectedItem().toString());
                    ps.setString(7, cmbGrado.getSelectedItem().toString());
                    ps.setString(8, cmbGrupo.getSelectedItem().toString());
                    ps.setString(9, cmbTurno.getSelectedItem().toString());
                    ps.setDate(10, new java.sql.Date(dateFecha.getDate().getTime()));
                    ps.setString(11, cmbHorario.getSelectedItem().toString());
                    ps.setInt(12, cantidad);
                    String obs = txtObservaciones.getText().trim();
                    if (obs.isEmpty()) ps.setNull(13, java.sql.Types.VARCHAR); else ps.setString(13, obs);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (java.sql.SQLException ex) {
                con.rollback();
                throw ex;
            }
            javax.swing.JOptionPane.showMessageDialog(this, "La reserva fue enviada con estado Pendiente.", "Reserva registrada", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudo registrar la reserva:\n" + ex.getMessage(), "Error SQL", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seleccionarTurnoPorHorario() {
        if (cmbHorario.getSelectedIndex() <= 0) {
            cmbTurno.setSelectedIndex(0);
            return;
        }
        String horario = cmbHorario.getSelectedItem().toString();
        java.time.LocalTime inicio = java.time.LocalTime.parse(
                horario.substring(0, horario.indexOf(" - ")).trim(),
                java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        cmbTurno.setSelectedItem(inicio.isBefore(java.time.LocalTime.of(14, 10))
                ? "Matutino" : "Vespertino");
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
        lbFecha = new javax.swing.JLabel();
        dateFecha = new com.toedter.calendar.JDateChooser();
        lbHorario = new javax.swing.JLabel();
        cmbHorario = new javax.swing.JComboBox<String>();
        lbLaboratorio = new javax.swing.JLabel();
        cmbLaboratorio = new javax.swing.JComboBox<String>();
        lbActividad = new javax.swing.JLabel();
        txtActividad = new javax.swing.JTextField();
        lbCarrera = new javax.swing.JLabel();
        cmbCarrera = new javax.swing.JComboBox<String>();
        lbTurno = new javax.swing.JLabel();
        cmbTurno = new javax.swing.JComboBox<String>();
        lbGrado = new javax.swing.JLabel();
        cmbGrado = new javax.swing.JComboBox<String>();
        lbGrupo = new javax.swing.JLabel();
        cmbGrupo = new javax.swing.JComboBox<String>();
        lbAlumnos = new javax.swing.JLabel();
        txtAlumnos = new javax.swing.JTextField();
        lbObservaciones = new javax.swing.JLabel();
        scrollObservaciones = new javax.swing.JScrollPane();
        txtObservaciones = new javax.swing.JTextArea();
        lbAyudaObservaciones = new javax.swing.JLabel();
        lbEstadoDisponibilidad = new javax.swing.JLabel();
        btnLimpiar = new javax.swing.JButton();
        btnConsultar = new javax.swing.JButton();
        btnEnviar = new javax.swing.JButton();
        panelGuia = new javax.swing.JPanel();
        lbTituloGuia = new javax.swing.JLabel();
        lbGuia1 = new javax.swing.JLabel();
        lbGuia2 = new javax.swing.JLabel();
        lbGuia3 = new javax.swing.JLabel();
        panelDatosBD = new javax.swing.JPanel();
        lbTituloDatosBD = new javax.swing.JLabel();
        lbDato1 = new javax.swing.JLabel();
        lbDato2 = new javax.swing.JLabel();
        lbDato3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LabSync - Reservar laboratorio");
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
        lbTituloPantalla.setText("Reservar laboratorio");
        body.add(lbTituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 15, 420, 30));
        lbSubtituloPantalla.setFont(new java.awt.Font("Arial", 0, 13));
        lbSubtituloPantalla.setForeground(new java.awt.Color(100, 100, 100));
        lbSubtituloPantalla.setText("Solicita un laboratorio con la información necesaria para registrar tu reservación.");
        body.add(lbSubtituloPantalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 45, 820, 22));
        panelFormulario.setBackground(new java.awt.Color(255, 255, 255));
        panelFormulario.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelFormulario.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloDatos.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloDatos.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloDatos.setText("Datos de la solicitud");
        panelFormulario.add(lbTituloDatos, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 15, 300, 28));
        lbFecha.setFont(new java.awt.Font("Arial", 1, 12));
        lbFecha.setForeground(new java.awt.Color(90, 90, 90));
        lbFecha.setText("Fecha");
        panelFormulario.add(lbFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 58, 120, 20));
        dateFecha.setBackground(new java.awt.Color(255, 255, 255));
        dateFecha.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        dateFecha.setForeground(new java.awt.Color(51, 51, 51));
        dateFecha.setDateFormatString("yyyy-MM-dd");
        dateFecha.setFont(new java.awt.Font("Arial", 0, 12));
        panelFormulario.add(dateFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 80, 180, 28));
        lbHorario.setFont(new java.awt.Font("Arial", 1, 12));
        lbHorario.setForeground(new java.awt.Color(90, 90, 90));
        lbHorario.setText("Horario");
        panelFormulario.add(lbHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 58, 120, 20));
        cmbHorario.setFont(new java.awt.Font("Arial", 0, 12));
        cmbHorario.setForeground(new java.awt.Color(51, 51, 51));
        cmbHorario.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Selecciona horario", "7:00 - 7:50", "7:50 - 8:40", "9:10 - 10:00", "10:00 - 10:50", "10:50 - 11:40", "11:40 - 12:30", "12:30 - 13:20", "13:20 - 14:10", "14:10 - 15:00", "15:00 - 15:50", "15:50 - 16:40", "16:40 - 17:30", "18:00 - 18:50", "18:50 - 19:40", "19:40 - 20:30"}));
        cmbHorario.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 80, 180, 28));
        lbLaboratorio.setFont(new java.awt.Font("Arial", 1, 12));
        lbLaboratorio.setForeground(new java.awt.Color(90, 90, 90));
        lbLaboratorio.setText("Laboratorio");
        panelFormulario.add(lbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(425, 58, 160, 20));
        cmbLaboratorio.setFont(new java.awt.Font("Arial", 0, 12));
        cmbLaboratorio.setForeground(new java.awt.Color(51, 51, 51));
        cmbLaboratorio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar"}));
        cmbLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(425, 80, 190, 28));
        lbActividad.setFont(new java.awt.Font("Arial", 1, 12));
        lbActividad.setForeground(new java.awt.Color(90, 90, 90));
        lbActividad.setText("Actividad o materia");
        panelFormulario.add(lbActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 125, 220, 20));
        txtActividad.setBackground(new java.awt.Color(255, 255, 255));
        txtActividad.setFont(new java.awt.Font("Arial", 0, 12));
        txtActividad.setForeground(new java.awt.Color(51, 51, 51));
        txtActividad.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(txtActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 147, 230, 28));
        lbCarrera.setFont(new java.awt.Font("Arial", 1, 12));
        lbCarrera.setForeground(new java.awt.Color(90, 90, 90));
        lbCarrera.setText("Carrera");
        panelFormulario.add(lbCarrera, new org.netbeans.lib.awtextra.AbsoluteConstraints(275, 125, 120, 20));
        cmbCarrera.setFont(new java.awt.Font("Arial", 0, 12));
        cmbCarrera.setForeground(new java.awt.Color(51, 51, 51));
        cmbCarrera.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar", "DSM - Desarrollo de Software Multiplataforma", "EVND - Entornos Virtuales y Negocios Digitales"}));
        cmbCarrera.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbCarrera, new org.netbeans.lib.awtextra.AbsoluteConstraints(275, 147, 220, 28));
        lbTurno.setFont(new java.awt.Font("Arial", 1, 12));
        lbTurno.setForeground(new java.awt.Color(90, 90, 90));
        lbTurno.setText("Turno");
        panelFormulario.add(lbTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(515, 125, 100, 20));
        cmbTurno.setFont(new java.awt.Font("Arial", 0, 12));
        cmbTurno.setForeground(new java.awt.Color(51, 51, 51));
        cmbTurno.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar", "Matutino", "Vespertino"}));
        cmbTurno.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(515, 147, 100, 28));
        lbGrado.setFont(new java.awt.Font("Arial", 1, 12));
        lbGrado.setForeground(new java.awt.Color(90, 90, 90));
        lbGrado.setText("Grado");
        panelFormulario.add(lbGrado, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 192, 100, 20));
        cmbGrado.setFont(new java.awt.Font("Arial", 0, 12));
        cmbGrado.setForeground(new java.awt.Color(51, 51, 51));
        cmbGrado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar", "1°", "2°", "3°", "4°", "5°", "6°"}));
        cmbGrado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbGrado, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 214, 140, 28));
        lbGrupo.setFont(new java.awt.Font("Arial", 1, 12));
        lbGrupo.setForeground(new java.awt.Color(90, 90, 90));
        lbGrupo.setText("Grupo");
        panelFormulario.add(lbGrupo, new org.netbeans.lib.awtextra.AbsoluteConstraints(185, 192, 100, 20));
        cmbGrupo.setFont(new java.awt.Font("Arial", 0, 12));
        cmbGrupo.setForeground(new java.awt.Color(51, 51, 51));
        cmbGrupo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"Seleccionar", "A", "B"}));
        cmbGrupo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(cmbGrupo, new org.netbeans.lib.awtextra.AbsoluteConstraints(185, 214, 140, 28));
        lbAlumnos.setFont(new java.awt.Font("Arial", 1, 12));
        lbAlumnos.setForeground(new java.awt.Color(90, 90, 90));
        lbAlumnos.setText("Cantidad de alumnos");
        panelFormulario.add(lbAlumnos, new org.netbeans.lib.awtextra.AbsoluteConstraints(345, 192, 180, 20));
        txtAlumnos.setBackground(new java.awt.Color(255, 255, 255));
        txtAlumnos.setFont(new java.awt.Font("Arial", 0, 12));
        txtAlumnos.setForeground(new java.awt.Color(51, 51, 51));
        txtAlumnos.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 180, 180)));
        panelFormulario.add(txtAlumnos, new org.netbeans.lib.awtextra.AbsoluteConstraints(345, 214, 270, 28));
        lbObservaciones.setFont(new java.awt.Font("Arial", 1, 12));
        lbObservaciones.setForeground(new java.awt.Color(90, 90, 90));
        lbObservaciones.setText("Observaciones");
        panelFormulario.add(lbObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 260, 200, 20));
        txtObservaciones.setColumns(20);
        txtObservaciones.setFont(new java.awt.Font("Arial", 0, 12));
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setRows(5);
        txtObservaciones.setWrapStyleWord(true);
        scrollObservaciones.setViewportView(txtObservaciones);
        panelFormulario.add(scrollObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 282, 590, 100));
        lbAyudaObservaciones.setFont(new java.awt.Font("Arial", 2, 11));
        lbAyudaObservaciones.setForeground(new java.awt.Color(120, 120, 120));
        lbAyudaObservaciones.setText("Incluye software requerido u otra indicación para el laboratorista.");
        panelFormulario.add(lbAyudaObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 384, 500, 18));
        lbEstadoDisponibilidad.setFont(new java.awt.Font("Arial", 1, 13));
        lbEstadoDisponibilidad.setForeground(new java.awt.Color(100, 100, 100));
        lbEstadoDisponibilidad.setText("Disponibilidad: pendiente de consulta");
        panelFormulario.add(lbEstadoDisponibilidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 405, 390, 22));
        btnLimpiar.setBackground(new java.awt.Color(230, 230, 230));
        btnLimpiar.setFont(new java.awt.Font("Arial", 1, 14));
        btnLimpiar.setForeground(new java.awt.Color(70, 70, 70));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnLimpiar.setFocusPainted(false);
        panelFormulario.add(btnLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 442, 110, 38));
        btnConsultar.setBackground(new java.awt.Color(6, 140, 115));
        btnConsultar.setFont(new java.awt.Font("Arial", 1, 14));
        btnConsultar.setForeground(new java.awt.Color(255, 255, 255));
        btnConsultar.setText("Consultar");
        btnConsultar.setBorderPainted(false);
        btnConsultar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnConsultar.setFocusPainted(false);
        panelFormulario.add(btnConsultar, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 442, 120, 38));
        btnEnviar.setBackground(new java.awt.Color(8, 173, 141));
        btnEnviar.setFont(new java.awt.Font("Arial", 1, 14));
        btnEnviar.setForeground(new java.awt.Color(255, 255, 255));
        btnEnviar.setText("Enviar solicitud");
        btnEnviar.setBorderPainted(false);
        btnEnviar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnEnviar.setFocusPainted(false);
        panelFormulario.add(btnEnviar, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 442, 145, 38));
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
        lbGuia1.setText("<html>1. Selecciona fecha, horario y laboratorio.</html>");
        panelGuia.add(lbGuia1, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 60, 240, 38));
        lbGuia2.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia2.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia2.setText("<html>2. Captura la actividad, grupo, turno y cantidad de alumnos.</html>");
        panelGuia.add(lbGuia2, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 105, 240, 48));
        lbGuia3.setFont(new java.awt.Font("Arial", 0, 12));
        lbGuia3.setForeground(new java.awt.Color(102, 102, 102));
        lbGuia3.setText("<html>3. Consulta la disponibilidad antes de enviar la solicitud.</html>");
        panelGuia.add(lbGuia3, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 160, 240, 45));
        body.add(panelGuia, new org.netbeans.lib.awtextra.AbsoluteConstraints(705, 85, 290, 235));
        panelDatosBD.setBackground(new java.awt.Color(255, 255, 255));
        panelDatosBD.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));
        panelDatosBD.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        lbTituloDatosBD.setFont(new java.awt.Font("Arial", 1, 18));
        lbTituloDatosBD.setForeground(new java.awt.Color(8, 173, 141));
        lbTituloDatosBD.setText("Información del registro");
        panelDatosBD.add(lbTituloDatosBD, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 18, 240, 26));
        lbDato1.setFont(new java.awt.Font("Arial", 0, 12));
        lbDato1.setForeground(new java.awt.Color(90, 90, 90));
        lbDato1.setText("<html>El nombre y rol del profesor se obtienen de la sesión.</html>");
        panelDatosBD.add(lbDato1, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 60, 240, 45));
        lbDato2.setFont(new java.awt.Font("Arial", 0, 12));
        lbDato2.setForeground(new java.awt.Color(90, 90, 90));
        lbDato2.setText("<html>La solicitud se guarda inicialmente con estado Pendiente.</html>");
        panelDatosBD.add(lbDato2, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 112, 240, 45));
        lbDato3.setFont(new java.awt.Font("Arial", 0, 12));
        lbDato3.setForeground(new java.awt.Color(90, 90, 90));
        lbDato3.setText("<html>Las observaciones pueden incluir software requerido o indicaciones especiales.</html>");
        panelDatosBD.add(lbDato3, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 164, 240, 55));
        body.add(panelDatosBD, new org.netbeans.lib.awtextra.AbsoluteConstraints(705, 340, 290, 245));

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
            java.util.logging.Logger.getLogger(VentanaReservasProfesor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new VentanaReservasProfesor("Profesor").setVisible(true));
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
    private javax.swing.JLabel lbFecha;
    private com.toedter.calendar.JDateChooser dateFecha;
    private javax.swing.JLabel lbHorario;
    private javax.swing.JComboBox<String> cmbHorario;
    private javax.swing.JLabel lbLaboratorio;
    private javax.swing.JComboBox<String> cmbLaboratorio;
    private javax.swing.JLabel lbActividad;
    private javax.swing.JTextField txtActividad;
    private javax.swing.JLabel lbCarrera;
    private javax.swing.JComboBox<String> cmbCarrera;
    private javax.swing.JLabel lbTurno;
    private javax.swing.JComboBox<String> cmbTurno;
    private javax.swing.JLabel lbGrado;
    private javax.swing.JComboBox<String> cmbGrado;
    private javax.swing.JLabel lbGrupo;
    private javax.swing.JComboBox<String> cmbGrupo;
    private javax.swing.JLabel lbAlumnos;
    private javax.swing.JTextField txtAlumnos;
    private javax.swing.JLabel lbObservaciones;
    private javax.swing.JScrollPane scrollObservaciones;
    private javax.swing.JTextArea txtObservaciones;
    private javax.swing.JLabel lbAyudaObservaciones;
    private javax.swing.JLabel lbEstadoDisponibilidad;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnConsultar;
    private javax.swing.JButton btnEnviar;
    private javax.swing.JPanel panelGuia;
    private javax.swing.JLabel lbTituloGuia;
    private javax.swing.JLabel lbGuia1;
    private javax.swing.JLabel lbGuia2;
    private javax.swing.JLabel lbGuia3;
    private javax.swing.JPanel panelDatosBD;
    private javax.swing.JLabel lbTituloDatosBD;
    private javax.swing.JLabel lbDato1;
    private javax.swing.JLabel lbDato2;
    private javax.swing.JLabel lbDato3;
    // End of variables declaration//GEN-END:variables
}
