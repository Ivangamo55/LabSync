package labsync.labsync;

import java.awt.Color;

import java.io.File;
import java.io.FileOutputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Reserva extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Reserva.class.getName());
    private String nombreUsuario;
    private final String PH_BUSCAR = "Usuario, laboratorio, fecha o estado";
    private final Color COLOR_PLACEHOLDER = new Color(150, 150, 150);
    private final Color COLOR_TEXTO = new Color(51, 51, 51);
    
    public Reserva(String nombreRecibido) {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        
        this.nombreUsuario = nombreRecibido;
        
        ponerPlaceholderBuscar();
        cargarTablaReservas();
        
        txtDetalleObservaciones.setLineWrap(true);
        txtDetalleObservaciones.setWrapStyleWord(true);
        txtDetalleObservaciones.setEditable(false);
    }

    public Reserva() {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        
        ponerPlaceholderBuscar();
        cargarTablaReservas();
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
    
    private void cargarTablaReservas() {
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel();

        modelo.addColumn("ID");
        modelo.addColumn("Fecha");
        modelo.addColumn("Maestro");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Actividad");
        modelo.addColumn("Grupo");
        modelo.addColumn("Turno");
        modelo.addColumn("Horario");
        modelo.addColumn("Estado");
        modelo.addColumn("Observaciones");

        String sql = "SELECT id_reserva, "
            + "DATE_FORMAT(fecha, '%Y-%m-%d') AS fecha, "
            + "nombre_solicitante, laboratorio, actividad, grupo, turno, horario, estado, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM reservas "
            + "WHERE estado NOT IN ('Finalizada', 'Cancelada') "
            + "ORDER BY fecha DESC";

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
                Object[] fila = new Object[10];

                fila[0] = rs.getInt("id_reserva");
                fila[1] = rs.getString("fecha");
                fila[2] = rs.getString("nombre_solicitante");
                fila[3] = rs.getString("laboratorio");
                fila[4] = rs.getString("actividad");
                fila[5] = rs.getString("grupo");
                fila[6] = rs.getString("turno");
                fila[7] = rs.getString("horario");
                fila[8] = rs.getString("estado");
                fila[9] = rs.getString("observaciones");

                modelo.addRow(fila);
            }

            tablaReservas.setModel(modelo);
            ocultarColumnaID();

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cargar reservas: " + e.getMessage(),
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
    
    private void cargarTablaReservasFiltrada() {
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel();
        
        modelo.addColumn("ID");
        modelo.addColumn("Fecha");
        modelo.addColumn("Maestro");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Actividad");
        modelo.addColumn("Grupo");
        modelo.addColumn("Turno");
        modelo.addColumn("Horario");
        modelo.addColumn("Estado");
        modelo.addColumn("Observaciones");
        
        String textoBusqueda = txtBuscar.getText().trim();
        
        if (textoBusqueda.equals(PH_BUSCAR)) {
            textoBusqueda = "";
        }
        
        String laboratorioSeleccionado = cmbLaboratorio.getSelectedItem().toString();
        String turnoSeleccionado = cmbTurno.getSelectedItem().toString();
        String estadoSeleccionado = cmbEstado.getSelectedItem().toString();
        
        String sql = "SELECT id_reserva, "
            + "DATE_FORMAT(fecha, '%Y-%m-%d') AS fecha, "
            + "nombre_solicitante, laboratorio, actividad, grupo, turno, horario, estado, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM reservas WHERE 1=1 ";
        
        java.util.ArrayList<String> parametros = new java.util.ArrayList<>();
        
        if (!textoBusqueda.isEmpty()) {
            sql += "AND (nombre_solicitante LIKE ? OR actividad LIKE ? OR grupo LIKE ? OR laboratorio LIKE ?) ";
            String busqueda = "%" + textoBusqueda + "%";
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
        }
        
        if (!laboratorioSeleccionado.equals("Todos")) {
            sql += "AND laboratorio = ? ";
            parametros.add(laboratorioSeleccionado);
        }

        if (!turnoSeleccionado.equals("Todos")) {
            sql += "AND turno = ? ";
            parametros.add(turnoSeleccionado);
        }

        if (!estadoSeleccionado.equals("Todos")) {
            sql += "AND estado = ? ";
            parametros.add(estadoSeleccionado);
        } else {
            sql += "AND estado NOT IN ('Finalizada', 'Cancelada') ";
        }
        
        sql += "ORDER BY fecha DESC";
        
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
                Object[] fila = new Object[10];
                
                fila[0] = rs.getInt("id_reserva");
                fila[1] = rs.getString("fecha");
                fila[2] = rs.getString("nombre_solicitante");
                fila[3] = rs.getString("laboratorio");
                fila[4] = rs.getString("actividad");
                fila[5] = rs.getString("grupo");
                fila[6] = rs.getString("turno");
                fila[7] = rs.getString("horario");
                fila[8] = rs.getString("estado");
                fila[9] = rs.getString("observaciones");
                
                modelo.addRow(fila);
            }
            tablaReservas.setModel(modelo);
            ocultarColumnaID();
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al filtrar reservas: " + e.getMessage(),
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
    
    private void ocultarColumnaID() {
        if (tablaReservas.getColumnModel().getColumnCount() > 0) {
            tablaReservas.getColumnModel().getColumn(0).setMinWidth(0);
            tablaReservas.getColumnModel().getColumn(0).setMaxWidth(0);
            tablaReservas.getColumnModel().getColumn(0).setWidth(0);
        }
    }
    
    private void limpiarFiltrosReservas() {
        txtBuscar.setText(PH_BUSCAR);
        txtBuscar.setForeground(COLOR_PLACEHOLDER);

        cmbLaboratorio.setSelectedIndex(0);
        cmbTurno.setSelectedIndex(0);
        cmbEstado.setSelectedIndex(0);

        cargarTablaReservasFiltrada();
    }
    
    private void limpiarDetalleReserva() {
        txtDetalleID.setText("");
        txtDetalleEstado.setText("");
        txtDetalleFechaRegistro.setText("");
        txtDetalleSolicitante.setText("");
        txtDetalleRol.setText("");
        txtDetalleLaboratorio.setText("");
        txtDetalleTurno.setText("");
        txtDetalleGrupo.setText("");
        txtDetalleGrado.setText("");
        txtDetalleFecha.setText("");
        txtDetalleHorario.setText("");
        txtDetalleCantidad.setText("");
        txtDetalleActividad.setText("");
        txtDetalleObservaciones.setText("");
    }
    
    private void cargarDetalleReserva(int idReserva) {
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
        
        String sql = "SELECT id_reserva, nombre_solicitante, rol_solicitante, laboratorio, "
            + "actividad, grado, grupo, turno, "
            + "DATE_FORMAT(fecha, '%Y-%m-%d') AS fecha, "
            + "horario, cantidad_alumnos, estado, "
            + "IFNULL(observaciones, '') AS observaciones, "
            + "DATE_FORMAT(fecha_registro, '%Y-%m-%d %H:%i:%s') AS fecha_registro "
            + "FROM reservas "
            + "WHERE id_reserva = ?";
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idReserva);
            
            java.sql.ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                txtDetalleID.setText(String.valueOf(rs.getInt("id_reserva")));
                txtDetalleSolicitante.setText(rs.getString("nombre_solicitante"));
                txtDetalleRol.setText(rs.getString("rol_solicitante"));
                txtDetalleLaboratorio.setText(rs.getString("laboratorio"));
                txtDetalleActividad.setText(rs.getString("actividad"));
                txtDetalleGrado.setText(rs.getString("grado"));
                txtDetalleGrupo.setText(rs.getString("grupo"));
                txtDetalleTurno.setText(rs.getString("turno"));
                txtDetalleFecha.setText(rs.getString("fecha"));
                txtDetalleHorario.setText(rs.getString("horario"));
                txtDetalleCantidad.setText(String.valueOf(rs.getInt("cantidad_alumnos")));
                txtDetalleEstado.setText(rs.getString("estado"));
                txtDetalleObservaciones.setText(rs.getString("observaciones"));
                txtDetalleFechaRegistro.setText(rs.getString("fecha_registro"));
                
                detalleReserva.pack();
                detalleReserva.setLocationRelativeTo(this);
                detalleReserva.setVisible(true);
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se encontró la reserva seleccionada.",
                    "Sin resultados",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cargar detalles de la reserva: " + e.getMessage(),
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
    
    private void actualizarEstadoReserva(int idReserva, String nuevoEstado) {
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
        
        String sql = "UPDATE reservas SET estado = ? WHERE id_reserva = ?";
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idReserva);
            
            int filas = ps.executeUpdate();
            
            if (filas > 0) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Reserva actualizada correctamente.",
                    "Actualización exitosa",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
                cargarTablaReservasFiltrada();
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se encontró la reserva seleccionada.",
                    "Sin cambios",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al actualizar la reserva: " + e.getMessage(),
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
    
    private void exportarTablaReservasExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar reservas");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo Excel (*.xlsx)", "xlsx"));
        fileChooser.setSelectedFile(new File("reservas_labsync.xlsx"));

        int seleccion = fileChooser.showSaveDialog(this);

        if (seleccion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = fileChooser.getSelectedFile();

        if (!archivo.getName().toLowerCase().endsWith(".xlsx")) {
            archivo = new File(archivo.getAbsolutePath() + ".xlsx");
        }

        String textoBusqueda = txtBuscar.getText().trim();

        if (textoBusqueda.equals(PH_BUSCAR)) {
            textoBusqueda = "";
        }

        String laboratorioSeleccionado = cmbLaboratorio.getSelectedItem().toString();
        String turnoSeleccionado = cmbTurno.getSelectedItem().toString();
        String estadoSeleccionado = cmbEstado.getSelectedItem().toString();

        String sql = "SELECT "
            + "id_reserva, "
            + "DATE_FORMAT(fecha, '%d/%m/%Y') AS fecha, "
            + "nombre_solicitante, "
            + "rol_solicitante, "
            + "laboratorio, "
            + "actividad, "
            + "grado, "
            + "grupo, "
            + "turno, "
            + "horario, "
            + "cantidad_alumnos, "
            + "estado, "
            + "IFNULL(observaciones, '') AS observaciones, "
            + "DATE_FORMAT(fecha_registro, '%d/%m/%Y %H:%i:%s') AS fecha_registro "
            + "FROM reservas "
            + "WHERE 1=1 ";

        java.util.ArrayList<String> parametros = new java.util.ArrayList<>();

        if (!textoBusqueda.isEmpty()) {
            sql += "AND (nombre_solicitante LIKE ? "
                + "OR rol_solicitante LIKE ? "
                + "OR laboratorio LIKE ? "
                + "OR actividad LIKE ? "
                + "OR grupo LIKE ? "
                + "OR estado LIKE ? "
                + "OR DATE_FORMAT(fecha, '%d/%m/%Y') LIKE ? "
                + "OR DATE_FORMAT(fecha, '%Y-%m-%d') LIKE ?) ";

            String busqueda = "%" + textoBusqueda + "%";

            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
        }

        if (!laboratorioSeleccionado.equals("Todos")) {
            sql += "AND laboratorio = ? ";
            parametros.add(laboratorioSeleccionado);
        }

        if (!turnoSeleccionado.equals("Todos")) {
            sql += "AND turno = ? ";
            parametros.add(turnoSeleccionado);
        }

        if (!estadoSeleccionado.equals("Todos")) {
            sql += "AND estado = ? ";
            parametros.add(estadoSeleccionado);
        } else {
            sql += "AND estado NOT IN ('Finalizada', 'Cancelada') ";
        }

        sql += "ORDER BY fecha DESC";

        java.sql.Connection con = ConexionBD.conectar();

        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexi\u00F3n con la base de datos.",
                "Error de conexi\u00F3n",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try (
            Workbook workbook = new XSSFWorkbook();
            FileOutputStream salida = new FileOutputStream(archivo)
        ) {
            Sheet hoja = workbook.createSheet("Reservas");

            CellStyle estiloTitulo = workbook.createCellStyle();
            Font fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 16);
            estiloTitulo.setFont(fuenteTitulo);
            estiloTitulo.setAlignment(HorizontalAlignment.CENTER);

            CellStyle estiloEncabezado = workbook.createCellStyle();
            Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);
            estiloEncabezado.setBorderTop(BorderStyle.THIN);
            estiloEncabezado.setBorderBottom(BorderStyle.THIN);
            estiloEncabezado.setBorderLeft(BorderStyle.THIN);
            estiloEncabezado.setBorderRight(BorderStyle.THIN);

            CellStyle estiloCelda = workbook.createCellStyle();
            estiloCelda.setBorderTop(BorderStyle.THIN);
            estiloCelda.setBorderBottom(BorderStyle.THIN);
            estiloCelda.setBorderLeft(BorderStyle.THIN);
            estiloCelda.setBorderRight(BorderStyle.THIN);

            Row filaTitulo = hoja.createRow(0);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue("Reporte de Reservas - LabSync");
            celdaTitulo.setCellStyle(estiloTitulo);

            hoja.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 12));

            Row filaEncabezado = hoja.createRow(2);

            String[] encabezados = {
                "Fecha",
                "Solicitante",
                "Rol",
                "Laboratorio",
                "Actividad",
                "Grado",
                "Grupo",
                "Turno",
                "Horario",
                "Cantidad Alumnos",
                "Estado",
                "Observaciones",
                "Fecha Registro"
            };

            for (int i = 0; i < encabezados.length; i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(encabezados[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            java.sql.PreparedStatement ps = con.prepareStatement(sql);

            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }

            java.sql.ResultSet rs = ps.executeQuery();

            int filaExcel = 3;
            int totalRegistros = 0;

            while (rs.next()) {
                Row fila = hoja.createRow(filaExcel);

                Object[] datos = {
                    rs.getString("fecha"),
                    rs.getString("nombre_solicitante"),
                    rs.getString("rol_solicitante"),
                    rs.getString("laboratorio"),
                    rs.getString("actividad"),
                    rs.getString("grado"),
                    rs.getString("grupo"),
                    rs.getString("turno"),
                    rs.getString("horario"),
                    rs.getInt("cantidad_alumnos"),
                    rs.getString("estado"),
                    rs.getString("observaciones"),
                    rs.getString("fecha_registro")
                };

                for (int col = 0; col < datos.length; col++) {
                    Cell celda = fila.createCell(col);
                    celda.setCellValue(datos[col] != null ? datos[col].toString() : "");
                    celda.setCellStyle(estiloCelda);
                }

                filaExcel++;
                totalRegistros++;
            }

            if (totalRegistros == 0) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No hay datos para exportar con los filtros actuales.",
                    "Sin datos",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            hoja.createFreezePane(0, 3);
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, filaExcel - 1, 0, encabezados.length - 1));

            for (int col = 0; col < encabezados.length; col++) {
                hoja.autoSizeColumn(col);
            }

            workbook.write(salida);

            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Las reservas se exportaron correctamente.",
                "Exportaci\u00F3n exitosa",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al exportar reservas: " + e.getMessage(),
                "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        detalleReserva = new javax.swing.JDialog();
        bodyModal = new javax.swing.JPanel();
        lbTituloModal = new javax.swing.JLabel();
        linea = new javax.swing.JPanel();
        lbIDReserva = new javax.swing.JLabel();
        txtDetalleID = new javax.swing.JTextField();
        lbDetalleEstado = new javax.swing.JLabel();
        txtDetalleEstado = new javax.swing.JTextField();
        lbDetalleFechaRegistro = new javax.swing.JLabel();
        txtDetalleFechaRegistro = new javax.swing.JTextField();
        lbDetalleSolicitante = new javax.swing.JLabel();
        txtDetalleSolicitante = new javax.swing.JTextField();
        lbDetalleRol = new javax.swing.JLabel();
        txtDetalleRol = new javax.swing.JTextField();
        lbDetalleLaboratorio = new javax.swing.JLabel();
        txtDetalleLaboratorio = new javax.swing.JTextField();
        lbDetalleTurno = new javax.swing.JLabel();
        txtDetalleTurno = new javax.swing.JTextField();
        lbDetalleGrupo = new javax.swing.JLabel();
        txtDetalleGrupo = new javax.swing.JTextField();
        lbDetalleGrado = new javax.swing.JLabel();
        txtDetalleGrado = new javax.swing.JTextField();
        lbDetalleFecha = new javax.swing.JLabel();
        txtDetalleFecha = new javax.swing.JTextField();
        lbDetalleHorario = new javax.swing.JLabel();
        txtDetalleHorario = new javax.swing.JTextField();
        lbDetalleCantidad = new javax.swing.JLabel();
        txtDetalleCantidad = new javax.swing.JTextField();
        lbDetalleActividad = new javax.swing.JLabel();
        txtDetalleActividad = new javax.swing.JTextField();
        lbDetalleObservaciones = new javax.swing.JLabel();
        scrollDetalleObservaciones = new javax.swing.JScrollPane();
        txtDetalleObservaciones = new javax.swing.JTextArea();
        btnCerrarDetalle = new javax.swing.JButton();
        sidebar = new javax.swing.JPanel();
        imgLabSync = new javax.swing.JLabel();
        btnInicio = new javax.swing.JButton();
        btnBitacora = new javax.swing.JButton();
        btnInventario = new javax.swing.JButton();
        btnMantenimiento = new javax.swing.JButton();
        btnReporteFallas = new javax.swing.JButton();
        header = new javax.swing.JPanel();
        lbTitulo = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        cmbLaboratorio = new javax.swing.JComboBox<>();
        lbLaboratorio = new javax.swing.JLabel();
        cmbTurno = new javax.swing.JComboBox<>();
        lbTurno = new javax.swing.JLabel();
        cmbEstado = new javax.swing.JComboBox<>();
        lbEstado = new javax.swing.JLabel();
        btnLimpiar = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        body = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaReservas = new javax.swing.JTable();
        btnVerDetalles = new javax.swing.JButton();
        btnAprobar = new javax.swing.JButton();
        btnRechazar = new javax.swing.JButton();
        btnFinalizar = new javax.swing.JButton();
        btnExportar = new javax.swing.JButton();

        detalleReserva.setTitle("Ver Detalles");
        detalleReserva.setLocationByPlatform(true);
        detalleReserva.setMinimumSize(new java.awt.Dimension(630, 660));
        detalleReserva.setModal(true);
        detalleReserva.setResizable(false);

        bodyModal.setBackground(new java.awt.Color(245, 245, 245));
        bodyModal.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloModal.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTituloModal.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloModal.setText("Detalles de Reserva");
        bodyModal.add(lbTituloModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 25, -1, -1));

        linea.setBackground(new java.awt.Color(8, 173, 141));
        linea.setForeground(new java.awt.Color(8, 173, 141));

        javax.swing.GroupLayout lineaLayout = new javax.swing.GroupLayout(linea);
        linea.setLayout(lineaLayout);
        lineaLayout.setHorizontalGroup(
            lineaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 560, Short.MAX_VALUE)
        );
        lineaLayout.setVerticalGroup(
            lineaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 3, Short.MAX_VALUE)
        );

        bodyModal.add(linea, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 65, 560, 3));

        lbIDReserva.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbIDReserva.setForeground(new java.awt.Color(102, 102, 102));
        lbIDReserva.setText("ID Reserva");
        bodyModal.add(lbIDReserva, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, -1, -1));

        txtDetalleID.setEditable(false);
        txtDetalleID.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleID.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleID.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleID.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleID, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 115, 110, 24));

        lbDetalleEstado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleEstado.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleEstado.setText("Estado");
        bodyModal.add(lbDetalleEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 90, -1, -1));

        txtDetalleEstado.setEditable(false);
        txtDetalleEstado.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleEstado.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleEstado.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleEstado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 115, 160, 24));

        lbDetalleFechaRegistro.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleFechaRegistro.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleFechaRegistro.setText("Fecha Registro");
        bodyModal.add(lbDetalleFechaRegistro, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 90, -1, -1));

        txtDetalleFechaRegistro.setEditable(false);
        txtDetalleFechaRegistro.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleFechaRegistro.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleFechaRegistro.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleFechaRegistro.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleFechaRegistro, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 115, 230, 24));

        lbDetalleSolicitante.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleSolicitante.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleSolicitante.setText("Solicitante");
        bodyModal.add(lbDetalleSolicitante, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 160, -1, -1));

        txtDetalleSolicitante.setEditable(false);
        txtDetalleSolicitante.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleSolicitante.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleSolicitante.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleSolicitante.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        txtDetalleSolicitante.addActionListener(this::txtDetalleSolicitanteActionPerformed);
        bodyModal.add(txtDetalleSolicitante, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 180, 270, 24));

        lbDetalleRol.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleRol.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleRol.setText("Rol");
        bodyModal.add(lbDetalleRol, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 160, -1, -1));

        txtDetalleRol.setEditable(false);
        txtDetalleRol.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleRol.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleRol.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleRol.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleRol, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 180, 260, 24));

        lbDetalleLaboratorio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleLaboratorio.setText("Laboratorio");
        bodyModal.add(lbDetalleLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 230, -1, -1));

        txtDetalleLaboratorio.setEditable(false);
        txtDetalleLaboratorio.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleLaboratorio.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleLaboratorio.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 250, 160, 24));

        lbDetalleTurno.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleTurno.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleTurno.setText("Turno");
        bodyModal.add(lbDetalleTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 230, -1, -1));

        txtDetalleTurno.setEditable(false);
        txtDetalleTurno.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleTurno.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleTurno.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleTurno.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 250, 160, 24));

        lbDetalleGrupo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleGrupo.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleGrupo.setText("Grupo");
        bodyModal.add(lbDetalleGrupo, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 230, -1, -1));

        txtDetalleGrupo.setEditable(false);
        txtDetalleGrupo.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleGrupo.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleGrupo.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleGrupo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleGrupo, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 250, 180, 24));

        lbDetalleGrado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleGrado.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleGrado.setText("Grado");
        bodyModal.add(lbDetalleGrado, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 300, -1, -1));

        txtDetalleGrado.setEditable(false);
        txtDetalleGrado.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleGrado.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleGrado.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleGrado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleGrado, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 320, 110, 24));

        lbDetalleFecha.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleFecha.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleFecha.setText("Fecha");
        bodyModal.add(lbDetalleFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 300, -1, -1));

        txtDetalleFecha.setEditable(false);
        txtDetalleFecha.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleFecha.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleFecha.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleFecha.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 320, 160, 24));

        lbDetalleHorario.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleHorario.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleHorario.setText("Horario");
        bodyModal.add(lbDetalleHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 300, -1, -1));

        txtDetalleHorario.setEditable(false);
        txtDetalleHorario.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleHorario.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleHorario.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleHorario.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 320, 230, 24));

        lbDetalleCantidad.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleCantidad.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleCantidad.setText("Cantidad de Alumnos");
        bodyModal.add(lbDetalleCantidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 370, -1, -1));

        txtDetalleCantidad.setEditable(false);
        txtDetalleCantidad.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleCantidad.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleCantidad.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleCantidad.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleCantidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 390, 160, 24));

        lbDetalleActividad.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleActividad.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleActividad.setText("Actividad");
        bodyModal.add(lbDetalleActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 370, -1, -1));

        txtDetalleActividad.setEditable(false);
        txtDetalleActividad.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleActividad.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleActividad.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleActividad.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 390, 370, 24));

        lbDetalleObservaciones.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleObservaciones.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleObservaciones.setText("Observaciones");
        bodyModal.add(lbDetalleObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 440, -1, -1));

        txtDetalleObservaciones.setEditable(false);
        txtDetalleObservaciones.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleObservaciones.setColumns(20);
        txtDetalleObservaciones.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleObservaciones.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleObservaciones.setLineWrap(true);
        txtDetalleObservaciones.setRows(5);
        txtDetalleObservaciones.setWrapStyleWord(true);
        txtDetalleObservaciones.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        scrollDetalleObservaciones.setViewportView(txtDetalleObservaciones);

        bodyModal.add(scrollDetalleObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 460, 560, 80));

        btnCerrarDetalle.setBackground(new java.awt.Color(108, 117, 125));
        btnCerrarDetalle.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCerrarDetalle.setForeground(new java.awt.Color(255, 255, 255));
        btnCerrarDetalle.setText("Cerrar");
        btnCerrarDetalle.setBorderPainted(false);
        btnCerrarDetalle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCerrarDetalle.setFocusPainted(false);
        btnCerrarDetalle.addActionListener(this::btnCerrarDetalleActionPerformed);
        bodyModal.add(btnCerrarDetalle, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 560, 180, 40));

        javax.swing.GroupLayout detalleReservaLayout = new javax.swing.GroupLayout(detalleReserva.getContentPane());
        detalleReserva.getContentPane().setLayout(detalleReservaLayout);
        detalleReservaLayout.setHorizontalGroup(
            detalleReservaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(bodyModal, javax.swing.GroupLayout.DEFAULT_SIZE, 630, Short.MAX_VALUE)
        );
        detalleReservaLayout.setVerticalGroup(
            detalleReservaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detalleReservaLayout.createSequentialGroup()
                .addComponent(bodyModal, javax.swing.GroupLayout.PREFERRED_SIZE, 660, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Reservas");
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

        btnMantenimiento.setBackground(new java.awt.Color(255, 255, 255));
        btnMantenimiento.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnMantenimiento.setForeground(new java.awt.Color(6, 140, 115));
        btnMantenimiento.setText("Mantenimiento");
        btnMantenimiento.setBorderPainted(false);
        btnMantenimiento.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnMantenimiento.setFocusPainted(false);
        btnMantenimiento.setPreferredSize(new java.awt.Dimension(200, 50));
        btnMantenimiento.addActionListener(this::btnMantenimientoActionPerformed);
        sidebar.add(btnMantenimiento, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 470, -1, -1));

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
        lbTitulo.setText("Control de Reservas");
        header.add(lbTitulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 30, -1, -1));

        txtBuscar.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        header.add(txtBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 80, 240, 30));

        cmbLaboratorio.setBackground(new java.awt.Color(255, 255, 255));
        cmbLaboratorio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        cmbLaboratorio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "PB-05", "M-11", "M-12", "M-13", "M-14", "M-02", "M-05", "5-06", "5-03" }));
        cmbLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        header.add(cmbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 80, 170, 30));

        lbLaboratorio.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        lbLaboratorio.setText("Laboratorio");
        header.add(lbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 60, -1, -1));

        cmbTurno.setBackground(new java.awt.Color(255, 255, 255));
        cmbTurno.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbTurno.setForeground(new java.awt.Color(102, 102, 102));
        cmbTurno.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Matutino", "Vespertino" }));
        cmbTurno.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        header.add(cmbTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 80, 150, 30));

        lbTurno.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbTurno.setForeground(new java.awt.Color(102, 102, 102));
        lbTurno.setText("Turno");
        header.add(lbTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 60, -1, -1));

        cmbEstado.setBackground(new java.awt.Color(255, 255, 255));
        cmbEstado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbEstado.setForeground(new java.awt.Color(102, 102, 102));
        cmbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Pendiente", "Aprobada", "Rechazada", "Cancelada", "Finalizada" }));
        cmbEstado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        header.add(cmbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 80, 150, 30));

        lbEstado.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbEstado.setForeground(new java.awt.Color(102, 102, 102));
        lbEstado.setText("Estado");
        header.add(lbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 60, -1, -1));

        btnLimpiar.setBackground(new java.awt.Color(8, 173, 141));
        btnLimpiar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnLimpiar.setForeground(new java.awt.Color(255, 255, 255));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.addActionListener(this::btnLimpiarActionPerformed);
        header.add(btnLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 40, 125, 30));

        btnBuscar.setBackground(new java.awt.Color(8, 173, 141));
        btnBuscar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnBuscar.setForeground(new java.awt.Color(255, 255, 255));
        btnBuscar.setText("Buscar");
        btnBuscar.setBorderPainted(false);
        btnBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBuscar.setFocusPainted(false);
        btnBuscar.addActionListener(this::btnBuscarActionPerformed);
        header.add(btnBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 80, 125, 30));

        body.setBackground(new java.awt.Color(204, 204, 204));
        body.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tablaReservas.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        tablaReservas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Fecha", "Maestro", "Laboratorio", "Actividad", "Grupo", "Turno", "Horario", "Estado", "Observaciones"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaReservas.setRowHeight(36);
        jScrollPane1.setViewportView(tablaReservas);

        body.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 20, 1000, 420));

        btnVerDetalles.setBackground(new java.awt.Color(6, 140, 115));
        btnVerDetalles.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnVerDetalles.setForeground(new java.awt.Color(255, 255, 255));
        btnVerDetalles.setText("Ver Detalles");
        btnVerDetalles.setBorderPainted(false);
        btnVerDetalles.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnVerDetalles.setFocusPainted(false);
        btnVerDetalles.addActionListener(this::btnVerDetallesActionPerformed);
        body.add(btnVerDetalles, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 500, 180, 40));

        btnAprobar.setBackground(new java.awt.Color(13, 110, 253));
        btnAprobar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnAprobar.setForeground(new java.awt.Color(255, 255, 255));
        btnAprobar.setText("Aprobar");
        btnAprobar.setBorderPainted(false);
        btnAprobar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnAprobar.setFocusPainted(false);
        btnAprobar.addActionListener(this::btnAprobarActionPerformed);
        body.add(btnAprobar, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 500, 120, 40));

        btnRechazar.setBackground(new java.awt.Color(25, 135, 84));
        btnRechazar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnRechazar.setForeground(new java.awt.Color(255, 255, 255));
        btnRechazar.setText("Rechazar");
        btnRechazar.setBorderPainted(false);
        btnRechazar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnRechazar.setFocusPainted(false);
        btnRechazar.addActionListener(this::btnRechazarActionPerformed);
        body.add(btnRechazar, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 500, 130, 40));

        btnFinalizar.setBackground(new java.awt.Color(220, 53, 69));
        btnFinalizar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnFinalizar.setForeground(new java.awt.Color(255, 255, 255));
        btnFinalizar.setText("Finalizar");
        btnFinalizar.setBorderPainted(false);
        btnFinalizar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnFinalizar.setFocusPainted(false);
        btnFinalizar.addActionListener(this::btnFinalizarActionPerformed);
        body.add(btnFinalizar, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 500, 170, 40));

        btnExportar.setBackground(new java.awt.Color(90, 90, 90));
        btnExportar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnExportar.setForeground(new java.awt.Color(255, 255, 255));
        btnExportar.setText("Exportar");
        btnExportar.setBorderPainted(false);
        btnExportar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnExportar.setFocusPainted(false);
        btnExportar.addActionListener(this::btnExportarActionPerformed);
        body.add(btnExportar, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 500, 130, 40));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sidebar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(header, javax.swing.GroupLayout.DEFAULT_SIZE, 1083, Short.MAX_VALUE)
                    .addComponent(body, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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

    private void btnMantenimientoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMantenimientoActionPerformed
        Mantenimiento ventanaMant = new Mantenimiento(nombreUsuario);
        
        ventanaMant.setVisible(true);
        ventanaMant.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnMantenimientoActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        cargarTablaReservasFiltrada();
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarFiltrosReservas();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnVerDetallesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerDetallesActionPerformed
        int filaSeleccionada = tablaReservas.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona una reserva de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int filaModelo = tablaReservas.convertRowIndexToModel(filaSeleccionada);
        
        int idReserva = Integer.parseInt(
            tablaReservas.getModel().getValueAt(filaModelo, 0).toString()
        );
        
        cargarDetalleReserva(idReserva);
    }//GEN-LAST:event_btnVerDetallesActionPerformed

    private void txtDetalleSolicitanteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDetalleSolicitanteActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDetalleSolicitanteActionPerformed

    private void btnCerrarDetalleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarDetalleActionPerformed
        detalleReserva.setVisible(false);
        limpiarDetalleReserva();
    }//GEN-LAST:event_btnCerrarDetalleActionPerformed

    private void btnAprobarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAprobarActionPerformed
        int filaSeleccionada = tablaReservas.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona una reserva de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int filaModelo = tablaReservas.convertRowIndexToModel(filaSeleccionada);
        
        int idReserva = Integer.parseInt(
            tablaReservas.getModel().getValueAt(filaModelo, 0).toString()
        );
        
        String estadoActual = tablaReservas.getModel().getValueAt(filaModelo, 8).toString();
        
        if (!estadoActual.equals("Pendiente")) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Solo se pueden aprobar reservas que estén en estado Pendiente.",
                "Acción no permitida",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Deseas aprobar esta reserva?",
            "Confirmar aprobación",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            actualizarEstadoReserva(idReserva, "Aprobada");
        }
    }//GEN-LAST:event_btnAprobarActionPerformed

    private void btnRechazarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRechazarActionPerformed
        int filaSeleccionada = tablaReservas.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona una reserva de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return; 
        }
        
        int filaModelo = tablaReservas.convertRowIndexToModel(filaSeleccionada);
        
        int idReserva = Integer.parseInt(
            tablaReservas.getModel().getValueAt(filaModelo, 0).toString()
        );
        
        String estadoActual = tablaReservas.getModel().getValueAt(filaModelo, 8).toString();
        
        if (!estadoActual.equals("Pendiente")) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Solo se pueden rechazar reservas que estén en estado Pendiente.",
                "Acción no permitida",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Deseas rechazar esta reserva?",
            "Confirmar rechazo",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        
        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            actualizarEstadoReserva(idReserva, "Rechazada");
        }
    }//GEN-LAST:event_btnRechazarActionPerformed

    private void btnFinalizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFinalizarActionPerformed
        int filaSeleccionada = tablaReservas.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona una reserva de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return; 
        }
        
        int filaModelo = tablaReservas.convertRowIndexToModel(filaSeleccionada);
        
        int idReserva = Integer.parseInt(
            tablaReservas.getModel().getValueAt(filaModelo, 0).toString()
        );
        
        String estadoActual = tablaReservas.getModel().getValueAt(filaModelo, 8).toString();
        
        if (!estadoActual.equals("Aprobada")) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Solo se pueden finalizar reservas que estén en estado Aprobada.",
                "Acción no permitida",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Deseas finalizar esta reserva?",
            "Confirmar finalización",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        
        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            actualizarEstadoReserva(idReserva, "Finalizada");
        }
    }//GEN-LAST:event_btnFinalizarActionPerformed

    private void btnReporteFallasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReporteFallasActionPerformed
        ReporteFalla ventanaReporte = new ReporteFalla(nombreUsuario);
        
        ventanaReporte.setVisible(true);
        ventanaReporte.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReporteFallasActionPerformed

    private void btnExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarActionPerformed
        exportarTablaReservasExcel();
    }//GEN-LAST:event_btnExportarActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
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
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new Reserva().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel body;
    private javax.swing.JPanel bodyModal;
    private javax.swing.JButton btnAprobar;
    private javax.swing.JButton btnBitacora;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCerrarDetalle;
    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnFinalizar;
    private javax.swing.JButton btnInicio;
    private javax.swing.JButton btnInventario;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnMantenimiento;
    private javax.swing.JButton btnRechazar;
    private javax.swing.JButton btnReporteFallas;
    private javax.swing.JButton btnVerDetalles;
    private javax.swing.JComboBox<String> cmbEstado;
    private javax.swing.JComboBox<String> cmbLaboratorio;
    private javax.swing.JComboBox<String> cmbTurno;
    private javax.swing.JDialog detalleReserva;
    private javax.swing.JPanel header;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbDetalleActividad;
    private javax.swing.JLabel lbDetalleCantidad;
    private javax.swing.JLabel lbDetalleEstado;
    private javax.swing.JLabel lbDetalleFecha;
    private javax.swing.JLabel lbDetalleFechaRegistro;
    private javax.swing.JLabel lbDetalleGrado;
    private javax.swing.JLabel lbDetalleGrupo;
    private javax.swing.JLabel lbDetalleHorario;
    private javax.swing.JLabel lbDetalleLaboratorio;
    private javax.swing.JLabel lbDetalleObservaciones;
    private javax.swing.JLabel lbDetalleRol;
    private javax.swing.JLabel lbDetalleSolicitante;
    private javax.swing.JLabel lbDetalleTurno;
    private javax.swing.JLabel lbEstado;
    private javax.swing.JLabel lbIDReserva;
    private javax.swing.JLabel lbLaboratorio;
    private javax.swing.JLabel lbTitulo;
    private javax.swing.JLabel lbTituloModal;
    private javax.swing.JLabel lbTurno;
    private javax.swing.JPanel linea;
    private javax.swing.JScrollPane scrollDetalleObservaciones;
    private javax.swing.JPanel sidebar;
    private javax.swing.JTable tablaReservas;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtDetalleActividad;
    private javax.swing.JTextField txtDetalleCantidad;
    private javax.swing.JTextField txtDetalleEstado;
    private javax.swing.JTextField txtDetalleFecha;
    private javax.swing.JTextField txtDetalleFechaRegistro;
    private javax.swing.JTextField txtDetalleGrado;
    private javax.swing.JTextField txtDetalleGrupo;
    private javax.swing.JTextField txtDetalleHorario;
    private javax.swing.JTextField txtDetalleID;
    private javax.swing.JTextField txtDetalleLaboratorio;
    private javax.swing.JTextArea txtDetalleObservaciones;
    private javax.swing.JTextField txtDetalleRol;
    private javax.swing.JTextField txtDetalleSolicitante;
    private javax.swing.JTextField txtDetalleTurno;
    // End of variables declaration//GEN-END:variables
}
