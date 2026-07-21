
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

public class Inventario extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Inventario.class.getName());
    private String nombreUsuario;
    private boolean modoEdicion = false;
    private String codigoOriginal = "";
    private final Color COLOR_PLACEHOLDER = new Color(150, 150, 150);
    private final Color COLOR_TEXTO = new Color(51, 51, 51);
    private final String PH_BUSCAR = "Código, marca, modelo o No. serie";   

    private void cargarLaboratoriosDesdeBD() {
        LaboratoriosBD.cargarDisponibles(cmbLaboratorioModal, "Selecciona");
        LaboratoriosBD.cargarTodos(laboratorio, "Todos");
    }
    
    public Inventario(String nombreRecibido) {
        initComponents();
        cargarLaboratoriosDesdeBD();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
       
        this.nombreUsuario = nombreRecibido;
        
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        
        ocultarCampoNombreEquipo();
        ponerPlaceholderBuscar();
        cargarTablaInventario();
        cargarTablaInventarioFiltrada();
    }

    public Inventario() {
        initComponents();
        cargarLaboratoriosDesdeBD();
        
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        
        ocultarCampoNombreEquipo();
        ponerPlaceholderBuscar();
        cargarTablaInventario();
        cargarTablaInventarioFiltrada();
    }
    
    private void ocultarCampoNombreEquipo() {
        // Evita que AbsoluteLayout restaure las posiciones del GUI Builder
        bodyModal.setLayout(null);

        lbNombreEquipo.setVisible(false);
        txtNombreEquipo.setVisible(false);
        txtNombreEquipo.setText("N/A");

        // Título y línea
        lbTituloModal.setBounds(30, 25, 300, 30);
        linea.setBounds(30, 65, 500, 3);

        // Primera fila
        lbCodigo.setBounds(30, 90, 220, 22);
        txtCodigo.setBounds(30, 120, 220, 24);

        lbTipoDispositivoModal.setBounds(310, 90, 220, 22);
        cmbTipoDispositivoModal.setBounds(310, 120, 220, 24);

        // Segunda fila
        lbMarca.setBounds(30, 165, 220, 22);
        txtMarca.setBounds(30, 195, 220, 24);

        lbModelo.setBounds(310, 165, 220, 22);
        txtModelo.setBounds(310, 195, 220, 24);

        // Tercera fila
        lbNoSerie.setBounds(30, 240, 220, 22);
        txtNoSerie.setBounds(30, 270, 220, 24);

        lbLaboratorioModal.setBounds(310, 240, 220, 22);
        cmbLaboratorioModal.setBounds(310, 270, 220, 24);

        // Parte inferior
        lbOberservaciones.setBounds(310, 315, 220, 22);
        jScrollPane3.setBounds(310, 345, 220, 120);

        btnGuardar.setBounds(50, 345, 160, 40);
        btnCancelar.setBounds(50, 405, 160, 40);

        // Tamaño interno del modal
        bodyModal.setPreferredSize(new java.awt.Dimension(567, 500));
        bodyModal.setMinimumSize(new java.awt.Dimension(567, 500));
        bodyModal.setSize(567, 500);

        bodyModal.revalidate();
        bodyModal.repaint();
    }
    
    private void ocultarColumnaNombreEquipo() {
        if (tablaInventario.getColumnModel().getColumnCount() > 1) {
            javax.swing.table.TableColumn columnaNombre
                    = tablaInventario.getColumnModel().getColumn(1);

            columnaNombre.setMinWidth(0);
            columnaNombre.setMaxWidth(0);
            columnaNombre.setPreferredWidth(0);
            columnaNombre.setWidth(0);
        }
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
    
    private void limpiarCamposEquipo() {
        txtCodigo.setText("");
        txtNombreEquipo.setText("N/A");
        txtMarca.setText("");
        txtModelo.setText("");
        txtNoSerie.setText("");
        txtObservaciones.setText("");

        cmbTipoDispositivoModal.setSelectedIndex(0);
        cmbLaboratorioModal.setSelectedIndex(0);
    }
    
    private void limpiarFiltrosInventario() {
        txtBuscar.setText(PH_BUSCAR);
        txtBuscar.setForeground(COLOR_PLACEHOLDER);

        laboratorio.setSelectedIndex(0);
        estado.setSelectedIndex(0);
        dispositivo.setSelectedIndex(0);

        cargarTablaInventarioFiltrada();
    }
    
    private boolean validarCamposEquipo() {
        String codigo = txtCodigo.getText().trim();
        String marca = txtMarca.getText().trim();
        String modelo = txtModelo.getText().trim();
        String noSerie = txtNoSerie.getText().trim();

        if (codigo.isEmpty()) {
            mostrarErrorValidacion(
                "Ingresa el código del equipo.",
                txtCodigo
            );
            return false;
        }

        if (cmbTipoDispositivoModal.getSelectedIndex() == 0) {
            mostrarErrorValidacion(
                "Selecciona el tipo de dispositivo.",
                cmbTipoDispositivoModal
            );
            return false;
        }

        if (marca.isEmpty()) {
            mostrarErrorValidacion(
                "Ingresa la marca del equipo.",
                txtMarca
            );
            return false;
        }

        if (modelo.isEmpty()) {
            mostrarErrorValidacion(
                "Ingresa el modelo del equipo.",
                txtModelo
            );
            return false;
        }

        if (noSerie.isEmpty()) {
            mostrarErrorValidacion(
                "Ingresa el número de serie del equipo.",
                txtNoSerie
            );
            return false;
        }

        if (cmbLaboratorioModal.getSelectedIndex() == 0) {
            mostrarErrorValidacion(
                "Selecciona el laboratorio asignado.",
                cmbLaboratorioModal
            );
            return false;
        }

        return true;
    }
    
    private boolean guardarEquipoBD() {
        String codigo = txtCodigo.getText().trim();
        String nombreEquipo = "N/A";
        String tipoDispositivo
                = cmbTipoDispositivoModal.getSelectedItem().toString();
        String marca = txtMarca.getText().trim();
        String modelo = txtModelo.getText().trim();
        String noSerie = txtNoSerie.getText().trim();
        String laboratorioSeleccionado
                = cmbLaboratorioModal.getSelectedItem().toString();
        String estadoEquipo = "Disponible";
        String observaciones = txtObservaciones.getText().trim();

        java.sql.Connection con = ConexionBD.conectar();

        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                addEquipo,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        String sql = "INSERT INTO inventario "
                + "(codigo, nombre_equipo, tipo_dispositivo, marca, modelo, "
                + "no_serie, laboratorio, estado, observaciones) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, codigo);
            ps.setString(2, nombreEquipo);
            ps.setString(3, tipoDispositivo);
            ps.setString(4, marca);
            ps.setString(5, modelo);
            ps.setString(6, noSerie);
            ps.setString(7, laboratorioSeleccionado);
            ps.setString(8, estadoEquipo);
            ps.setString(9, observaciones);

            int filas = ps.executeUpdate();

            return filas > 0;

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                addEquipo,
                "Error al guardar el equipo: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;

        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
                logger.log(
                    java.util.logging.Level.WARNING,
                    "No se pudo cerrar la conexión.",
                    ex
                );
            }
        }
    }
    
    private boolean actualizarEquipoBD() {
        String codigo = txtCodigo.getText().trim();
        String nombreEquipo = "N/A";
        String tipoDispositivo
                = cmbTipoDispositivoModal.getSelectedItem().toString();
        String marca = txtMarca.getText().trim();
        String modelo = txtModelo.getText().trim();
        String noSerie = txtNoSerie.getText().trim();
        String laboratorioSeleccionado
                = cmbLaboratorioModal.getSelectedItem().toString();
        String observaciones = txtObservaciones.getText().trim();

        java.sql.Connection con = ConexionBD.conectar();

        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                addEquipo,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        String sql = "UPDATE inventario SET "
                + "codigo = ?, "
                + "nombre_equipo = ?, "
                + "tipo_dispositivo = ?, "
                + "marca = ?, "
                + "modelo = ?, "
                + "no_serie = ?, "
                + "laboratorio = ?, "
                + "observaciones = ? "
                + "WHERE codigo = ?";

        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, codigo);
            ps.setString(2, nombreEquipo);
            ps.setString(3, tipoDispositivo);
            ps.setString(4, marca);
            ps.setString(5, modelo);
            ps.setString(6, noSerie);
            ps.setString(7, laboratorioSeleccionado);
            ps.setString(8, observaciones);
            ps.setString(9, codigoOriginal);

            int filas = ps.executeUpdate();

            return filas > 0;

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                addEquipo,
                "Error al actualizar el equipo: " + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;

        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
                logger.log(
                    java.util.logging.Level.WARNING,
                    "No se pudo cerrar la conexión.",
                    ex
                );
            }
        }
    }
    
    private void mostrarErrorValidacion(String mensaje, java.awt.Component componente) {
        javax.swing.JOptionPane.showMessageDialog(
            addEquipo,
            mensaje,
            "Campo obligatorio",
            javax.swing.JOptionPane.WARNING_MESSAGE
        );

        componente.requestFocusInWindow();
    }
    
    private void cargarTablaInventario() {
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel();
        
        modelo.addColumn("Código");
        modelo.addColumn("Nombre Equipo");
        modelo.addColumn("Tipo de Dispositivo");
        modelo.addColumn("Marca");
        modelo.addColumn("Modelo");
        modelo.addColumn("No. Serie");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Estado");
        modelo.addColumn("Último Mantenimiento");
        modelo.addColumn("Observaciones");
        
        tablaInventario.setModel(modelo);
        ocultarColumnaNombreEquipo();   
        
        String sql = "SELECT codigo, nombre_equipo, tipo_dispositivo, marca, modelo, "
            + "no_serie, laboratorio, estado, "
            + "IFNULL(DATE_FORMAT(ultimo_mantenimiento, '%d/%m/%Y'), 'Sin registro') AS ultimo_mantenimiento, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM inventario "
            + "ORDER BY id_inventario DESC";
        
        java.sql.Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos",
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
                
                fila[0] = rs.getString("codigo");
                fila[1] = rs.getString("nombre_equipo");
                fila[2] = rs.getString("tipo_dispositivo");
                fila[3] = rs.getString("marca");
                fila[4] = rs.getString("modelo");
                fila[5] = rs.getString("no_serie");
                fila[6] = rs.getString("laboratorio");
                fila[7] = rs.getString("estado");
                fila[8] = rs.getString("ultimo_mantenimiento");
                fila[9] = rs.getString("observaciones");
                
                modelo.addRow(fila);
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al cargar inventario: " + e.getMessage(),
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
    
    private void cargarTablaInventarioFiltrada() {
        javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel();
        
        modelo.addColumn("Código");
        modelo.addColumn("Nombre Equipo");
        modelo.addColumn("Tipo de Dispositivo");
        modelo.addColumn("Marca");
        modelo.addColumn("Modelo");
        modelo.addColumn("No. Serie");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Estado");
        modelo.addColumn("Último Mantenimiento");
        modelo.addColumn("Observaciones");
        
        String textoBusqueda = txtBuscar.getText().trim();

        if (textoBusqueda.equals(PH_BUSCAR)) {
            textoBusqueda = "";
        }
        
        String laboratorioSeleccionado = laboratorio.getSelectedItem().toString();
        String estadoSeleccionado = estado.getSelectedItem().toString();
        String dispositivoSeleccionado = dispositivo.getSelectedItem().toString();
        
        String sql = "SELECT codigo, nombre_equipo, tipo_dispositivo, marca, modelo, "
            + "no_serie, laboratorio, estado, "
            + "IFNULL(DATE_FORMAT(ultimo_mantenimiento, '%d/%m/%Y'), 'Sin registro') AS ultimo_mantenimiento, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM inventario WHERE 1=1 ";
        
        java.util.ArrayList<String> parametros = new java.util.ArrayList<>();
        
        if (!textoBusqueda.isEmpty()) {
            sql += "AND (codigo LIKE ? OR marca LIKE ? OR modelo LIKE ? OR no_serie LIKE ?) ";

            String busqueda = "%" + textoBusqueda + "%";

            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
        }
        
        if (!laboratorioSeleccionado.equals("Todos") && !laboratorioSeleccionado.startsWith("Item")) {
            sql += "AND laboratorio = ? ";
            parametros.add(laboratorioSeleccionado);
        }

        if (!estadoSeleccionado.equals("Todos")) {
            sql += "AND estado = ? ";
            parametros.add(estadoSeleccionado);
        }

        if (!dispositivoSeleccionado.equals("Todos")) {
            sql += "AND tipo_dispositivo = ? ";
            parametros.add(dispositivoSeleccionado);
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
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            
            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }
            
            java.sql.ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] fila  = new Object[10];
                
                fila[0] = rs.getString("codigo");
                fila[1] = rs.getString("nombre_equipo");
                fila[2] = rs.getString("tipo_dispositivo");
                fila[3] = rs.getString("marca");
                fila[4] = rs.getString("modelo");
                fila[5] = rs.getString("no_serie");
                fila[6] = rs.getString("laboratorio");
                fila[7] = rs.getString("estado");
                fila[8] = rs.getString("ultimo_mantenimiento");
                fila[9] = rs.getString("observaciones");
                
                modelo.addRow(fila);
            }
            
            tablaInventario.setModel(modelo);
            ocultarColumnaNombreEquipo();
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al filtrar inventario: " + e.getMessage(),
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
    
    private void darDeBajaEquipo(String codigo) {
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
        
        String sql = "UPDATE inventario SET estado = 'Dado de baja' WHERE codigo = ?";
        
        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            
            ps.setString(1, codigo);
            
            int filas = ps.executeUpdate();
            
            if (filas > 0) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Equipo dado de baja correctamente.",
                    "Baja exitosa",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
                cargarTablaInventarioFiltrada();
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al dar de baja el equipo: " + e.getMessage(),
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
    
    private void exportarTablaInventarioExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar inventario");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo Excel (*.xlsx)", "xlsx"));
        fileChooser.setSelectedFile(new File("inventario_labsync.xlsx"));

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

        String laboratorioSeleccionado = laboratorio.getSelectedItem().toString();
        String estadoSeleccionado = estado.getSelectedItem().toString();
        String dispositivoSeleccionado = dispositivo.getSelectedItem().toString();

        String sql = "SELECT "
            + "codigo, "
            + "nombre_equipo, "
            + "tipo_dispositivo, "
            + "marca, "
            + "modelo, "
            + "no_serie, "
            + "laboratorio, "
            + "estado, "
            + "IFNULL(DATE_FORMAT(ultimo_mantenimiento, '%d/%m/%Y'), 'Sin registro') AS ultimo_mantenimiento, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM inventario "
            + "WHERE 1=1 ";

        java.util.ArrayList<String> parametros = new java.util.ArrayList<>();

        if (!textoBusqueda.isEmpty()) {
            sql += "AND (codigo LIKE ? "
                + "OR nombre_equipo LIKE ? "
                + "OR marca LIKE ? "
                + "OR modelo LIKE ? "
                + "OR no_serie LIKE ?) ";

            String busqueda = "%" + textoBusqueda + "%";

            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
        }

        if (!laboratorioSeleccionado.equals("Todos") && !laboratorioSeleccionado.startsWith("Item")) {
            sql += "AND laboratorio = ? ";
            parametros.add(laboratorioSeleccionado);
        }

        if (!estadoSeleccionado.equals("Todos")) {
            sql += "AND estado = ? ";
            parametros.add(estadoSeleccionado);
        }

        if (!dispositivoSeleccionado.equals("Todos")) {
            sql += "AND tipo_dispositivo = ? ";
            parametros.add(dispositivoSeleccionado);
        }

        sql += "ORDER BY id_inventario DESC";

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
            Sheet hoja = workbook.createSheet("Inventario");

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
            celdaTitulo.setCellValue("Reporte de Inventario - LabSync");
            celdaTitulo.setCellStyle(estiloTitulo);

            hoja.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 9));

            Row filaEncabezado = hoja.createRow(2);

            String[] encabezados = {
                "C\u00F3digo",
                "Nombre Equipo",
                "Tipo de Dispositivo",
                "Marca",
                "Modelo",
                "No. Serie",
                "Laboratorio",
                "Estado",
                "\u00DAltimo Mantenimiento",
                "Observaciones"
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
                    rs.getString("codigo"),
                    rs.getString("nombre_equipo"),
                    rs.getString("tipo_dispositivo"),
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    rs.getString("no_serie"),
                    rs.getString("laboratorio"),
                    rs.getString("estado"),
                    rs.getString("ultimo_mantenimiento"),
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
                "El inventario se export\u00F3 correctamente.",
                "Exportaci\u00F3n exitosa",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Error al exportar inventario: " + e.getMessage(),
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
    
    private void actualizarMantenimientosDelEquipo(
        String codigoAnterior,
        String nuevoCodigo,
        String nuevoLaboratorio
    ) {
        java.sql.Connection con = ConexionBD.conectar();

        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos para actualizar mantenimientos.",
                "Error de conexión",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String sql = "UPDATE mantenimiento SET "
                + "codigo_equipo = ?, "
                + "nombre_equipo = 'N/A', "
                + "laboratorio = ? "
                + "WHERE codigo_equipo = ? "
                + "AND estado IN ('Pendiente', 'En proceso')";

        try {
            java.sql.PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, nuevoCodigo);
            ps.setString(2, nuevoLaboratorio);
            ps.setString(3, codigoAnterior);

            ps.executeUpdate();

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "El equipo se actualizó, pero ocurrió un error al "
                        + "actualizar sus mantenimientos: "
                        + e.getMessage(),
                "Error SQL",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );

        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException ex) {
                logger.log(
                    java.util.logging.Level.WARNING,
                    "No se pudo cerrar la conexión.",
                    ex
                );
            }
        }
    }
    
    private boolean tieneMantenimientoPendiente(String codigoEquipo) {
        java.sql.Connection con = ConexionBD.conectar();
        
        if (con == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
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
                return rs.getInt("total") > 0;
            }
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(
            this,
                "Error al validar mantenimientos pendientes: " + e.getMessage(),
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

        addEquipo = new javax.swing.JDialog();
        bodyModal = new javax.swing.JPanel();
        linea = new javax.swing.JPanel();
        lbCodigo = new javax.swing.JLabel();
        txtCodigo = new javax.swing.JTextField();
        lbNombreEquipo = new javax.swing.JLabel();
        txtNombreEquipo = new javax.swing.JTextField();
        lbTipoDispositivoModal = new javax.swing.JLabel();
        cmbTipoDispositivoModal = new javax.swing.JComboBox<>();
        lbMarca = new javax.swing.JLabel();
        txtMarca = new javax.swing.JTextField();
        lbModelo = new javax.swing.JLabel();
        txtModelo = new javax.swing.JTextField();
        lbNoSerie = new javax.swing.JLabel();
        txtNoSerie = new javax.swing.JTextField();
        lbLaboratorioModal = new javax.swing.JLabel();
        cmbLaboratorioModal = new javax.swing.JComboBox<>();
        lbOberservaciones = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtObservaciones = new javax.swing.JTextArea();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        lbTituloModal = new javax.swing.JLabel();
        sidebar = new javax.swing.JPanel();
        imgLabSync = new javax.swing.JLabel();
        btnInicio = new javax.swing.JButton();
        btnBitacora = new javax.swing.JButton();
        btnMant = new javax.swing.JButton();
        btnReservas = new javax.swing.JButton();
        btnReporteFallas = new javax.swing.JButton();
        header = new javax.swing.JPanel();
        lbTitulo = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        estado = new javax.swing.JComboBox<>();
        laboratorio = new javax.swing.JComboBox<>();
        dispositivo = new javax.swing.JComboBox<>();
        lbLaboratorio = new javax.swing.JLabel();
        lbEstado = new javax.swing.JLabel();
        lbTipoDispositivo = new javax.swing.JLabel();
        btnLimpiar = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        body = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaInventario = new javax.swing.JTable();
        btnExportar = new javax.swing.JButton();
        btnRegistrar = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btnDarBaja = new javax.swing.JButton();

        addEquipo.setTitle("Registrar Equipo");
        addEquipo.setBackground(new java.awt.Color(245, 245, 245));
        addEquipo.setModal(true);
        addEquipo.setName("addEquipo"); // NOI18N
        addEquipo.setResizable(false);

        bodyModal.setBackground(new java.awt.Color(245, 245, 245));
        bodyModal.setForeground(new java.awt.Color(245, 245, 245));
        bodyModal.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        linea.setBackground(new java.awt.Color(8, 173, 141));
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

        lbCodigo.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbCodigo.setForeground(new java.awt.Color(102, 102, 102));
        lbCodigo.setText("Código");
        lbCodigo.setPreferredSize(new java.awt.Dimension(200, 20));
        bodyModal.add(lbCodigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, 90, 22));

        txtCodigo.setBackground(new java.awt.Color(255, 255, 255));
        txtCodigo.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtCodigo.setForeground(new java.awt.Color(51, 51, 51));
        txtCodigo.setPreferredSize(new java.awt.Dimension(200, 20));
        bodyModal.add(txtCodigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 120, 220, 22));

        lbNombreEquipo.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbNombreEquipo.setForeground(new java.awt.Color(102, 102, 102));
        lbNombreEquipo.setText("Nombre del Equipo");
        bodyModal.add(lbNombreEquipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 90, -1, -1));

        txtNombreEquipo.setBackground(new java.awt.Color(255, 255, 255));
        txtNombreEquipo.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtNombreEquipo.setForeground(new java.awt.Color(51, 51, 51));
        bodyModal.add(txtNombreEquipo, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 120, 220, -1));

        lbTipoDispositivoModal.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbTipoDispositivoModal.setForeground(new java.awt.Color(102, 102, 102));
        lbTipoDispositivoModal.setText("Tipo de Dispositivo");
        bodyModal.add(lbTipoDispositivoModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 160, -1, -1));

        cmbTipoDispositivoModal.setBackground(new java.awt.Color(255, 255, 255));
        cmbTipoDispositivoModal.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        cmbTipoDispositivoModal.setForeground(new java.awt.Color(102, 102, 102));
        cmbTipoDispositivoModal.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Selecciona", "Computadora", "Monitor", "Teclado", "Mouse", "Proyector", "Extensión", "HDMI", "Otro" }));
        cmbTipoDispositivoModal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbTipoDispositivoModal.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cmbTipoDispositivoModal.setPreferredSize(new java.awt.Dimension(150, 24));
        bodyModal.add(cmbTipoDispositivoModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, 220, -1));

        lbMarca.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbMarca.setForeground(new java.awt.Color(102, 102, 102));
        lbMarca.setText("Marca");
        bodyModal.add(lbMarca, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 160, -1, -1));

        txtMarca.setBackground(new java.awt.Color(255, 255, 255));
        txtMarca.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtMarca.setForeground(new java.awt.Color(51, 51, 51));
        bodyModal.add(txtMarca, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 190, 220, -1));

        lbModelo.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbModelo.setForeground(new java.awt.Color(102, 102, 102));
        lbModelo.setText("Modelo");
        bodyModal.add(lbModelo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 240, -1, -1));

        txtModelo.setBackground(new java.awt.Color(255, 255, 255));
        txtModelo.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtModelo.setForeground(new java.awt.Color(51, 51, 51));
        bodyModal.add(txtModelo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 270, 220, -1));

        lbNoSerie.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbNoSerie.setForeground(new java.awt.Color(102, 102, 102));
        lbNoSerie.setText("No. Serie");
        bodyModal.add(lbNoSerie, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 240, -1, -1));

        txtNoSerie.setBackground(new java.awt.Color(255, 255, 255));
        txtNoSerie.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtNoSerie.setForeground(new java.awt.Color(51, 51, 51));
        bodyModal.add(txtNoSerie, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 270, 220, -1));

        lbLaboratorioModal.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbLaboratorioModal.setForeground(new java.awt.Color(102, 102, 102));
        lbLaboratorioModal.setText("Laboratorio");
        bodyModal.add(lbLaboratorioModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 310, -1, -1));

        cmbLaboratorioModal.setBackground(new java.awt.Color(255, 255, 255));
        cmbLaboratorioModal.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        cmbLaboratorioModal.setForeground(new java.awt.Color(102, 102, 102));
        cmbLaboratorioModal.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Selecciona" }));
        cmbLaboratorioModal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        bodyModal.add(cmbLaboratorioModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 340, 220, -1));

        lbOberservaciones.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        lbOberservaciones.setForeground(new java.awt.Color(102, 102, 102));
        lbOberservaciones.setText("Observaciones");
        bodyModal.add(lbOberservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 310, -1, -1));

        jScrollPane3.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane3.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        txtObservaciones.setColumns(20);
        txtObservaciones.setRows(5);
        txtObservaciones.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        jScrollPane3.setViewportView(txtObservaciones);

        bodyModal.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 340, 220, 220));

        btnGuardar.setBackground(new java.awt.Color(8, 173, 141));
        btnGuardar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnGuardar.setForeground(new java.awt.Color(255, 255, 255));
        btnGuardar.setText("Guardar");
        btnGuardar.setBorderPainted(false);
        btnGuardar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnGuardar.setFocusPainted(false);
        btnGuardar.setPreferredSize(new java.awt.Dimension(200, 50));
        btnGuardar.addActionListener(this::btnGuardarActionPerformed);
        bodyModal.add(btnGuardar, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 420, 160, 40));

        btnCancelar.setBackground(new java.awt.Color(108, 117, 125));
        btnCancelar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCancelar.setForeground(new java.awt.Color(255, 255, 255));
        btnCancelar.setText("Cancelar");
        btnCancelar.setBorderPainted(false);
        btnCancelar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setPreferredSize(new java.awt.Dimension(200, 50));
        btnCancelar.addActionListener(this::btnCancelarActionPerformed);
        bodyModal.add(btnCancelar, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 480, 160, 40));

        lbTituloModal.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTituloModal.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloModal.setText("Registrar Equipo");
        bodyModal.add(lbTituloModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 25, -1, -1));

        javax.swing.GroupLayout addEquipoLayout = new javax.swing.GroupLayout(addEquipo.getContentPane());
        addEquipo.getContentPane().setLayout(addEquipoLayout);
        addEquipoLayout.setHorizontalGroup(
            addEquipoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(bodyModal, javax.swing.GroupLayout.PREFERRED_SIZE, 567, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        addEquipoLayout.setVerticalGroup(
            addEquipoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(bodyModal, javax.swing.GroupLayout.DEFAULT_SIZE, 585, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Inventario");
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

        btnMant.setBackground(new java.awt.Color(255, 255, 255));
        btnMant.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnMant.setForeground(new java.awt.Color(6, 140, 115));
        btnMant.setText("Mantenimiento");
        btnMant.setBorderPainted(false);
        btnMant.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnMant.setFocusPainted(false);
        btnMant.setPreferredSize(new java.awt.Dimension(200, 50));
        btnMant.addActionListener(this::btnMantActionPerformed);
        sidebar.add(btnMant, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 410, -1, -1));

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
        lbTitulo.setText("Control de Inventario");
        header.add(lbTitulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 30, -1, -1));

        txtBuscar.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtBuscar.setPreferredSize(new java.awt.Dimension(68, 30));
        header.add(txtBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 80, 240, 30));

        estado.setBackground(new java.awt.Color(255, 255, 255));
        estado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        estado.setForeground(new java.awt.Color(102, 102, 102));
        estado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Disponible", "En uso", "Con falla", "En mantenimiento", "Dado de baja" }));
        estado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        estado.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        estado.setPreferredSize(new java.awt.Dimension(150, 30));
        estado.addActionListener(this::estadoActionPerformed);
        header.add(estado, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 80, -1, -1));

        laboratorio.setBackground(new java.awt.Color(255, 255, 255));
        laboratorio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        laboratorio.setForeground(new java.awt.Color(102, 102, 102));
        laboratorio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos" }));
        laboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        laboratorio.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        laboratorio.setPreferredSize(new java.awt.Dimension(150, 30));
        header.add(laboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 80, -1, -1));

        dispositivo.setBackground(new java.awt.Color(255, 255, 255));
        dispositivo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        dispositivo.setForeground(new java.awt.Color(102, 102, 102));
        dispositivo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Computadora", "Monitor", "Teclado", "Mouse", "Proyector", "Extensión", "HDMI", "Otro" }));
        dispositivo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        dispositivo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        dispositivo.setPreferredSize(new java.awt.Dimension(150, 30));
        header.add(dispositivo, new org.netbeans.lib.awtextra.AbsoluteConstraints(680, 80, -1, -1));

        lbLaboratorio.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        lbLaboratorio.setText("Laboratorio");
        header.add(lbLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 60, -1, -1));

        lbEstado.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbEstado.setForeground(new java.awt.Color(102, 102, 102));
        lbEstado.setText("Estado");
        header.add(lbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 60, -1, -1));

        lbTipoDispositivo.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbTipoDispositivo.setForeground(new java.awt.Color(102, 102, 102));
        lbTipoDispositivo.setText("Tipo de Dispositivo");
        header.add(lbTipoDispositivo, new org.netbeans.lib.awtextra.AbsoluteConstraints(680, 60, -1, -1));

        btnLimpiar.setBackground(new java.awt.Color(8, 173, 141));
        btnLimpiar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnLimpiar.setForeground(new java.awt.Color(255, 255, 255));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.setPreferredSize(new java.awt.Dimension(125, 30));
        btnLimpiar.addActionListener(this::btnLimpiarActionPerformed);
        header.add(btnLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 40, -1, -1));

        btnBuscar.setBackground(new java.awt.Color(8, 173, 141));
        btnBuscar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnBuscar.setForeground(new java.awt.Color(255, 255, 255));
        btnBuscar.setText("Buscar");
        btnBuscar.setBorderPainted(false);
        btnBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBuscar.setFocusPainted(false);
        btnBuscar.setPreferredSize(new java.awt.Dimension(125, 30));
        btnBuscar.addActionListener(this::btnBuscarActionPerformed);
        header.add(btnBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 80, -1, -1));

        body.setBackground(new java.awt.Color(204, 204, 204));
        body.setForeground(new java.awt.Color(102, 102, 102));
        body.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tablaInventario.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        tablaInventario.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Código", "Nombre Equipo", "Tipo de Dispositivo", "Marca", "Modelo", "No. Serie", "Laboratorio", "Estado", "Último Mantenimiento", "Observaciones"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaInventario.setRowHeight(36);
        jScrollPane1.setViewportView(tablaInventario);

        body.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 20, 1000, 420));

        btnExportar.setBackground(new java.awt.Color(90, 90, 90));
        btnExportar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnExportar.setForeground(new java.awt.Color(255, 255, 255));
        btnExportar.setText("Exportar");
        btnExportar.setBorderPainted(false);
        btnExportar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnExportar.setFocusPainted(false);
        btnExportar.setPreferredSize(new java.awt.Dimension(130, 40));
        btnExportar.addActionListener(this::btnExportarActionPerformed);
        body.add(btnExportar, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 500, -1, -1));

        btnRegistrar.setBackground(new java.awt.Color(6, 140, 115));
        btnRegistrar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnRegistrar.setForeground(new java.awt.Color(255, 255, 255));
        btnRegistrar.setText("Registrar Equipo");
        btnRegistrar.setBorderPainted(false);
        btnRegistrar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnRegistrar.setFocusPainted(false);
        btnRegistrar.setPreferredSize(new java.awt.Dimension(160, 40));
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
        body.add(btnEditar, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 500, 120, 40));

        btnDarBaja.setBackground(new java.awt.Color(220, 53, 69));
        btnDarBaja.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnDarBaja.setForeground(new java.awt.Color(255, 255, 255));
        btnDarBaja.setText("Dar de Baja");
        btnDarBaja.setBorderPainted(false);
        btnDarBaja.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnDarBaja.setFocusPainted(false);
        btnDarBaja.setPreferredSize(new java.awt.Dimension(130, 40));
        btnDarBaja.addActionListener(this::btnDarBajaActionPerformed);
        body.add(btnDarBaja, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 500, -1, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sidebar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(header, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

    private void btnBitacoraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBitacoraActionPerformed
        Bitacora ventanaBitacora = new Bitacora(nombreUsuario);
        
        ventanaBitacora.setVisible(true);
        ventanaBitacora.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnBitacoraActionPerformed

    private void btnInicioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInicioActionPerformed
        DashboardLabo ventanaDashboard = new DashboardLabo(nombreUsuario);
        
        ventanaDashboard.setVisible(true);
        ventanaDashboard.setLocationRelativeTo(null);
        
        this.dispose();
    }//GEN-LAST:event_btnInicioActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        int filaSeleccionada = tablaInventario.getSelectedRow();

        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona un equipo de la tabla para editar.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int filaModelo = tablaInventario.convertRowIndexToModel(
            filaSeleccionada
        );

        modoEdicion = true;

        codigoOriginal = tablaInventario
                .getModel()
                .getValueAt(filaModelo, 0)
                .toString();

        txtCodigo.setText(
            tablaInventario.getModel().getValueAt(filaModelo, 0).toString()
        );

        txtNombreEquipo.setText("N/A");

        cmbTipoDispositivoModal.setSelectedItem(
            tablaInventario.getModel().getValueAt(filaModelo, 2).toString()
        );

        txtMarca.setText(
            tablaInventario.getModel().getValueAt(filaModelo, 3).toString()
        );

        txtModelo.setText(
            tablaInventario.getModel().getValueAt(filaModelo, 4).toString()
        );

        txtNoSerie.setText(
            tablaInventario.getModel().getValueAt(filaModelo, 5).toString()
        );

        cmbLaboratorioModal.setSelectedItem(
            tablaInventario.getModel().getValueAt(filaModelo, 6).toString()
        );

        Object observaciones = tablaInventario
                .getModel()
                .getValueAt(filaModelo, 9);

        txtObservaciones.setText(
            observaciones != null ? observaciones.toString() : ""
        );

        lbTituloModal.setText("Editar Equipo");
        btnGuardar.setText("Actualizar");

        addEquipo.pack();
        addEquipo.setLocationRelativeTo(this);
        addEquipo.setVisible(true);
    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnRegistrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarActionPerformed
        modoEdicion = false;
        codigoOriginal = "";
        
        limpiarCamposEquipo();
        
        lbTituloModal.setText("Registrar Equipo");
        btnGuardar.setText("Guardar");
        
        addEquipo.pack();
        addEquipo.setSize(567, 560);
        addEquipo.setLocationRelativeTo(this);
        addEquipo.setVisible(true);
    }//GEN-LAST:event_btnRegistrarActionPerformed

    private void estadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_estadoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_estadoActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        limpiarCamposEquipo();
        
        modoEdicion = false;
        codigoOriginal = "";
        
        lbTituloModal.setText("Registrar Equipo");
        btnGuardar.setText("Guardar");
        
        addEquipo.setVisible(false);
    }//GEN-LAST:event_btnCancelarActionPerformed

    // BOTON GUARDAR REGISTRO DENTRO DEL MODAL
    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        if (!validarCamposEquipo()) {
            return;
        }

        if (modoEdicion) {
            String codigoAnterior = codigoOriginal;
            String nuevoCodigo = txtCodigo.getText().trim();
            String nuevoLaboratorio
                    = cmbLaboratorioModal.getSelectedItem().toString();

            boolean actualizado = actualizarEquipoBD();

            if (actualizado) {
                actualizarMantenimientosDelEquipo(
                    codigoAnterior,
                    nuevoCodigo,
                    nuevoLaboratorio
                );

                javax.swing.JOptionPane.showMessageDialog(
                    addEquipo,
                    "Equipo actualizado correctamente.",
                    "Actualización exitosa",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );

                limpiarCamposEquipo();
                addEquipo.setVisible(false);
                cargarTablaInventarioFiltrada();

                modoEdicion = false;
                codigoOriginal = "";
                lbTituloModal.setText("Registrar Equipo");
                btnGuardar.setText("Guardar");
            }

        } else {
            boolean guardado = guardarEquipoBD();

            if (guardado) {
                javax.swing.JOptionPane.showMessageDialog(
                    addEquipo,
                    "Equipo registrado correctamente.",
                    "Registro exitoso",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );

                limpiarCamposEquipo();
                addEquipo.setVisible(false);
                cargarTablaInventarioFiltrada();
            }
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        cargarTablaInventarioFiltrada();
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarFiltrosInventario();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnDarBajaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDarBajaActionPerformed
        int filaSeleccionada = tablaInventario.getSelectedRow();

        if (filaSeleccionada == -1) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Selecciona un equipo para darlo de baja.",
                "Sin selección",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int filaModelo = tablaInventario.convertRowIndexToModel(filaSeleccionada);

        String codigo = tablaInventario
                .getModel()
                .getValueAt(filaModelo, 0)
                .toString();

        if (tieneMantenimientoPendiente(codigo)) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No puedes dar de baja este equipo porque tiene un mantenimiento pendiente o en proceso.",
                "Mantenimiento activo",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro que deseas dar de baja este equipo?",
            "Confirmar baja",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );

        if (confirmacion != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        darDeBajaEquipo(codigo);
    }//GEN-LAST:event_btnDarBajaActionPerformed

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

    private void btnExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarActionPerformed
        exportarTablaInventarioExcel();
    }//GEN-LAST:event_btnExportarActionPerformed

    private void btnReporteFallasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReporteFallasActionPerformed
        ReporteFalla ventanaReporteFalla = new ReporteFalla(nombreUsuario);
        
        ventanaReporteFalla.setVisible(true);
        ventanaReporteFalla.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReporteFallasActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Inventario().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDialog addEquipo;
    private javax.swing.JPanel body;
    private javax.swing.JPanel bodyModal;
    private javax.swing.JButton btnBitacora;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnDarBaja;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnInicio;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnMant;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JButton btnReporteFallas;
    private javax.swing.JButton btnReservas;
    private javax.swing.JComboBox<String> cmbLaboratorioModal;
    private javax.swing.JComboBox<String> cmbTipoDispositivoModal;
    private javax.swing.JComboBox<String> dispositivo;
    private javax.swing.JComboBox<String> estado;
    private javax.swing.JPanel header;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JComboBox<String> laboratorio;
    private javax.swing.JLabel lbCodigo;
    private javax.swing.JLabel lbEstado;
    private javax.swing.JLabel lbLaboratorio;
    private javax.swing.JLabel lbLaboratorioModal;
    private javax.swing.JLabel lbMarca;
    private javax.swing.JLabel lbModelo;
    private javax.swing.JLabel lbNoSerie;
    private javax.swing.JLabel lbNombreEquipo;
    private javax.swing.JLabel lbOberservaciones;
    private javax.swing.JLabel lbTipoDispositivo;
    private javax.swing.JLabel lbTipoDispositivoModal;
    private javax.swing.JLabel lbTitulo;
    private javax.swing.JLabel lbTituloModal;
    private javax.swing.JPanel linea;
    private javax.swing.JPanel sidebar;
    private javax.swing.JTable tablaInventario;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtCodigo;
    private javax.swing.JTextField txtMarca;
    private javax.swing.JTextField txtModelo;
    private javax.swing.JTextField txtNoSerie;
    private javax.swing.JTextField txtNombreEquipo;
    private javax.swing.JTextArea txtObservaciones;
    // End of variables declaration//GEN-END:variables
}
