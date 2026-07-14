package labsync.labsync;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import java.awt.Component;
import java.util.Date;
import java.text.ParseException;
import java.time.LocalDate;
import com.toedter.calendar.JDateChooser;
import java.text.SimpleDateFormat;

public class Mantenimiento extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Mantenimiento.class.getName());
    private String nombreUsuario;
    private int idMantenimientoSeleccionado = 0;
    boolean modoEdicion = false;
    private final String PH_BUSCAR = "Código, equipo, laboratorio o estado";
    private final Color COLOR_PLACEHOLDER = new Color(150, 150, 150);
    private final Color COLOR_TEXTO = new Color(51, 51, 51);
    
    public Mantenimiento(String nombreRecibido) {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        
        this.nombreUsuario = nombreRecibido;
        
        txtObservacionesMant.setLineWrap(true);
        txtObservacionesMant.setWrapStyleWord(true);
        
        txtResponsable.setText(nombreUsuario);
        
        configurarDataChooser();
        ponerPlaceholderBuscar();
        cargarCodigosEquipo();
        cargarTablaMantenimiento();
    }
    
    public Mantenimiento() {
        initComponents();

        this.nombreUsuario = "Usuario";

        txtObservacionesMant.setLineWrap(true);
        txtObservacionesMant.setWrapStyleWord(true);

        txtResponsable.setText(nombreUsuario);

        configurarDataChooser();
        ponerPlaceholderBuscar();
        cargarCodigosEquipo();
        cargarTablaMantenimiento();
    }
    
    private void configurarDataChooser() {
        dateFechaProgramada.setDateFormatString("yyyy-MM-dd");
        dateFechaProgramada.setBackground(Color.WHITE);
        dateFechaProgramada.setForeground(new Color(51, 51, 51));
        dateFechaProgramada.setFont(new Font("Arial", Font.PLAIN, 12));
        dateFechaProgramada.setBorder(BorderFactory.createLineBorder(new Color(102, 102, 102)));
        
        Component editor = dateFechaProgramada.getDateEditor().getUiComponent();
        
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
        
        dateFechaProgramada.getCalendarButton().setBackground(Color.WHITE);
        dateFechaProgramada.getCalendarButton().setForeground(new Color(51, 51, 51));
    }
    
    private void ponerPlaceholderBuscar() {
        txtBuscar.setText(PH_BUSCAR);
        txtBuscar.setForeground(COLOR_PLACEHOLDER);
        
        txtBuscar.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (txtBuscar.getText().equals(PH_BUSCAR)) {
                    txtBuscar.setText("");
                    txtBuscar.setForeground(COLOR_TEXTO);
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (txtBuscar.getText().trim().isEmpty()) {
                    txtBuscar.setText(PH_BUSCAR);
                    txtBuscar.setForeground(COLOR_PLACEHOLDER);
                }
            }
        });
    }
    
    private void limpiarCampos() {
        if (cmbCodigoEquipo.getItemCount() > 0) {
            cmbCodigoEquipo.setSelectedIndex(0);
        }

        txtNombreEquipoMant.setText("");
        txtLaboratorioModal.setText("");
        dateFechaProgramada.setDate(null);
        txtObservacionesMant.setText("");

        txtResponsable.setText(nombreUsuario);

        cmbTipoMantModal.setSelectedIndex(0);
        cmbEstadoMantModal.setSelectedIndex(0);
    }
    
    private void cargarCodigosEquipo() {
        cmbCodigoEquipo.removeAllItems();
        cmbCodigoEquipo.addItem("Selecciona");
        
        java.sql.Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        String sql = "SELECT codigo FROM inventario ORDER BY codigo ASC";
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                cmbCodigoEquipo.addItem(rs.getString("codigo"));
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cargar códigos de equipo: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
                
            }
        }
    }
    
    private void cargarDatosEquipoSeleccionado() {
        Object itemSeleccionado = cmbCodigoEquipo.getSelectedItem();

        if (itemSeleccionado == null) {
            txtNombreEquipoMant.setText("");
            txtLaboratorioModal.setText("");
            return;
        }

        String codigoSeleccionado = itemSeleccionado.toString();

        if (codigoSeleccionado.equals("Selecciona")) {
            txtNombreEquipoMant.setText("");
            txtLaboratorioModal.setText("");
            return;
        }

        java.sql.Connection con = ConexionBD.conectar();

        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String sql = "SELECT nombre_equipo, laboratorio FROM inventario WHERE codigo = ?";

        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, codigoSeleccionado);

            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                txtNombreEquipoMant.setText(rs.getString("nombre_equipo"));
                txtLaboratorioModal.setText(rs.getString("laboratorio"));
            } else {
                txtNombreEquipoMant.setText("");
                txtLaboratorioModal.setText("");
            }

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cargar los datos del equipo: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );

        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException e) {
            }
        }
    }
    
    private boolean validarCamposMantenimiento() {
        Object codigoSeleccionado = cmbCodigoEquipo.getSelectedItem();
        
        if (codigoSeleccionado == null || codigoSeleccionado.toString().equals("Selecciona")) {
            mostrarErrorValidacion("Selecciona el código del equipo.", cmbCodigoEquipo);
            return false;
        }
        
        if (txtNombreEquipoMant.getText().trim().isEmpty()) {
            mostrarErrorValidacion("El nombre del equpo no se cargó correctamente.", txtNombreEquipoMant);
            return false;
        }
        
        if (txtLaboratorioModal.getText().trim().isEmpty()) {
            mostrarErrorValidacion("El laboratorio no se cargó correctamente.", txtLaboratorioModal);
            return false;
        }
        
        if (cmbTipoMantModal.getSelectedIndex()== 0) {
            mostrarErrorValidacion("Selecciona el tipo de mantenimiento.", cmbTipoMantModal);
            return false;
        }
        
        java.time.LocalDate fechaSeleccionada = dateFechaProgramada.getDate()
            .toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate();
        
        java.time.LocalDate fechaHoy = java.time.LocalDate.now();
        
        if (fechaSeleccionada.isBefore(fechaHoy)) {
            mostrarErrorValidacion(
                "La fecha programada no puede ser anterior al dia de hoy.",
                dateFechaProgramada   
            );
            return false;
        }
        
        if (dateFechaProgramada.getDate() == null) {
            mostrarErrorValidacion("Selecciona la fecha programada", dateFechaProgramada);
            return false;
        }
          
        if (txtResponsable.getText().trim().isEmpty()) {
            mostrarErrorValidacion("Ingresa el responsable del mantenimiento.", txtResponsable);
            return false;
        }
        
        if (cmbEstadoMantModal.getSelectedIndex() == 0) {
            mostrarErrorValidacion("Selecciona el estado del mantenimiento", cmbEstadoMantModal);
            return false;
        }
        
        return true;
    }
    
    private void mostrarErrorValidacion(String mensaje, java.awt.Component componente) {
        javax.swing.JOptionPane.showMessageDialog(
            addMantenimiento,
            mensaje,
            "Campo obligatorio",
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        
        componente.requestFocus();
    }
    
    private boolean validarFormatoFecha(String fecha) {
        try {
            java.time.LocalDate.parse(fecha);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }
    
    private String obtenerFechaProgramada() {
        Date fecha = dateFechaProgramada.getDate();
        
        if (fecha == null) {
            return "";
        }
        
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
        return formato.format(fecha);
    }
    
    private boolean guardarMantenimientoBD() {
        String codigoEquipo = cmbCodigoEquipo.getSelectedItem().toString();
        String nombreEquipo = txtNombreEquipoMant.getText().trim();
        String laboratorio = txtLaboratorioModal.getText().trim();
        String tipoMantenimiento = cmbTipoMantModal.getSelectedItem().toString();
        String fechaProgramada = obtenerFechaProgramada();
        String estadoMantenimiento = cmbEstadoMantModal.getSelectedItem().toString();
        String responsable = txtResponsable.getText().trim();
        String observaciones = txtObservacionesMant.getText().trim();
        
        java.sql.Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                addMantenimiento,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
        
        String sql = "INSERT INTO mantenimiento "
            + "(codigo_equipo, nombre_equipo, laboratorio, tipo_mantenimiento, fecha_programada, estado, responsable, observaciones) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            
            ps.setString(1, codigoEquipo);
            ps.setString(2, nombreEquipo);
            ps.setString(3, laboratorio);
            ps.setString(4, tipoMantenimiento);
            ps.setString(5, fechaProgramada);
            ps.setString(6, estadoMantenimiento);
            ps.setString(7, responsable);
            ps.setString(8, observaciones);
            
            int filas = ps.executeUpdate();
            
            return filas > 0;
        } catch (java.sql.SQLException e ) {
            javax.swing.JOptionPane.showMessageDialog(
                addMantenimiento,
                "Error al guardar el mantenimiento: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;
        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException e) {
                
            }
        }
    }
    
    private void cargarTablaMantenimiento() {
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel();
        
        modelo.addColumn("ID");
        modelo.addColumn("Código Equipo");
        modelo.addColumn("Nombre Equipo");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Tipo Mantenimiento");
        modelo.addColumn("Fecha Programada");
        modelo.addColumn("Estado");
        modelo.addColumn("Responsable");
        modelo.addColumn("Observaciones");
        
        tablaMantenimiento.setModel(modelo);
        ocultarColumnaID();
        
        String sql = "SELECT id_mantenimiento, codigo_equipo, nombre_equipo, laboratorio, tipo_mantenimiento, "
            + "DATE_FORMAT(fecha_programada, '%Y-%m-%d') AS fecha_programada, "
            + "estado, responsable, IFNULL(observaciones, '') AS observaciones "
            + "FROM mantenimiento "
            + "WHERE estado IN ('Pendiente', 'En proceso') "
            + "ORDER BY fecha_programada DESC";
        
        java.sql.Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] fila = new Object[9];

                fila[0] = rs.getInt("id_mantenimiento");
                fila[1] = rs.getString("codigo_equipo");
                fila[2] = rs.getString("nombre_equipo");
                fila[3] = rs.getString("laboratorio");
                fila[4] = rs.getString("tipo_mantenimiento");
                fila[5] = rs.getString("fecha_programada");
                fila[6] = rs.getString("estado");
                fila[7] = rs.getString("responsable");
                fila[8] = rs.getString("observaciones");

                modelo.addRow(fila);
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cargar mantenimientos: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
                
            }
        }
    }
    
    private void cargarTablaMantenimientoFiltrada() {
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel();
        
        modelo.addColumn("ID");
        modelo.addColumn("Código Equipo");
        modelo.addColumn("Nombre Equipo");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Tipo Mantenimiento");
        modelo.addColumn("Fecha Programada");
        modelo.addColumn("Estado");
        modelo.addColumn("Responsable");
        modelo.addColumn("Observaciones");
        
        String textoBusqueda = txtBuscar.getText().trim();
        
        if (textoBusqueda.equals(PH_BUSCAR)) {
            textoBusqueda = "";
        }
        
        String tipoSeleccionado = cmbTipoMant.getSelectedItem().toString();
        String estadoSeleccionado = cmbEstado.getSelectedItem().toString();
        String laboratorioSeleccionado = cmbLaboratorio.getSelectedItem().toString();
        
        String sql = "SELECT id_mantenimiento, codigo_equipo, nombre_equipo, laboratorio, tipo_mantenimiento, "
            + "DATE_FORMAT(fecha_programada, '%Y-%m-%d') AS fecha_programada, "
            + "estado, responsable, IFNULL(observaciones, '') AS observaciones "
            + "FROM mantenimiento WHERE 1=1 ";
        
        java.util.ArrayList<String> parametros = new java.util.ArrayList<>();
        
        if (!textoBusqueda.isEmpty()) {
            sql += "AND (codigo_equipo LIKE ? OR nombre_equipo LIKE ? OR responsable LIKE ?) ";
            String busqueda = "%" + textoBusqueda + "%";
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
        }
        
        if (!tipoSeleccionado.equals("Todos")) {
            sql += "AND tipo_mantenimiento = ? ";
            parametros.add(tipoSeleccionado);
        }
        
        if (!estadoSeleccionado.equals("Todos")) {
            sql += "AND estado = ? ";
            parametros.add(estadoSeleccionado);
        } else {
            sql += "AND estado IN ('Pendiente', 'En proceso') ";
        }
        
        if (!laboratorioSeleccionado.equals("Todos")) {
            sql += "AND laboratorio = ? ";
            parametros.add(laboratorioSeleccionado);
        }
        
        sql += "ORDER BY fecha_programada DESC";
        
        java.sql.Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            
            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }
            
            java.sql.ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] fila = new Object[9];
                
                fila[0] = rs.getInt("id_mantenimiento");
                fila[1] = rs.getString("codigo_equipo");
                fila[2] = rs.getString("nombre_equipo");
                fila[3] = rs.getString("laboratorio");
                fila[4] = rs.getString("tipo_mantenimiento");
                fila[5] = rs.getString("fecha_programada");
                fila[6] = rs.getString("estado");
                fila[7] = rs.getString("responsable");
                fila[8] = rs.getString("observaciones");
                
                modelo.addRow(fila);
            }
            
            tablaMantenimiento.setModel(modelo);
            ocultarColumnaID();
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al filtrar mantenimientos: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
                
            }
        }
   }
    
    private void limpiarFiltrosMantenimiento() {
        txtBuscar.setText(PH_BUSCAR);
        txtBuscar.setForeground(COLOR_PLACEHOLDER);

        cmbTipoMant.setSelectedIndex(0);
        cmbEstado.setSelectedIndex(0);
        cmbLaboratorio.setSelectedIndex(0);

        cargarTablaMantenimientoFiltrada();
    }
    
    private void ocultarColumnaID() {
        if (tablaMantenimiento.getColumnModel().getColumnCount() > 0) {
            tablaMantenimiento.getColumnModel().getColumn(0).setMinWidth(0);
            tablaMantenimiento.getColumnModel().getColumn(0).setMaxWidth(0);
            tablaMantenimiento.getColumnModel().getColumn(0).setWidth(0);
        }
    }
    
    private void marcarMantenimientoRealizado(int idMantenimiento, String codigoEquipo) {
        java.sql.Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        String sqlMantenimiento = "UPDATE mantenimiento SET estado = 'Realizado' WHERE id_mantenimiento = ?";
        String sqlInventario = "UPDATE inventario SET estado = 'Disponible', ultimo_mantenimiento = CURDATE() WHERE codigo = ?";
        
        try {
            java.sql.PreparedStatement psMant = con.prepareStatement(sqlMantenimiento);
            psMant.setInt(1, idMantenimiento);
            psMant.executeUpdate();
            
            java.sql.PreparedStatement psInv = con.prepareStatement(sqlInventario);
            psInv.setString(1, codigoEquipo);
            psInv.executeUpdate();
            
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Mantenimiento marcado como realizado.",
                "Actualización exitosa",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
            
            cargarTablaMantenimientoFiltrada();
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al marcar como realizado: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
                
            }
        }
    }
    
    private void cancelarMantenimiento(int idMantenimiento) {
        java.sql.Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        String sql = "UPDATE mantenimiento SET estado = 'Cancelado' WHERE id_mantenimiento = ?";
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idMantenimiento);
            
            int filas = ps.executeUpdate();
            
            if (filas > 0) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Mantenimiento cancelado correctamente.",
                    "Cancelación exitosa",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
                cargarTablaMantenimientoFiltrada();
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cancelar mantenimiento: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
                
            }
        }
    }
    
    private boolean actualizarMantenimientoBD() {
        String codigoEquipo = cmbCodigoEquipo.getSelectedItem().toString();
        String nombreEquipo = txtNombreEquipoMant.getText().trim();
        String laboratorio = txtLaboratorioModal.getText().trim();
        String tipoMantenimiento = cmbTipoMantModal.getSelectedItem().toString();
        String fechaProgramada = obtenerFechaProgramada();
        String estadoMantenimiento = cmbEstadoMantModal.getSelectedItem().toString();
        String responsable = txtResponsable.getText().trim();
        String observaciones = txtObservacionesMant.getText().trim();

        java.sql.Connection con = ConexionBD.conectar();

        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                addMantenimiento,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        String sql = "UPDATE mantenimiento SET "
                + "codigo_equipo = ?, "
                + "nombre_equipo = ?, "
                + "laboratorio = ?, "
                + "tipo_mantenimiento = ?, "
                + "fecha_programada = ?, "
                + "estado = ?, "
                + "responsable = ?, "
                + "observaciones = ? "
                + "WHERE id_mantenimiento = ?";

        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, codigoEquipo);
            ps.setString(2, nombreEquipo);
            ps.setString(3, laboratorio);
            ps.setString(4, tipoMantenimiento);
            ps.setString(5, fechaProgramada);
            ps.setString(6, estadoMantenimiento);
            ps.setString(7, responsable);
            ps.setString(8, observaciones);
            ps.setInt(9, idMantenimientoSeleccionado);

            int filas = ps.executeUpdate();

            return filas > 0;

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                addMantenimiento,
                "Error al actualizar mantenimiento: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;

        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
            }
        }
    }
    
    private void actualizarEstadoInventarioPorMantenimiento(String codigoEquipo, String estadoMantenimiento) {
        java.sql.Connection con = ConexionBD.conectar();

        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String nuevoEstadoInventario;

        estadoMantenimiento = estadoMantenimiento.trim();

        if (estadoMantenimiento.equals("Pendiente") || estadoMantenimiento.equals("En proceso")) {
            nuevoEstadoInventario = "En mantenimiento";
        } else if (estadoMantenimiento.equals("Realizado")) {
            nuevoEstadoInventario = "Disponible";
        } else if (estadoMantenimiento.equals("Cancelado")) {
            nuevoEstadoInventario = "Disponible";
        } else {
            return;
        }

        String sql = "UPDATE inventario SET estado = ? WHERE codigo = ?";

        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, nuevoEstadoInventario);
            ps.setString(2, codigoEquipo);

            int filas = ps.executeUpdate();

            if (filas == 0) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se encontró el equipo en inventario para actualizar su estado.",
                    "Sin cambios",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
            }

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al actualizar estado del inventario: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );

        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
            }
        }
    }
    
    private boolean existeMantenimiento(String codigoEquipo) {
        java.sql.Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                addMantenimiento,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return true;
        }
        
        String sql = "SELECT COUNT(*) AS total "
            + "FROM mantenimiento "
            + "WHERE codigo_equipo = ? "
            + "AND estado IN ('Pendiente', 'En proceso')";
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, codigoEquipo);
            
            java.sql.ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int total = rs.getInt("total");
                return total > 0;
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                addMantenimiento,
                "Error al validar mantenimiento activo: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return true;
        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
                
            }
        }
        
        return false;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addMantenimiento = new javax.swing.JDialog();
        bodyModal = new javax.swing.JPanel();
        linea = new javax.swing.JPanel();
        lbCodigoEquipo = new javax.swing.JLabel();
        cmbCodigoEquipo = new javax.swing.JComboBox<>();
        lbNombreEquipo = new javax.swing.JLabel();
        txtNombreEquipoMant = new javax.swing.JTextField();
        lbLaboratorioModal = new javax.swing.JLabel();
        txtLaboratorioModal = new javax.swing.JTextField();
        lbTipoMantModal = new javax.swing.JLabel();
        cmbTipoMantModal = new javax.swing.JComboBox<>();
        lbFecha = new javax.swing.JLabel();
        lbResponsable = new javax.swing.JLabel();
        txtResponsable = new javax.swing.JTextField();
        lbEstadoModal = new javax.swing.JLabel();
        cmbEstadoMantModal = new javax.swing.JComboBox<>();
        lbObservaciones = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtObservacionesMant = new javax.swing.JTextArea();
        btnGuardarMant = new javax.swing.JButton();
        btnCancelarMantModal = new javax.swing.JButton();
        lbTituloModal = new javax.swing.JLabel();
        dateFechaProgramada = new com.toedter.calendar.JDateChooser();
        sidebar = new javax.swing.JPanel();
        imgLabSync = new javax.swing.JLabel();
        btnInicio = new javax.swing.JButton();
        btnBitacora = new javax.swing.JButton();
        btnInventario = new javax.swing.JButton();
        btnReservas = new javax.swing.JButton();
        btnReporteFallas = new javax.swing.JButton();
        header = new javax.swing.JPanel();
        lbTitulo = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        cmbTipoMant = new javax.swing.JComboBox<>();
        cmbEstado = new javax.swing.JComboBox<>();
        cmbLaboratorio = new javax.swing.JComboBox<>();
        lbTipoMant = new javax.swing.JLabel();
        lbEstado = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        btnLimpiar = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        body = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaMantenimiento = new javax.swing.JTable();
        btnRegistrar = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btnRealizado = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnExportar = new javax.swing.JButton();

        addMantenimiento.setTitle("Registrar Mantenimiento");
        addMantenimiento.setModal(true);
        addMantenimiento.setResizable(false);

        bodyModal.setBackground(new java.awt.Color(245, 245, 245));
        bodyModal.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        linea.setBackground(new java.awt.Color(8, 173, 141));
        linea.setForeground(new java.awt.Color(8, 173, 141));
        linea.setPreferredSize(new java.awt.Dimension(500, 3));

        javax.swing.GroupLayout lineaLayout = new javax.swing.GroupLayout(linea);
        linea.setLayout(lineaLayout);
        lineaLayout.setHorizontalGroup(
            lineaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 500, Short.MAX_VALUE)
        );
        lineaLayout.setVerticalGroup(
            lineaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 3, Short.MAX_VALUE)
        );

        bodyModal.add(linea, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 65, -1, -1));

        lbCodigoEquipo.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbCodigoEquipo.setForeground(new java.awt.Color(102, 102, 102));
        lbCodigoEquipo.setText("Código Equipo");
        bodyModal.add(lbCodigoEquipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, -1, -1));

        cmbCodigoEquipo.setBackground(new java.awt.Color(255, 255, 255));
        cmbCodigoEquipo.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        cmbCodigoEquipo.setForeground(new java.awt.Color(102, 102, 102));
        cmbCodigoEquipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbCodigoEquipo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbCodigoEquipo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cmbCodigoEquipo.setPreferredSize(new java.awt.Dimension(220, 24));
        cmbCodigoEquipo.addActionListener(this::cmbCodigoEquipoActionPerformed);
        bodyModal.add(cmbCodigoEquipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 120, -1, -1));

        lbNombreEquipo.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbNombreEquipo.setForeground(new java.awt.Color(102, 102, 102));
        lbNombreEquipo.setText("Nombre Equipo");
        bodyModal.add(lbNombreEquipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 90, -1, -1));

        txtNombreEquipoMant.setEditable(false);
        txtNombreEquipoMant.setBackground(new java.awt.Color(255, 255, 255));
        txtNombreEquipoMant.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtNombreEquipoMant.setForeground(new java.awt.Color(51, 51, 51));
        txtNombreEquipoMant.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        txtNombreEquipoMant.setPreferredSize(new java.awt.Dimension(220, 24));
        bodyModal.add(txtNombreEquipoMant, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 120, -1, -1));

        lbLaboratorioModal.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbLaboratorioModal.setForeground(new java.awt.Color(102, 102, 102));
        lbLaboratorioModal.setText("Laboratorio");
        bodyModal.add(lbLaboratorioModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 170, -1, -1));

        txtLaboratorioModal.setEditable(false);
        txtLaboratorioModal.setBackground(new java.awt.Color(255, 255, 255));
        txtLaboratorioModal.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtLaboratorioModal.setForeground(new java.awt.Color(51, 51, 51));
        txtLaboratorioModal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtLaboratorioModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 200, 220, 24));

        lbTipoMantModal.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbTipoMantModal.setForeground(new java.awt.Color(102, 102, 102));
        lbTipoMantModal.setText("Tipo Mantenimiento");
        bodyModal.add(lbTipoMantModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 170, -1, -1));

        cmbTipoMantModal.setBackground(new java.awt.Color(255, 255, 255));
        cmbTipoMantModal.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        cmbTipoMantModal.setForeground(new java.awt.Color(102, 102, 102));
        cmbTipoMantModal.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Selecciona", "Preventivo", "Correctivo", "Actualización de software", "Limpieza", "Retiro de equipo obsoleto", "Otro" }));
        cmbTipoMantModal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbTipoMantModal.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        bodyModal.add(cmbTipoMantModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 200, 220, 24));

        lbFecha.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbFecha.setForeground(new java.awt.Color(102, 102, 102));
        lbFecha.setText("Fecha Programada");
        bodyModal.add(lbFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 250, -1, -1));

        lbResponsable.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbResponsable.setForeground(new java.awt.Color(102, 102, 102));
        lbResponsable.setText("Responsable");
        bodyModal.add(lbResponsable, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 250, -1, -1));

        txtResponsable.setEditable(false);
        txtResponsable.setBackground(new java.awt.Color(255, 255, 255));
        txtResponsable.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtResponsable.setForeground(new java.awt.Color(51, 51, 51));
        txtResponsable.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtResponsable, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 280, 220, 24));

        lbEstadoModal.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbEstadoModal.setForeground(new java.awt.Color(102, 102, 102));
        lbEstadoModal.setText("Estado");
        bodyModal.add(lbEstadoModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 330, -1, -1));

        cmbEstadoMantModal.setBackground(new java.awt.Color(255, 255, 255));
        cmbEstadoMantModal.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        cmbEstadoMantModal.setForeground(new java.awt.Color(102, 102, 102));
        cmbEstadoMantModal.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Selecciona", "Pendiente", "En proceso" }));
        cmbEstadoMantModal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(cmbEstadoMantModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 360, 220, 24));

        lbObservaciones.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbObservaciones.setForeground(new java.awt.Color(102, 102, 102));
        lbObservaciones.setText("Observaciones");
        bodyModal.add(lbObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 330, -1, -1));

        txtObservacionesMant.setColumns(20);
        txtObservacionesMant.setRows(5);
        jScrollPane2.setViewportView(txtObservacionesMant);

        bodyModal.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 355, 230, 80));

        btnGuardarMant.setBackground(new java.awt.Color(8, 173, 141));
        btnGuardarMant.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnGuardarMant.setForeground(new java.awt.Color(255, 255, 255));
        btnGuardarMant.setText("Guardar");
        btnGuardarMant.setBorderPainted(false);
        btnGuardarMant.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnGuardarMant.setFocusPainted(false);
        btnGuardarMant.addActionListener(this::btnGuardarMantActionPerformed);
        bodyModal.add(btnGuardarMant, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 420, 160, 40));

        btnCancelarMantModal.setBackground(new java.awt.Color(108, 117, 125));
        btnCancelarMantModal.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCancelarMantModal.setForeground(new java.awt.Color(255, 255, 255));
        btnCancelarMantModal.setText("Cancelar");
        btnCancelarMantModal.setBorderPainted(false);
        btnCancelarMantModal.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCancelarMantModal.setFocusPainted(false);
        btnCancelarMantModal.addActionListener(this::btnCancelarMantModalActionPerformed);
        bodyModal.add(btnCancelarMantModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 470, 160, 40));

        lbTituloModal.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTituloModal.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloModal.setText("Registrar Mantenimiento");
        bodyModal.add(lbTituloModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 25, -1, -1));

        dateFechaProgramada.setBackground(new java.awt.Color(255, 255, 255));
        dateFechaProgramada.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        dateFechaProgramada.setForeground(new java.awt.Color(51, 51, 51));
        dateFechaProgramada.setDateFormatString("yyyy-MM-dd");
        dateFechaProgramada.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        bodyModal.add(dateFechaProgramada, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 280, 220, 24));

        javax.swing.GroupLayout addMantenimientoLayout = new javax.swing.GroupLayout(addMantenimiento.getContentPane());
        addMantenimiento.getContentPane().setLayout(addMantenimientoLayout);
        addMantenimientoLayout.setHorizontalGroup(
            addMantenimientoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(bodyModal, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
        );
        addMantenimientoLayout.setVerticalGroup(
            addMantenimientoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(bodyModal, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Mantenimiento");
        setResizable(false);

        sidebar.setBackground(new java.awt.Color(0, 171, 132));
        sidebar.setPreferredSize(new java.awt.Dimension(250, 720));
        sidebar.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        imgLabSync.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/labsync_blanco_200.png"))); // NOI18N
        sidebar.add(imgLabSync, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 24, -1, -1));

        btnInicio.setBackground(new java.awt.Color(255, 255, 255));
        btnInicio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnInicio.setForeground(new java.awt.Color(6, 140, 115));
        btnInicio.setText("Inicio");
        btnInicio.setBorderPainted(false);
        btnInicio.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnInicio.setFocusPainted(false);
        btnInicio.setPreferredSize(new java.awt.Dimension(200, 50));
        btnInicio.addActionListener(this::btnInicioActionPerformed);
        sidebar.add(btnInicio, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 290, -1, -1));

        btnBitacora.setBackground(new java.awt.Color(255, 255, 255));
        btnBitacora.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnBitacora.setForeground(new java.awt.Color(6, 140, 115));
        btnBitacora.setText("Bitácora");
        btnBitacora.setBorderPainted(false);
        btnBitacora.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBitacora.setFocusPainted(false);
        btnBitacora.setPreferredSize(new java.awt.Dimension(200, 50));
        btnBitacora.addActionListener(this::btnBitacoraActionPerformed);
        sidebar.add(btnBitacora, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 350, -1, -1));

        btnInventario.setBackground(new java.awt.Color(255, 255, 255));
        btnInventario.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnInventario.setForeground(new java.awt.Color(6, 140, 115));
        btnInventario.setText("Inventario");
        btnInventario.setBorderPainted(false);
        btnInventario.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnInventario.setFocusPainted(false);
        btnInventario.setPreferredSize(new java.awt.Dimension(200, 50));
        btnInventario.addActionListener(this::btnInventarioActionPerformed);
        sidebar.add(btnInventario, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 410, -1, -1));

        btnReservas.setBackground(new java.awt.Color(255, 255, 255));
        btnReservas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnReservas.setForeground(new java.awt.Color(6, 140, 115));
        btnReservas.setText("Reservas");
        btnReservas.setBorderPainted(false);
        btnReservas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnReservas.setFocusPainted(false);
        btnReservas.setPreferredSize(new java.awt.Dimension(200, 50));
        btnReservas.addActionListener(this::btnReservasActionPerformed);
        sidebar.add(btnReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 470, -1, -1));

        btnReporteFallas.setBackground(new java.awt.Color(255, 255, 255));
        btnReporteFallas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnReporteFallas.setForeground(new java.awt.Color(6, 140, 115));
        btnReporteFallas.setText("Reporte de Fallas");
        btnReporteFallas.setBorderPainted(false);
        btnReporteFallas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnReporteFallas.setFocusPainted(false);
        btnReporteFallas.setPreferredSize(new java.awt.Dimension(200, 50));
        btnReporteFallas.addActionListener(this::btnReporteFallasActionPerformed);
        sidebar.add(btnReporteFallas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 530, -1, -1));

        header.setBackground(new java.awt.Color(255, 255, 255));
        header.setPreferredSize(new java.awt.Dimension(1071, 150));
        header.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTitulo.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTitulo.setForeground(new java.awt.Color(102, 102, 102));
        lbTitulo.setText("Control de Mantenimiento");
        header.add(lbTitulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 30, -1, -1));

        txtBuscar.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtBuscar.setPreferredSize(new java.awt.Dimension(240, 30));
        header.add(txtBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 80, -1, -1));

        cmbTipoMant.setBackground(new java.awt.Color(255, 255, 255));
        cmbTipoMant.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbTipoMant.setForeground(new java.awt.Color(102, 102, 102));
        cmbTipoMant.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Preventivo", "Correctivo", "Actualización de software", "Limpieza", "Retiro de equipo obsoleto", "Otro" }));
        cmbTipoMant.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbTipoMant.setMinimumSize(new java.awt.Dimension(170, 170));
        cmbTipoMant.setName(""); // NOI18N
        cmbTipoMant.setPreferredSize(new java.awt.Dimension(170, 30));
        header.add(cmbTipoMant, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 80, -1, -1));

        cmbEstado.setBackground(new java.awt.Color(255, 255, 255));
        cmbEstado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbEstado.setForeground(new java.awt.Color(102, 102, 102));
        cmbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Pendiente", "En proceso", "Realizado", "Cancelado" }));
        cmbEstado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbEstado.setMinimumSize(new java.awt.Dimension(170, 170));
        cmbEstado.setName(""); // NOI18N
        cmbEstado.setPreferredSize(new java.awt.Dimension(170, 30));
        header.add(cmbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 80, -1, -1));

        cmbLaboratorio.setBackground(new java.awt.Color(255, 255, 255));
        cmbLaboratorio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        cmbLaboratorio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "PB-05", "M-11", "M-12", "M-13", "M-14", "M-02", "M-05", "5-06", "5-03" }));
        cmbLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbLaboratorio.setMinimumSize(new java.awt.Dimension(170, 170));
        cmbLaboratorio.setName(""); // NOI18N
        cmbLaboratorio.setPreferredSize(new java.awt.Dimension(170, 30));
        header.add(cmbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 80, -1, -1));

        lbTipoMant.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbTipoMant.setForeground(new java.awt.Color(102, 102, 102));
        lbTipoMant.setText("Tipo de Mantenimiento");
        header.add(lbTipoMant, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 60, -1, -1));

        lbEstado.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbEstado.setForeground(new java.awt.Color(102, 102, 102));
        lbEstado.setText("Estado");
        header.add(lbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 60, -1, -1));

        jLabel1.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(102, 102, 102));
        jLabel1.setText("Laboratorio");
        header.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 60, -1, -1));

        btnLimpiar.setBackground(new java.awt.Color(8, 173, 141));
        btnLimpiar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnLimpiar.setForeground(new java.awt.Color(255, 255, 255));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.setPreferredSize(new java.awt.Dimension(125, 30));
        btnLimpiar.addActionListener(this::btnLimpiarActionPerformed);
        header.add(btnLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(920, 40, -1, -1));

        btnBuscar.setBackground(new java.awt.Color(8, 173, 141));
        btnBuscar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnBuscar.setForeground(new java.awt.Color(255, 255, 255));
        btnBuscar.setText("Buscar");
        btnBuscar.setBorderPainted(false);
        btnBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBuscar.setFocusPainted(false);
        btnBuscar.setPreferredSize(new java.awt.Dimension(125, 30));
        btnBuscar.addActionListener(this::btnBuscarActionPerformed);
        header.add(btnBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(920, 80, -1, -1));

        body.setBackground(new java.awt.Color(204, 204, 204));
        body.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane1.setPreferredSize(new java.awt.Dimension(1000, 420));

        tablaMantenimiento.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        tablaMantenimiento.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Código Equipo", "Nombre Equipo", "Laboratorio", "Tipo Mantenimiento", "Fecha Programada", "Estado", "Responsable", "Observaciones"
            }
        ));
        tablaMantenimiento.setRowHeight(36);
        jScrollPane1.setViewportView(tablaMantenimiento);

        body.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 20, -1, -1));

        btnRegistrar.setBackground(new java.awt.Color(6, 140, 115));
        btnRegistrar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnRegistrar.setForeground(new java.awt.Color(255, 255, 255));
        btnRegistrar.setText("Registrar Mantenimiento");
        btnRegistrar.setBorderPainted(false);
        btnRegistrar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnRegistrar.setFocusPainted(false);
        btnRegistrar.setPreferredSize(new java.awt.Dimension(210, 40));
        btnRegistrar.addActionListener(this::btnRegistrarActionPerformed);
        body.add(btnRegistrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 500, -1, -1));

        btnEditar.setBackground(new java.awt.Color(13, 110, 253));
        btnEditar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnEditar.setForeground(new java.awt.Color(255, 255, 255));
        btnEditar.setText("Editar");
        btnEditar.setBorderPainted(false);
        btnEditar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnEditar.setFocusPainted(false);
        btnEditar.setPreferredSize(new java.awt.Dimension(120, 40));
        btnEditar.addActionListener(this::btnEditarActionPerformed);
        body.add(btnEditar, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 500, -1, -1));

        btnRealizado.setBackground(new java.awt.Color(25, 135, 84));
        btnRealizado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnRealizado.setForeground(new java.awt.Color(255, 255, 255));
        btnRealizado.setText("Marcar Realizado");
        btnRealizado.setBorderPainted(false);
        btnRealizado.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnRealizado.setFocusPainted(false);
        btnRealizado.setPreferredSize(new java.awt.Dimension(170, 40));
        btnRealizado.addActionListener(this::btnRealizadoActionPerformed);
        body.add(btnRealizado, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 500, -1, -1));

        btnCancelar.setBackground(new java.awt.Color(220, 53, 69));
        btnCancelar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCancelar.setForeground(new java.awt.Color(255, 255, 255));
        btnCancelar.setText("Cancelar Mant.");
        btnCancelar.setBorderPainted(false);
        btnCancelar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setPreferredSize(new java.awt.Dimension(130, 40));
        btnCancelar.addActionListener(this::btnCancelarActionPerformed);
        body.add(btnCancelar, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 500, 160, -1));

        btnExportar.setBackground(new java.awt.Color(90, 90, 90));
        btnExportar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnExportar.setForeground(new java.awt.Color(255, 255, 255));
        btnExportar.setText("Exportar");
        btnExportar.setBorderPainted(false);
        btnExportar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnExportar.setFocusPainted(false);
        btnExportar.setPreferredSize(new java.awt.Dimension(130, 40));
        body.add(btnExportar, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 500, -1, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sidebar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(header, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(body, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sidebar, javax.swing.GroupLayout.DEFAULT_SIZE, 731, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(body, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnInicioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInicioActionPerformed
        DashboardLabo ventanaDashboard = new DashboardLabo(nombreUsuario);

        ventanaDashboard.setVisible(true);
        ventanaDashboard.setLocationRelativeTo(null);

        this.dispose();
    }//GEN-LAST:event_btnInicioActionPerformed

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

    private void btnReservasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReservasActionPerformed
        Reserva ventanaReserva = new Reserva(nombreUsuario);
        
        ventanaReserva.setVisible(true);
        ventanaReserva.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReservasActionPerformed

    private void btnRegistrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarActionPerformed
        modoEdicion = false;
        idMantenimientoSeleccionado = 0;

        limpiarCampos();
        cargarCodigosEquipo();

        txtResponsable.setText(nombreUsuario);
        txtResponsable.setEditable(false);

        lbTituloModal.setText("Registrar Mantenimiento");
        btnGuardarMant.setText("Guardar");

        addMantenimiento.pack();
        addMantenimiento.setLocationRelativeTo(this);
        addMantenimiento.setVisible(true);
    }//GEN-LAST:event_btnRegistrarActionPerformed

    private void btnCancelarMantModalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarMantModalActionPerformed
        limpiarCampos();
        addMantenimiento.setVisible(false);
    }//GEN-LAST:event_btnCancelarMantModalActionPerformed

    private void cmbCodigoEquipoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbCodigoEquipoActionPerformed
        cargarDatosEquipoSeleccionado();
    }//GEN-LAST:event_cmbCodigoEquipoActionPerformed

    private void btnGuardarMantActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarMantActionPerformed
        if (!validarCamposMantenimiento()) {
            return;
        }

        if (modoEdicion) {
            boolean actualizado = actualizarMantenimientoBD();

            if (actualizado) {
                String codigoEquipo = cmbCodigoEquipo.getSelectedItem().toString();
                String estadoMantenimiento = cmbEstadoMantModal.getSelectedItem().toString();
                
                actualizarEstadoInventarioPorMantenimiento(codigoEquipo, estadoMantenimiento);
                
                javax.swing.JOptionPane.showMessageDialog(
                    addMantenimiento,
                    "Mantenimiento actualizado correctamente.",
                    "Actualización exitosa",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );

                limpiarCampos();
                addMantenimiento.setVisible(false);
                cargarTablaMantenimientoFiltrada();

                modoEdicion = false;
                idMantenimientoSeleccionado = 0;
                lbTituloModal.setText("Registrar Mantenimiento");
                btnGuardarMant.setText("Guardar");
            }

        } else {
            String codigoEquipo = cmbCodigoEquipo.getSelectedItem().toString();
            
            if (existeMantenimiento(codigoEquipo)) {
                javax.swing.JOptionPane.showMessageDialog(
                    addMantenimiento,
                    "Este equipo ya tiene un mantenimiento pendiente o en proceso.",
                    "Mantenimiento activo",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            boolean guardado = guardarMantenimientoBD();

            if (guardado) {
                String estadoMantenimiento = cmbEstadoMantModal.getSelectedItem().toString();
                
                actualizarEstadoInventarioPorMantenimiento(codigoEquipo, estadoMantenimiento);
                
                javax.swing.JOptionPane.showMessageDialog(
                    addMantenimiento,
                    "Mantenimiento registrado correctamente.",
                    "Registro exitoso",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );

                limpiarCampos();
                addMantenimiento.setVisible(false);
                cargarTablaMantenimientoFiltrada();
            }
    }
    }//GEN-LAST:event_btnGuardarMantActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        cargarTablaMantenimientoFiltrada();
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarFiltrosMantenimiento();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnRealizadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRealizadoActionPerformed
        int filaSeleccionada = tablaMantenimiento.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona un mantenimiento de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int idMantenimiento = Integer.parseInt(tablaMantenimiento.getValueAt(filaSeleccionada, 0).toString());
        String codigoEquipo = tablaMantenimiento.getValueAt(filaSeleccionada, 1).toString();
        
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Deseas marcar este mantenimiento como realizado?",
            "Confirmar selección",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            marcarMantenimientoRealizado(idMantenimiento, codigoEquipo);
        }
    }//GEN-LAST:event_btnRealizadoActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        int filaSeleccionada = tablaMantenimiento.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona un mantenimiento de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int idMantenimiento = Integer.parseInt(tablaMantenimiento.getValueAt(filaSeleccionada, 0).toString());
        
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Deseas cancelar el mantenimiento?",
            "Confirmar cancelación",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        
        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            cancelarMantenimiento(idMantenimiento);
        }
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        int filaSeleccionada = tablaMantenimiento.getSelectedRow();

        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona un mantenimiento para editar.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        modoEdicion = true;

        idMantenimientoSeleccionado = Integer.parseInt(
            tablaMantenimiento.getValueAt(filaSeleccionada, 0).toString()
        );

        cmbCodigoEquipo.setSelectedItem(tablaMantenimiento.getValueAt(filaSeleccionada, 1).toString());
        txtNombreEquipoMant.setText(tablaMantenimiento.getValueAt(filaSeleccionada, 2).toString());
        txtLaboratorioModal.setText(tablaMantenimiento.getValueAt(filaSeleccionada, 3).toString());
        cmbTipoMantModal.setSelectedItem(tablaMantenimiento.getValueAt(filaSeleccionada, 4).toString());
        try {
            String fechaTexto = tablaMantenimiento.getValueAt(filaSeleccionada, 5).toString();
            java.util.Date fecha = new SimpleDateFormat("yyyy-MM-dd").parse(fechaTexto);
            dateFechaProgramada.setDate(fecha);
        } catch (ParseException e) {
            dateFechaProgramada.setDate(null);
        }
        cmbEstadoMantModal.setSelectedItem(tablaMantenimiento.getValueAt(filaSeleccionada, 6).toString());
        txtResponsable.setText(tablaMantenimiento.getValueAt(filaSeleccionada, 7).toString());

        Object observaciones = tablaMantenimiento.getValueAt(filaSeleccionada, 8);
        txtObservacionesMant.setText(observaciones != null ? observaciones.toString() : "");

        lbTituloModal.setText("Editar Mantenimiento");
        btnGuardarMant.setText("Actualizar");
        
        cmbCodigoEquipo.setBackground(Color.WHITE);
        cmbCodigoEquipo.setEnabled(false);
        cmbCodigoEquipo.setForeground(COLOR_TEXTO);

        addMantenimiento.pack();
        addMantenimiento.setLocationRelativeTo(this);
        addMantenimiento.setVisible(true);
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnReporteFallasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReporteFallasActionPerformed
        ReporteFalla ventanaReporteFalla = new ReporteFalla(nombreUsuario);
        
        ventanaReporteFalla.setVisible(true);
        ventanaReporteFalla.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReporteFallasActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new Mantenimiento().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDialog addMantenimiento;
    private javax.swing.JPanel body;
    private javax.swing.JPanel bodyModal;
    private javax.swing.JButton btnBitacora;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnCancelarMantModal;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnGuardarMant;
    private javax.swing.JButton btnInicio;
    private javax.swing.JButton btnInventario;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnRealizado;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JButton btnReporteFallas;
    private javax.swing.JButton btnReservas;
    private javax.swing.JComboBox<String> cmbCodigoEquipo;
    private javax.swing.JComboBox<String> cmbEstado;
    private javax.swing.JComboBox<String> cmbEstadoMantModal;
    private javax.swing.JComboBox<String> cmbLaboratorio;
    private javax.swing.JComboBox<String> cmbTipoMant;
    private javax.swing.JComboBox<String> cmbTipoMantModal;
    private com.toedter.calendar.JDateChooser dateFechaProgramada;
    private javax.swing.JPanel header;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lbCodigoEquipo;
    private javax.swing.JLabel lbEstado;
    private javax.swing.JLabel lbEstadoModal;
    private javax.swing.JLabel lbFecha;
    private javax.swing.JLabel lbLaboratorioModal;
    private javax.swing.JLabel lbNombreEquipo;
    private javax.swing.JLabel lbObservaciones;
    private javax.swing.JLabel lbResponsable;
    private javax.swing.JLabel lbTipoMant;
    private javax.swing.JLabel lbTipoMantModal;
    private javax.swing.JLabel lbTitulo;
    private javax.swing.JLabel lbTituloModal;
    private javax.swing.JPanel linea;
    private javax.swing.JPanel sidebar;
    private javax.swing.JTable tablaMantenimiento;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtLaboratorioModal;
    private javax.swing.JTextField txtNombreEquipoMant;
    private javax.swing.JTextArea txtObservacionesMant;
    private javax.swing.JTextField txtResponsable;
    // End of variables declaration//GEN-END:variables
}
