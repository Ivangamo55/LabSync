package labsync.labsync;

import java.awt.Color;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;

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

public class ReporteFalla extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ReporteFalla.class.getName());
    String nombreUsuario;
    private final Color COLOR_PLACEHOLDER = new Color(150, 150, 150);
    private final Color COLOR_TEXTO = new Color(51, 51, 51);
    private final String PH_BUSCAR = "Código, equipo, laboratorio o falla";

    public ReporteFalla(String nombreRecibido) {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        
        this.nombreUsuario = nombreRecibido;
        
        txtDetalleDescripcion.setLineWrap(true);
        txtDetalleDescripcion.setWrapStyleWord(true);
        txtDetalleDescripcion.setEditable(false);

        txtDetalleObservaciones.setLineWrap(true);
        txtDetalleObservaciones.setWrapStyleWord(true);
        txtDetalleObservaciones.setEditable(false);
        
        ponerPlaceholderBuscar();
        cargarTablaReportes();
    }
    
    public ReporteFalla() {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        
        txtDetalleDescripcion.setLineWrap(true);
        txtDetalleDescripcion.setWrapStyleWord(true);
        txtDetalleDescripcion.setEditable(false);

        txtDetalleObservaciones.setLineWrap(true);
        txtDetalleObservaciones.setWrapStyleWord(true);
        txtDetalleObservaciones.setEditable(false);
        
        ponerPlaceholderBuscar();
        cargarTablaReportes();
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
    
    private void limpiarFiltrosReporteFalla() {
        txtBuscar.setText(PH_BUSCAR);
        txtBuscar.setForeground(COLOR_PLACEHOLDER);

        cmbLaboratorio.setSelectedIndex(0);
        cmbEstado.setSelectedIndex(0);
        cmbPrioridad.setSelectedIndex(0);

        cargarTablaReportesFiltrada();
    }

    private void cargarTablaReportes() {
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel();
        Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        modelo.addColumn("ID");
        modelo.addColumn("Fecha");
        modelo.addColumn("Código Equipo");
        modelo.addColumn("Nombre Equipo");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Falla");
        modelo.addColumn("Prioridad");
        modelo.addColumn("Estado");
        modelo.addColumn("Reportado Por");
        modelo.addColumn("Observaciones");
        
        String sql = "SELECT id_falla, "
            + "DATE_FORMAT(fecha_reporte, '%Y-%m-%d %H:%i:%s') AS fecha_reporte, "
            + "codigo_equipo, nombre_equipo, laboratorio, descripcion_falla, "
            + "prioridad, estado, reportado_por, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM reporte_fallas "
            + "WHERE estado NOT IN ('Atendida', 'Cancelada') "
            + "ORDER BY fecha_reporte DESC";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] fila = new Object[10];
                
                fila[0] = rs.getInt("id_falla");
                fila[1] = rs.getString("fecha_reporte");
                fila[2] = rs.getString("codigo_equipo");
                fila[3] = rs.getString("nombre_equipo");
                fila[4] = rs.getString("laboratorio");
                fila[5] = rs.getString("descripcion_falla");
                fila[6] = rs.getString("prioridad");
                fila[7] = rs.getString("estado");
                fila[8] = rs.getString("reportado_por");
                fila[9] = rs.getString("observaciones");
                
                modelo.addRow(fila);
            }
            
            tablaReportes.setModel(modelo);
            ocultarColumnaID();
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cargar reportes de fallas: " + e.getMessage(),
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
    
    private void ocultarColumnaID() {
        if (tablaReportes.getColumnModel().getColumnCount() > 0) {
            tablaReportes.getColumnModel().getColumn(0).setMinWidth(0);
            tablaReportes.getColumnModel().getColumn(0).setMaxWidth(0);
            tablaReportes.getColumnModel().getColumn(0).setWidth(0);
        }
    }
    
    private void cargarTablaReportesFiltrada() {
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel();
        Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        modelo.addColumn("ID");
        modelo.addColumn("Fecha");
        modelo.addColumn("Código Equipo");
        modelo.addColumn("Nombre Equipo");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Falla");
        modelo.addColumn("Prioridad");
        modelo.addColumn("Estado");
        modelo.addColumn("Reportado Por");
        modelo.addColumn("Observaciones");
        
        String textoBusqueda = txtBuscar.getText().trim();
        String laboratorioSeleccionado = cmbLaboratorio.getSelectedItem().toString();
        String estadoSeleccionado = cmbEstado.getSelectedItem().toString();
        String prioridadSeleccionada = cmbPrioridad.getSelectedItem().toString();
        
        if (textoBusqueda.equals(PH_BUSCAR)) {
            textoBusqueda = "";
        }
        
        String sql = "SELECT id_falla, "
            + "DATE_FORMAT(fecha_reporte, '%Y-%m-%d %H:%i:%s') AS fecha_reporte, "
            + "codigo_equipo, nombre_equipo, laboratorio, descripcion_falla, "
            + "prioridad, estado, reportado_por, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM reporte_fallas WHERE 1=1 ";
        
        java.util.ArrayList<String> parametros = new java.util.ArrayList<>();
        
        if (!textoBusqueda.isEmpty()) {
            sql += "AND (codigo_equipo LIKE ? OR nombre_equipo LIKE ? OR laboratorio LIKE ? "
                + "OR reportado_por LIKE ? OR descripcion_falla LIKE ?) ";

            String busqueda = "%" + textoBusqueda + "%";
            
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
        }
        
        if (!laboratorioSeleccionado.equals("Todos")) {
            sql += "AND laboratorio = ?";
            parametros.add(laboratorioSeleccionado);
        }
        
        if (!estadoSeleccionado.equals("Todos")) {
            sql += "AND estado = ?";
            parametros.add(estadoSeleccionado);
        } else {
            sql += "AND estado NOT IN ('Atendida', 'Cancelada')";
        }
        
        if (!prioridadSeleccionada.equals("Todos")) {
            sql += "AND prioridad = ?";
            parametros.add(prioridadSeleccionada);
        }
        
        sql += "ORDER BY fecha_reporte DESC";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            
            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] fila = new Object[10];
                
                fila[0] = rs.getInt("id_falla");
                fila[1] = rs.getString("fecha_reporte");
                fila[2] = rs.getString("codigo_equipo");
                fila[3] = rs.getString("nombre_equipo");
                fila[4] = rs.getString("laboratorio");
                fila[5] = rs.getString("descripcion_falla");
                fila[6] = rs.getString("prioridad");
                fila[7] = rs.getString("estado");
                fila[8] = rs.getString("reportado_por");
                fila[9] = rs.getString("observaciones");
                
                modelo.addRow(fila);
            }
            
            tablaReportes.setModel(modelo);
            ocultarColumnaID();
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al filtrar reportes: " + e.getMessage(),
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
    
    private void cargarDetalleReportesFalla(int idFalla) {
        Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        String sql = "SELECT id_falla, codigo_equipo, nombre_equipo, laboratorio, "
            + "reportado_por, rol_reportante, descripcion_falla, prioridad, estado, "
            + "DATE_FORMAT(fecha_reporte, '%Y-%m-%d %H:%i:%s') AS fecha_reporte, "
            + "IFNULL(DATE_FORMAT(fecha_revision, '%Y-%m-%d'), 'Sin revisión') AS fecha_revision, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM reporte_fallas "
            + "WHERE id_falla = ?";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idFalla);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                txtDetalleID.setText(String.valueOf(rs.getInt("id_falla")));
                txtDetalleEstado.setText(rs.getString("estado"));
                txtDetalleFecha.setText(rs.getString("fecha_reporte"));

                txtDetalleCodigo.setText(rs.getString("codigo_equipo"));
                txtDetalleEquipo.setText(rs.getString("nombre_equipo"));
                txtDetalleLaboratorio.setText(rs.getString("laboratorio"));

                txtDetallePrioridad.setText(rs.getString("prioridad"));
                txtDetalleReportadoPor.setText(rs.getString("reportado_por"));

                txtDetalleTipoFalla.setText(rs.getString("rol_reportante"));

                txtDetalleFechaAtencion.setText(rs.getString("fecha_revision"));
                txtDetalleDescripcion.setText(rs.getString("descripcion_falla"));
                txtDetalleObservaciones.setText(rs.getString("observaciones"));
                
                detalleReporteFalla.pack();
                detalleReporteFalla.setLocationRelativeTo(null);
                detalleReporteFalla.setVisible(true);
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se encontró el reporte seleccionado.",
                    "Sin resultados",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
            }
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cargar detalles del reporte: " + e.getMessage(),
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
    
    private void limpiarDetalleReporteFalla() {
        txtDetalleID.setText("");
        txtDetalleEstado.setText("");
        txtDetalleFecha.setText("");
        txtDetalleCodigo.setText("");
        txtDetalleEquipo.setText("");
        txtDetalleLaboratorio.setText("");
        txtDetallePrioridad.setText("");
        txtDetalleReportadoPor.setText("");
        txtDetalleTipoFalla.setText("");
        txtDetalleFechaAtencion.setText("");
        txtDetalleDescripcion.setText("");
        txtDetalleObservaciones.setText("");
    }
    
    private void marcarReporteEnRevision(int idFalla) {
        Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        String sql = "UPDATE reporte_fallas "
            + "SET estado = 'En revisión', fecha_revision = CURDATE() "
            + "WHERE id_falla = ?";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idFalla);
            
            int filas = ps.executeUpdate();
            
            if (filas > 0) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "El reporte fue marcado como En revisión.",
                    "Actualización exitosa",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
                cargarTablaReportesFiltrada();
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se encontró el reporte seleccionado.",
                    "Sin cambios",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
            }
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al marcar el reporte en revisión: " + e.getMessage(),
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
    
    private void marcarReporteAtendido(int idFalla, String codigoEquipo) {
        Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        String sqlReporte = "UPDATE reporte_fallas "
            + "SET estado = 'Atendida', fecha_revision = CURDATE() "
            + "WHERE id_falla = ?";
        
        String sqlInventario = "UPDATE inventario "
            + "SET estado = 'Disponible' "
            + "WHERE codigo = ?";
        
        try {
            PreparedStatement psReporte = con.prepareStatement(sqlReporte);
            psReporte.setInt(1, idFalla);
            
            int filasReporte = psReporte.executeUpdate();
            
            if (filasReporte > 0) {
                PreparedStatement psInventario = con.prepareStatement(sqlInventario);
                psInventario.setString(1, codigoEquipo);
                psInventario.executeUpdate();
                
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "El reporte fue marcado como atendido.",
                    "Actualización exitosa",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
                
                cargarTablaReportesFiltrada();
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se encontró el reporte seleccionado.",
                    "Sin cambios",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
            } 
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al marcar el reporte como atendido: " + e.getMessage(),
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
    
    private void cancelarReporteFalla(int idFalla, String codigoEquipo) {
        Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        String sqlReporte = "UPDATE reporte_fallas "
            + "SET estado = 'Cancelada' "
            + "WHERE id_falla = ?";
        
        String sqlInventario = "UPDATE inventario "
            + "SET estado = 'Disponible' "
            + "WHERE codigo = ?";
        
        try {
            PreparedStatement psReporte = con.prepareCall(sqlReporte);
            psReporte.setInt(1, idFalla);
            
            int filaReporte = psReporte.executeUpdate();
            
            if (filaReporte > 0) {
                PreparedStatement psInventario = con.prepareStatement(sqlInventario);
                psInventario.setString(1, codigoEquipo);
                psInventario.executeUpdate();
                
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "El reporte fue cancelado correctamente.",
                    "Reporte cancelado",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
                
                cargarTablaReportesFiltrada();
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se encontró el reporte seleccionado.",
                    "Sin cambios",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
            } 
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cancelar el reporte: " + e.getMessage(),
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
    
    private void exportarTablaReporteFallasExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar reporte de fallas");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo Excel (*.xlsx)", "xlsx"));
        fileChooser.setSelectedFile(new File("reporte_fallas_labsync.xlsx"));

        int seleccion = fileChooser.showSaveDialog(this);

        if (seleccion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = fileChooser.getSelectedFile();

        if (!archivo.getName().toLowerCase().endsWith(".xlsx")) {
            archivo = new File(archivo.getAbsolutePath() + ".xlsx");
        }

        String textoBusqueda = txtBuscar.getText().trim();
        String laboratorioSeleccionado = cmbLaboratorio.getSelectedItem().toString();
        String estadoSeleccionado = cmbEstado.getSelectedItem().toString();
        String prioridadSeleccionada = cmbPrioridad.getSelectedItem().toString();

        if (textoBusqueda.equals(PH_BUSCAR)) {
            textoBusqueda = "";
        }

        String sql = "SELECT "
            + "id_falla, "
            + "DATE_FORMAT(fecha_reporte, '%d/%m/%Y %H:%i:%s') AS fecha_reporte, "
            + "codigo_equipo, "
            + "nombre_equipo, "
            + "laboratorio, "
            + "reportado_por, "
            + "rol_reportante, "
            + "descripcion_falla, "
            + "prioridad, "
            + "estado, "
            + "IFNULL(DATE_FORMAT(fecha_revision, '%d/%m/%Y'), 'Sin revisi\u00F3n') AS fecha_revision, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM reporte_fallas "
            + "WHERE 1=1 ";

        java.util.ArrayList<String> parametros = new java.util.ArrayList<>();

        if (!textoBusqueda.isEmpty()) {
            sql += "AND (codigo_equipo LIKE ? "
                + "OR nombre_equipo LIKE ? "
                + "OR laboratorio LIKE ? "
                + "OR reportado_por LIKE ? "
                + "OR rol_reportante LIKE ? "
                + "OR descripcion_falla LIKE ?) ";

            String busqueda = "%" + textoBusqueda + "%";

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

        if (!estadoSeleccionado.equals("Todos")) {
            sql += "AND estado = ? ";
            parametros.add(estadoSeleccionado);
        } else {
            sql += "AND estado NOT IN ('Atendida', 'Cancelada') ";
        }

        if (!prioridadSeleccionada.equals("Todos")) {
            sql += "AND prioridad = ? ";
            parametros.add(prioridadSeleccionada);
        }

        sql += "ORDER BY fecha_reporte DESC";

        Connection con = ConexionBD.conectar();

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
            Sheet hoja = workbook.createSheet("Reporte de Fallas");

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
            celdaTitulo.setCellValue("Reporte de Fallas - LabSync");
            celdaTitulo.setCellStyle(estiloTitulo);

            hoja.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 10));

            Row filaEncabezado = hoja.createRow(2);

            String[] encabezados = {
                "Fecha Reporte",
                "C\u00F3digo Equipo",
                "Nombre Equipo",
                "Laboratorio",
                "Reportado Por",
                "Rol Reportante",
                "Descripci\u00F3n de la Falla",
                "Prioridad",
                "Estado",
                "Fecha Revisi\u00F3n",
                "Observaciones"
            };

            for (int i = 0; i < encabezados.length; i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(encabezados[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            PreparedStatement ps = con.prepareStatement(sql);

            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }

            ResultSet rs = ps.executeQuery();

            int filaExcel = 3;
            int totalRegistros = 0;

            while (rs.next()) {
                Row fila = hoja.createRow(filaExcel);

                Object[] datos = {
                    rs.getString("fecha_reporte"),
                    rs.getString("codigo_equipo"),
                    rs.getString("nombre_equipo"),
                    rs.getString("laboratorio"),
                    rs.getString("reportado_por"),
                    rs.getString("rol_reportante"),
                    rs.getString("descripcion_falla"),
                    rs.getString("prioridad"),
                    rs.getString("estado"),
                    rs.getString("fecha_revision"),
                    rs.getString("observaciones")
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
                "El reporte de fallas se export\u00F3 correctamente.",
                "Exportaci\u00F3n exitosa",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al exportar reporte de fallas: " + e.getMessage(),
                "Error",
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

        detalleReporteFalla = new javax.swing.JDialog();
        bodyModal = new javax.swing.JPanel();
        lbTituloModal = new javax.swing.JLabel();
        linea = new javax.swing.JPanel();
        lbDetalleID = new javax.swing.JLabel();
        txtDetalleID = new javax.swing.JTextField();
        lbDetalleEstado = new javax.swing.JLabel();
        txtDetalleEstado = new javax.swing.JTextField();
        lbDetalleFecha = new javax.swing.JLabel();
        txtDetalleFecha = new javax.swing.JTextField();
        lbDetalleCodigo = new javax.swing.JLabel();
        txtDetalleCodigo = new javax.swing.JTextField();
        lbDetalleEquipo = new javax.swing.JLabel();
        txtDetalleEquipo = new javax.swing.JTextField();
        lbDetalleLaboratorio = new javax.swing.JLabel();
        txtDetalleLaboratorio = new javax.swing.JTextField();
        lbDetallePrioridad = new javax.swing.JLabel();
        txtDetallePrioridad = new javax.swing.JTextField();
        lbDetalleReportadoPor = new javax.swing.JLabel();
        txtDetalleReportadoPor = new javax.swing.JTextField();
        lbDetalleTipoFalla = new javax.swing.JLabel();
        txtDetalleTipoFalla = new javax.swing.JTextField();
        lbDetalleFechaAtencion = new javax.swing.JLabel();
        txtDetalleFechaAtencion = new javax.swing.JTextField();
        lbDetalleDescripcion = new javax.swing.JLabel();
        scrollDetalleDescripcion = new javax.swing.JScrollPane();
        txtDetalleDescripcion = new javax.swing.JTextArea();
        lbDetalleObservaciones = new javax.swing.JLabel();
        scrollDetalleObservaciones = new javax.swing.JScrollPane();
        txtDetalleObservaciones = new javax.swing.JTextArea();
        btnCerrarDetalle = new javax.swing.JButton();
        sidebar = new javax.swing.JPanel();
        imgLabSync = new javax.swing.JLabel();
        btnInicio = new javax.swing.JButton();
        btnBitacora = new javax.swing.JButton();
        btnInventario = new javax.swing.JButton();
        btnMant = new javax.swing.JButton();
        btnReservas = new javax.swing.JButton();
        header = new javax.swing.JPanel();
        lbTitulo = new javax.swing.JLabel();
        lbEstado = new javax.swing.JLabel();
        lbLaboratorio = new javax.swing.JLabel();
        lbPrioridad = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        cmbLaboratorio = new javax.swing.JComboBox<>();
        cmbEstado = new javax.swing.JComboBox<>();
        cmbPrioridad = new javax.swing.JComboBox<>();
        btnLimpiar = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        body = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaReportes = new javax.swing.JTable();
        btnVerDetalles = new javax.swing.JButton();
        btnRevision = new javax.swing.JButton();
        btnAtendida = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnExportar = new javax.swing.JButton();

        detalleReporteFalla.setTitle("Detalles del Reporte de Falla");
        detalleReporteFalla.setMinimumSize(new java.awt.Dimension(630, 680));
        detalleReporteFalla.setModal(true);
        detalleReporteFalla.setResizable(false);

        bodyModal.setBackground(new java.awt.Color(245, 245, 245));
        bodyModal.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloModal.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTituloModal.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloModal.setText("Detalles del Reporte");
        bodyModal.add(lbTituloModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 25, 240, -1));

        linea.setBackground(new java.awt.Color(8, 173, 141));

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

        lbDetalleID.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleID.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleID.setText("ID Reporte");
        bodyModal.add(lbDetalleID, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, -1, -1));

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

        lbDetalleFecha.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleFecha.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleFecha.setText("Fecha Reporte");
        bodyModal.add(lbDetalleFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 90, -1, -1));

        txtDetalleFecha.setEditable(false);
        txtDetalleFecha.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleFecha.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleFecha.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleFecha.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        txtDetalleFecha.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        bodyModal.add(txtDetalleFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 115, 230, 24));

        lbDetalleCodigo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleCodigo.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleCodigo.setText("Código Equipo");
        bodyModal.add(lbDetalleCodigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 155, -1, -1));

        txtDetalleCodigo.setEditable(false);
        txtDetalleCodigo.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleCodigo.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleCodigo.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleCodigo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleCodigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 180, 180, 24));

        lbDetalleEquipo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleEquipo.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleEquipo.setText("Nombre Equipo");
        bodyModal.add(lbDetalleEquipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 155, -1, -1));

        txtDetalleEquipo.setEditable(false);
        txtDetalleEquipo.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleEquipo.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleEquipo.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleEquipo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleEquipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 180, 350, 24));

        lbDetalleLaboratorio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleLaboratorio.setText("Laboratorio");
        bodyModal.add(lbDetalleLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 220, -1, -1));

        txtDetalleLaboratorio.setEditable(false);
        txtDetalleLaboratorio.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleLaboratorio.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleLaboratorio.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 245, 170, 24));

        lbDetallePrioridad.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetallePrioridad.setForeground(new java.awt.Color(102, 102, 102));
        lbDetallePrioridad.setText("Prioidad");
        bodyModal.add(lbDetallePrioridad, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 220, -1, -1));

        txtDetallePrioridad.setEditable(false);
        txtDetallePrioridad.setBackground(new java.awt.Color(255, 255, 255));
        txtDetallePrioridad.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetallePrioridad.setForeground(new java.awt.Color(51, 51, 51));
        txtDetallePrioridad.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetallePrioridad, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 245, 160, 24));

        lbDetalleReportadoPor.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleReportadoPor.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleReportadoPor.setText("Reportado por");
        bodyModal.add(lbDetalleReportadoPor, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 220, -1, -1));

        txtDetalleReportadoPor.setEditable(false);
        txtDetalleReportadoPor.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleReportadoPor.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleReportadoPor.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleReportadoPor.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleReportadoPor, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 245, 170, 24));

        lbDetalleTipoFalla.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleTipoFalla.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleTipoFalla.setText("Rol reportante");
        bodyModal.add(lbDetalleTipoFalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 285, -1, -1));

        txtDetalleTipoFalla.setEditable(false);
        txtDetalleTipoFalla.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleTipoFalla.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleTipoFalla.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleTipoFalla.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleTipoFalla, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 310, 260, 24));

        lbDetalleFechaAtencion.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleFechaAtencion.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleFechaAtencion.setText("Fecha de Atención");
        bodyModal.add(lbDetalleFechaAtencion, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 285, -1, -1));

        txtDetalleFechaAtencion.setEditable(false);
        txtDetalleFechaAtencion.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleFechaAtencion.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleFechaAtencion.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleFechaAtencion.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(txtDetalleFechaAtencion, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 310, 270, 24));

        lbDetalleDescripcion.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleDescripcion.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleDescripcion.setText("Descripción de la Falla");
        bodyModal.add(lbDetalleDescripcion, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 355, -1, -1));

        txtDetalleDescripcion.setEditable(false);
        txtDetalleDescripcion.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleDescripcion.setColumns(20);
        txtDetalleDescripcion.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleDescripcion.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleDescripcion.setLineWrap(true);
        txtDetalleDescripcion.setRows(5);
        txtDetalleDescripcion.setWrapStyleWord(true);
        txtDetalleDescripcion.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        scrollDetalleDescripcion.setViewportView(txtDetalleDescripcion);

        bodyModal.add(scrollDetalleDescripcion, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 380, 560, 80));

        lbDetalleObservaciones.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleObservaciones.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleObservaciones.setText("Observaciones");
        bodyModal.add(lbDetalleObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 475, -1, -1));

        txtDetalleObservaciones.setEditable(false);
        txtDetalleObservaciones.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleObservaciones.setColumns(20);
        txtDetalleObservaciones.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleObservaciones.setLineWrap(true);
        txtDetalleObservaciones.setRows(5);
        txtDetalleObservaciones.setWrapStyleWord(true);
        txtDetalleObservaciones.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        scrollDetalleObservaciones.setViewportView(txtDetalleObservaciones);

        bodyModal.add(scrollDetalleObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 500, 560, 70));

        btnCerrarDetalle.setBackground(new java.awt.Color(108, 117, 125));
        btnCerrarDetalle.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCerrarDetalle.setForeground(new java.awt.Color(255, 255, 255));
        btnCerrarDetalle.setText("Cerrar");
        btnCerrarDetalle.setBorderPainted(false);
        btnCerrarDetalle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCerrarDetalle.setFocusPainted(false);
        btnCerrarDetalle.addActionListener(this::btnCerrarDetalleActionPerformed);
        bodyModal.add(btnCerrarDetalle, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 590, 180, 40));

        javax.swing.GroupLayout detalleReporteFallaLayout = new javax.swing.GroupLayout(detalleReporteFalla.getContentPane());
        detalleReporteFalla.getContentPane().setLayout(detalleReporteFallaLayout);
        detalleReporteFallaLayout.setHorizontalGroup(
            detalleReporteFallaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(bodyModal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        detalleReporteFallaLayout.setVerticalGroup(
            detalleReporteFallaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(bodyModal, javax.swing.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Reporte de Fallas");
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
        btnInicio.addActionListener(this::btnInicioActionPerformed);
        sidebar.add(btnInicio, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 290, 200, 50));

        btnBitacora.setBackground(new java.awt.Color(255, 255, 255));
        btnBitacora.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnBitacora.setForeground(new java.awt.Color(6, 140, 115));
        btnBitacora.setText("Bitácora");
        btnBitacora.setBorderPainted(false);
        btnBitacora.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBitacora.setFocusPainted(false);
        btnBitacora.addActionListener(this::btnBitacoraActionPerformed);
        sidebar.add(btnBitacora, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 350, 200, 50));

        btnInventario.setBackground(new java.awt.Color(255, 255, 255));
        btnInventario.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnInventario.setForeground(new java.awt.Color(6, 140, 115));
        btnInventario.setText("Inventario");
        btnInventario.setBorderPainted(false);
        btnInventario.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnInventario.setFocusPainted(false);
        btnInventario.addActionListener(this::btnInventarioActionPerformed);
        sidebar.add(btnInventario, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 410, 200, 50));

        btnMant.setBackground(new java.awt.Color(255, 255, 255));
        btnMant.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnMant.setForeground(new java.awt.Color(6, 140, 115));
        btnMant.setText("Mantenimiento");
        btnMant.setBorderPainted(false);
        btnMant.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnMant.setFocusPainted(false);
        btnMant.addActionListener(this::btnMantActionPerformed);
        sidebar.add(btnMant, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 470, 200, 50));

        btnReservas.setBackground(new java.awt.Color(255, 255, 255));
        btnReservas.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnReservas.setForeground(new java.awt.Color(6, 140, 115));
        btnReservas.setText("Reservas");
        btnReservas.setBorderPainted(false);
        btnReservas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnReservas.setFocusPainted(false);
        btnReservas.addActionListener(this::btnReservasActionPerformed);
        sidebar.add(btnReservas, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 530, 200, 50));

        header.setBackground(new java.awt.Color(255, 255, 255));
        header.setPreferredSize(new java.awt.Dimension(1071, 150));
        header.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTitulo.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTitulo.setForeground(new java.awt.Color(102, 102, 102));
        lbTitulo.setText("Reporte de Fallas");
        header.add(lbTitulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 30, -1, -1));

        lbEstado.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbEstado.setForeground(new java.awt.Color(102, 102, 102));
        lbEstado.setText("Estado");
        header.add(lbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 60, -1, -1));

        lbLaboratorio.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        lbLaboratorio.setText("Laboratorio");
        header.add(lbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 60, -1, -1));

        lbPrioridad.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbPrioridad.setForeground(new java.awt.Color(102, 102, 102));
        lbPrioridad.setText("Prioridad");
        header.add(lbPrioridad, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 60, -1, -1));

        txtBuscar.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        header.add(txtBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 80, 240, 30));

        cmbLaboratorio.setBackground(new java.awt.Color(255, 255, 255));
        cmbLaboratorio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        cmbLaboratorio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "PB-05", "M-11", "M-12", "M-13", "M-14", "M-02", "M-05", "5-06", "5-03" }));
        cmbLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbLaboratorio.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        header.add(cmbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 80, 170, 30));

        cmbEstado.setBackground(new java.awt.Color(255, 255, 255));
        cmbEstado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbEstado.setForeground(new java.awt.Color(102, 102, 102));
        cmbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Pendiente", "En revisión", "Atendida", "Cancelada" }));
        cmbEstado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbEstado.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        header.add(cmbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 80, 170, 30));

        cmbPrioridad.setBackground(new java.awt.Color(255, 255, 255));
        cmbPrioridad.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbPrioridad.setForeground(new java.awt.Color(102, 102, 102));
        cmbPrioridad.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Baja", "Media", "Alta", "Urgente" }));
        cmbPrioridad.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbPrioridad.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        header.add(cmbPrioridad, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 80, 170, 30));

        btnLimpiar.setBackground(new java.awt.Color(8, 173, 141));
        btnLimpiar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnLimpiar.setForeground(new java.awt.Color(255, 255, 255));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.addActionListener(this::btnLimpiarActionPerformed);
        header.add(btnLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(920, 40, 125, 30));

        btnBuscar.setBackground(new java.awt.Color(8, 173, 141));
        btnBuscar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnBuscar.setForeground(new java.awt.Color(255, 255, 255));
        btnBuscar.setText("Buscar");
        btnBuscar.setBorderPainted(false);
        btnBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBuscar.setFocusPainted(false);
        btnBuscar.addActionListener(this::btnBuscarActionPerformed);
        header.add(btnBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(920, 80, 125, 30));

        body.setBackground(new java.awt.Color(204, 204, 204));
        body.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tablaReportes.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        tablaReportes.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Fecha", "Código Equipo", "Nombre Equipo", "Laboratorio", "Falla", "Prioridad", "Estado", "Reportado Por", "Observaciones"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaReportes.setRowHeight(36);
        jScrollPane1.setViewportView(tablaReportes);

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

        btnRevision.setBackground(new java.awt.Color(13, 110, 253));
        btnRevision.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnRevision.setForeground(new java.awt.Color(255, 255, 255));
        btnRevision.setText("En Revisión");
        btnRevision.setBorderPainted(false);
        btnRevision.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnRevision.setFocusPainted(false);
        btnRevision.addActionListener(this::btnRevisionActionPerformed);
        body.add(btnRevision, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 500, 150, 40));

        btnAtendida.setBackground(new java.awt.Color(25, 135, 84));
        btnAtendida.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnAtendida.setForeground(new java.awt.Color(255, 255, 255));
        btnAtendida.setText("Marcar Atendida");
        btnAtendida.setBorderPainted(false);
        btnAtendida.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnAtendida.setFocusPainted(false);
        btnAtendida.addActionListener(this::btnAtendidaActionPerformed);
        body.add(btnAtendida, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 500, 180, 40));

        btnCancelar.setBackground(new java.awt.Color(220, 53, 69));
        btnCancelar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCancelar.setForeground(new java.awt.Color(255, 255, 255));
        btnCancelar.setText("Cancelar");
        btnCancelar.setBorderPainted(false);
        btnCancelar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCancelar.setFocusPainted(false);
        btnCancelar.addActionListener(this::btnCancelarActionPerformed);
        body.add(btnCancelar, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 500, 130, 40));

        btnExportar.setBackground(new java.awt.Color(90, 90, 90));
        btnExportar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnExportar.setForeground(new java.awt.Color(255, 255, 255));
        btnExportar.setText("Exportar");
        btnExportar.setBorderPainted(false);
        btnExportar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnExportar.setFocusPainted(false);
        btnExportar.addActionListener(this::btnExportarActionPerformed);
        body.add(btnExportar, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 500, 130, 40));

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

    private void btnMantActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMantActionPerformed
        Mantenimiento ventanaMant = new Mantenimiento(nombreUsuario);
        
        ventanaMant.setVisible(true);
        ventanaMant.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnMantActionPerformed

    private void btnReservasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReservasActionPerformed
        Reserva ventanaReserva = new Reserva(nombreUsuario);
        
        ventanaReserva.setVisible(true);
        ventanaReserva.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReservasActionPerformed

    private void btnVerDetallesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerDetallesActionPerformed
        int filaSeleccionada = tablaReportes.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona un reporte de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int filaModelo = tablaReportes.convertRowIndexToModel(filaSeleccionada);
        
        int idFalla = Integer.parseInt(
            tablaReportes.getModel().getValueAt(filaModelo, 0).toString()
        );
        
        cargarDetalleReportesFalla(idFalla);
    }//GEN-LAST:event_btnVerDetallesActionPerformed

    private void btnCerrarDetalleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarDetalleActionPerformed
        detalleReporteFalla.setVisible(false);
        limpiarDetalleReporteFalla();
    }//GEN-LAST:event_btnCerrarDetalleActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarFiltrosReporteFalla();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnRevisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRevisionActionPerformed
        int filaSeleccionada = tablaReportes.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona un reporte de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int filaModelo = tablaReportes.convertRowIndexToModel(filaSeleccionada);
        
        int idFalla = Integer.parseInt(
            tablaReportes.getModel().getValueAt(filaModelo, 0).toString()
        );
        
        String estadoActual = tablaReportes.getModel().getValueAt(filaModelo, 7).toString();
        
        if (!estadoActual.equals("Pendiente")) {
            javax.swing.JOptionPane.showMessageDialog(
               this,
               "Solo se pueden poner en revisión reportes que estén en estado Pendiente.",
               "Acción no permitida",
               javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Deseas marcar este reporte como En revisión?",
            "Confirmar revisión",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            marcarReporteEnRevision(idFalla);
        }
    }//GEN-LAST:event_btnRevisionActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        cargarTablaReportesFiltrada();
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnAtendidaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAtendidaActionPerformed
        int filaSeleccionada = tablaReportes.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona un reporte de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int filaModelo = tablaReportes.convertRowIndexToModel(filaSeleccionada);
        
        int idFalla = Integer.parseInt(
            tablaReportes.getModel().getValueAt(filaModelo, 0).toString()
        );
        
        String codigoEquipo = tablaReportes.getModel().getValueAt(filaModelo, 2).toString();
        String estadoActual = tablaReportes.getModel().getValueAt(filaModelo, 7).toString();
        
        if (estadoActual.equals("Atendida")) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Este reporte ya fue atendido.",
                "Acción no permitida",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        if (estadoActual.equals("Cancelada")) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No se puede atender un reporte cancelado.",
                "Acción no permitida",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Deseas marcar este reporte como atendido?",
            "Confirmar atención",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            marcarReporteAtendido(idFalla, codigoEquipo);
        }
    }//GEN-LAST:event_btnAtendidaActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        int filaSeleccionada = tablaReportes.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona un reporte de la tabla.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int filaModelo = tablaReportes.convertRowIndexToModel(filaSeleccionada);
        
        int idFalla = Integer.parseInt(
            tablaReportes.getModel().getValueAt(filaModelo, 0).toString()
        );
        
        String codigoEquipo = tablaReportes.getModel().getValueAt(filaModelo, 2).toString();
        String estadoActual = tablaReportes.getModel().getValueAt(filaModelo, 7).toString();
        
        if (estadoActual.equals("Atendida")) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No se puede cancelar un reporte que ya fue atendido.",
                "Acción no permitida",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        if (estadoActual.equals("Cancelada")) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Este reporte ya está cancelado.",
                "Acción no permitida",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Deseas cancelar este reporte de falla?",
            "Confirmar cancelación",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        
        if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
            cancelarReporteFalla(idFalla, codigoEquipo);
        }            
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarActionPerformed
        exportarTablaReporteFallasExcel();
    }//GEN-LAST:event_btnExportarActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new ReporteFalla().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel body;
    private javax.swing.JPanel bodyModal;
    private javax.swing.JButton btnAtendida;
    private javax.swing.JButton btnBitacora;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnCerrarDetalle;
    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnInicio;
    private javax.swing.JButton btnInventario;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnMant;
    private javax.swing.JButton btnReservas;
    private javax.swing.JButton btnRevision;
    private javax.swing.JButton btnVerDetalles;
    private javax.swing.JComboBox<String> cmbEstado;
    private javax.swing.JComboBox<String> cmbLaboratorio;
    private javax.swing.JComboBox<String> cmbPrioridad;
    private javax.swing.JDialog detalleReporteFalla;
    private javax.swing.JPanel header;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbDetalleCodigo;
    private javax.swing.JLabel lbDetalleDescripcion;
    private javax.swing.JLabel lbDetalleEquipo;
    private javax.swing.JLabel lbDetalleEstado;
    private javax.swing.JLabel lbDetalleFecha;
    private javax.swing.JLabel lbDetalleFechaAtencion;
    private javax.swing.JLabel lbDetalleID;
    private javax.swing.JLabel lbDetalleLaboratorio;
    private javax.swing.JLabel lbDetalleObservaciones;
    private javax.swing.JLabel lbDetallePrioridad;
    private javax.swing.JLabel lbDetalleReportadoPor;
    private javax.swing.JLabel lbDetalleTipoFalla;
    private javax.swing.JLabel lbEstado;
    private javax.swing.JLabel lbLaboratorio;
    private javax.swing.JLabel lbPrioridad;
    private javax.swing.JLabel lbTitulo;
    private javax.swing.JLabel lbTituloModal;
    private javax.swing.JPanel linea;
    private javax.swing.JScrollPane scrollDetalleDescripcion;
    private javax.swing.JScrollPane scrollDetalleObservaciones;
    private javax.swing.JPanel sidebar;
    private javax.swing.JTable tablaReportes;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtDetalleCodigo;
    private javax.swing.JTextArea txtDetalleDescripcion;
    private javax.swing.JTextField txtDetalleEquipo;
    private javax.swing.JTextField txtDetalleEstado;
    private javax.swing.JTextField txtDetalleFecha;
    private javax.swing.JTextField txtDetalleFechaAtencion;
    private javax.swing.JTextField txtDetalleID;
    private javax.swing.JTextField txtDetalleLaboratorio;
    private javax.swing.JTextArea txtDetalleObservaciones;
    private javax.swing.JTextField txtDetallePrioridad;
    private javax.swing.JTextField txtDetalleReportadoPor;
    private javax.swing.JTextField txtDetalleTipoFalla;
    // End of variables declaration//GEN-END:variables
}
