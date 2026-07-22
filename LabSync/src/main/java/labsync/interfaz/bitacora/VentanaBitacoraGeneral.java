package labsync.interfaz.bitacora;

import labsync.interfaz.comun.ActualizacionAutomatica;
import labsync.aplicacion.AplicacionLabSync;
import labsync.configuracion.ConexionBaseDatos;
import labsync.persistencia.ConsultaTabla;
import labsync.interfaz.inventario.VentanaGestionInventario;
import labsync.interfaz.mantenimiento.VentanaGestionMantenimiento;
import labsync.interfaz.fallas.VentanaGestionReportesFallas;
import labsync.interfaz.reservas.VentanaGestionReservas;
import labsync.interfaz.panel.VentanaPanelLaboratorista;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.FileOutputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;

/** Muestra al laboratorista la bitácora general con filtros y exportación. */
public class VentanaBitacoraGeneral extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(VentanaBitacoraGeneral.class.getName());
    private String nombreUsuario;
    private final String PH_BUSCAR = "Usuario, laboratorio, actividad o fecha";
    private final Color COLOR_PLACEHOLDER = new Color(150, 150, 150);
    private final Color COLOR_TEXTO = new Color(51, 51, 51);

    public VentanaBitacoraGeneral(String nombreRecibido) {
        initComponents();
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        
        this.nombreUsuario = nombreRecibido;
        labsync.interfaz.comun.NotificacionesGlobales.laboratorista(this, header, nombreUsuario);
        
        ponerPlaceholderBuscar();
        cargarTablaBitacora();
        iniciarActualizacionAutomatica();
    }
    
    public VentanaBitacoraGeneral() {
        initComponents();
        this.nombreUsuario = "Usuario";
        labsync.interfaz.comun.NotificacionesGlobales.laboratorista(this, header, nombreUsuario);
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo_labsync_no_background.png")).getImage());
        
        panelContenedor.setSize(960, 60);
        panelContenedor.setPreferredSize(new java.awt.Dimension(960, 60));
        
        ponerPlaceholderBuscar();
        cargarTablaBitacora();
        iniciarActualizacionAutomatica();
    }

    private void iniciarActualizacionAutomatica() {
        new ActualizacionAutomatica<>(this, 7_000, () -> ConsultaTabla.ejecutar(
                "SELECT id_bitacora, DATE_FORMAT(fecha, '%d/%m/%Y') fecha, nombre_usuario, rol_usuario, laboratorio, actividad_materia, IFNULL(estado, 'Registrado') estado FROM bitacora ORDER BY fecha_registro DESC",
                new String[]{"ID", "Fecha", "Usuario", "Rol", "Laboratorio", "Actividad / Materia", "Estado"},
                new String[]{"id_bitacora", "fecha", "nombre_usuario", "rol_usuario", "laboratorio", "actividad_materia", "estado"}),
                modelo -> { tablaBitacora.setModel(modelo); ocultarColumnaID(); });
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
    
    private void limpiarFiltrosBitacora() {
        txtBuscar.setText(PH_BUSCAR);
        txtBuscar.setForeground(COLOR_PLACEHOLDER);

        cmbEstado.setSelectedIndex(0);
        
        cargarTablaBitacoraFiltrada();
    }
    
    private void ocultarColumnaID() {
        if (tablaBitacora.getColumnModel().getColumnCount() > 0) {
            tablaBitacora.getColumnModel().getColumn(0).setMinWidth(0);
            tablaBitacora.getColumnModel().getColumn(0).setMaxWidth(0);
            tablaBitacora.getColumnModel().getColumn(0).setWidth(0);
        }
    }
    
    private void cargarTablaBitacora() {
        DefaultTableModel modelo = new DefaultTableModel();
        Connection con = ConexionBaseDatos.conectar();

        modelo.addColumn("ID");
        modelo.addColumn("Fecha");
        modelo.addColumn("Usuario");
        modelo.addColumn("Rol");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Actividad / Materia");
        modelo.addColumn("Estado");

        String sql = "SELECT "
            + "id_bitacora, "
            + "DATE_FORMAT(fecha, '%d/%m/%Y') AS fecha, "
            + "nombre_usuario, "
            + "rol_usuario, "
            + "laboratorio, "
            + "actividad_materia, "
            + "IFNULL(estado, 'Registrado') AS estado "
            + "FROM bitacora "
            + "ORDER BY fecha_registro DESC";

        if (con == null) {
            JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] fila = new Object[7];

                fila[0] = rs.getInt("id_bitacora");
                fila[1] = rs.getString("fecha");
                fila[2] = rs.getString("nombre_usuario");
                fila[3] = rs.getString("rol_usuario");
                fila[4] = rs.getString("laboratorio");
                fila[5] = rs.getString("actividad_materia");
                fila[6] = rs.getString("estado");

                modelo.addRow(fila);
            }

            tablaBitacora.setModel(modelo);
            ocultarColumnaID();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Error al cargar bitácora: " + e.getMessage(),
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
    
    private void cargarDetalleBitacora(int idBitacora) {
        Connection con = ConexionBaseDatos.conectar();
        
        if (con == null) {
            JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        String sql = "SELECT "
        + "id_bitacora, "
        + "DATE_FORMAT(fecha, '%d/%m/%Y') AS fecha, "
        + "nombre_usuario, "
        + "rol_usuario, "
        + "carrera_dependencia, "
        + "IFNULL(grado, '') AS grado, "
        + "IFNULL(grupo, '') AS grupo, "
        + "laboratorio, "
        + "actividad_materia, "
        + "turno, "
        + "horario, "
        + "total_usuarios, "
        + "IFNULL(estado, 'Registrado') AS estado, "
        + "IFNULL(observaciones, '') AS observaciones "
        + "FROM bitacora "
        + "WHERE id_bitacora = ?";
        
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idBitacora);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                txtDetalleID.setText(String.valueOf(rs.getInt("id_bitacora")));
                txtDetalleEstado.setText(rs.getString("estado"));
                txtDetalleFecha.setText(rs.getString("fecha"));

                txtDetalleUsuario.setText(rs.getString("nombre_usuario"));
                txtDetalleRol.setText(rs.getString("rol_usuario"));
                txtDetalleCarrera.setText(rs.getString("carrera_dependencia"));

                txtDetalleLaboratorio.setText(rs.getString("laboratorio"));
                txtDetalleGrado.setText(rs.getString("grado"));
                txtDetalleGrupo.setText(rs.getString("grupo"));

                txtDetalleTurno.setText(rs.getString("turno"));
                txtDetalleHorario.setText(rs.getString("horario"));
                txtDetalleTotalUsuarios.setText(String.valueOf(rs.getInt("total_usuarios")));

                txtDetalleActividad.setText(rs.getString("actividad_materia"));
                txtDetalleObservaciones.setText(rs.getString("observaciones"));

                detalleBitacora.pack();
                detalleBitacora.setLocationRelativeTo(this);
                detalleBitacora.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "No se encontró el registro seleccionado.",
                    "Sin resultados",
                    JOptionPane.WARNING_MESSAGE
                );
            } 
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Error al cargar detalles de bitácora: " + e.getMessage(),
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
    
    private void cargarTablaBitacoraFiltrada() {
        DefaultTableModel modelo = new DefaultTableModel();
        Connection con = ConexionBaseDatos.conectar();

        modelo.addColumn("ID");
        modelo.addColumn("Fecha");
        modelo.addColumn("Usuario");
        modelo.addColumn("Rol");
        modelo.addColumn("Laboratorio");
        modelo.addColumn("Actividad / Materia");
        modelo.addColumn("Estado");

        String textoBusqueda = txtBuscar.getText().trim();
        String estadoSeleccionado = cmbEstado.getSelectedItem().toString();

        if (textoBusqueda.equals(PH_BUSCAR)) {
            textoBusqueda = "";
        }

        String sql = "SELECT "
            + "id_bitacora, "
            + "DATE_FORMAT(fecha, '%d/%m/%Y') AS fecha, "
            + "nombre_usuario, "
            + "rol_usuario, "
            + "laboratorio, "
            + "actividad_materia, "
            + "IFNULL(estado, 'Registrado') AS estado "
            + "FROM bitacora "
            + "WHERE 1=1 ";

        java.util.ArrayList<String> parametros = new java.util.ArrayList<>();

        if (!textoBusqueda.isEmpty()) {
            sql += "AND (nombre_usuario LIKE ? "
                + "OR rol_usuario LIKE ? "
                + "OR laboratorio LIKE ? "
                + "OR actividad_materia LIKE ? "
                + "OR DATE_FORMAT(fecha, '%d/%m/%Y') LIKE ? "
                + "OR DATE_FORMAT(fecha, '%Y-%m-%d') LIKE ?) ";

            String busqueda = "%" + textoBusqueda + "%";

            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
            parametros.add(busqueda);
        }

        if (!estadoSeleccionado.equals("Todos")) {
            sql += "AND estado = ? ";
            parametros.add(estadoSeleccionado);
        }

        sql += "ORDER BY fecha_registro DESC";

        if (con == null) {
            JOptionPane.showMessageDialog(
                this,
                "No hay conexión con la base de datos.",
                "Error de conexión",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            PreparedStatement ps = con.prepareStatement(sql);

            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] fila = new Object[7];

                fila[0] = rs.getInt("id_bitacora");
                fila[1] = rs.getString("fecha");
                fila[2] = rs.getString("nombre_usuario");
                fila[3] = rs.getString("rol_usuario");
                fila[4] = rs.getString("laboratorio");
                fila[5] = rs.getString("actividad_materia");
                fila[6] = rs.getString("estado");

                modelo.addRow(fila);
            }

            tablaBitacora.setModel(modelo);
            ocultarColumnaID();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Error al filtrar bitácora: " + e.getMessage(),
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
    
    private void limpiarDetalleBitacora() {
        txtDetalleID.setText("");
        txtDetalleEstado.setText("");
        txtDetalleFecha.setText("");
        txtDetalleUsuario.setText("");
        txtDetalleRol.setText("");
        txtDetalleCarrera.setText("");
        txtDetalleLaboratorio.setText("");
        txtDetalleGrado.setText("");
        txtDetalleGrupo.setText("");
        txtDetalleTurno.setText("");
        txtDetalleHorario.setText("");
        txtDetalleTotalUsuarios.setText("");
        txtDetalleActividad.setText("");
        txtDetalleObservaciones.setText("");
    }
    
    private void exportarTablaBitacoraExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar bit\u00E1cora");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo Excel (*.xlsx)", "xlsx"));
        fileChooser.setSelectedFile(new File("bitacora_labsync.xlsx"));

        int seleccion = fileChooser.showSaveDialog(this);

        if (seleccion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = fileChooser.getSelectedFile();

        if (!archivo.getName().toLowerCase().endsWith(".xlsx")) {
            archivo = new File(archivo.getAbsolutePath() + ".xlsx");
        }

        String textoBusqueda = txtBuscar.getText().trim();
        String estadoSeleccionado = cmbEstado.getSelectedItem().toString();

        if (textoBusqueda.equals(PH_BUSCAR)) {
            textoBusqueda = "";
        }

        String sql = "SELECT "
            + "id_bitacora, "
            + "DATE_FORMAT(fecha, '%d/%m/%Y') AS fecha, "
            + "nombre_usuario, "
            + "rol_usuario, "
            + "carrera_dependencia, "
            + "IFNULL(grado, '') AS grado, "
            + "IFNULL(grupo, '') AS grupo, "
            + "laboratorio, "
            + "actividad_materia, "
            + "turno, "
            + "horario, "
            + "total_usuarios, "
            + "IFNULL(estado, 'Registrado') AS estado, "
            + "IFNULL(observaciones, '') AS observaciones "
            + "FROM bitacora "
            + "WHERE 1=1 ";

        java.util.ArrayList<String> parametros = new java.util.ArrayList<>();

        if (!textoBusqueda.isEmpty()) {
            sql += "AND (nombre_usuario LIKE ? "
                + "OR rol_usuario LIKE ? "
                + "OR laboratorio LIKE ? "
                + "OR actividad_materia LIKE ? "
                + "OR carrera_dependencia LIKE ? "
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
        }

        if (!estadoSeleccionado.equals("Todos")) {
            sql += "AND estado = ? ";
            parametros.add(estadoSeleccionado);
        }

        sql += "ORDER BY fecha_registro DESC";

        Connection con = ConexionBaseDatos.conectar();

        if (con == null) {
            JOptionPane.showMessageDialog(
                this,
                "No hay conexi\u00F3n con la base de datos.",
                "Error de conexi\u00F3n",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try (
            Workbook workbook = new XSSFWorkbook();
            FileOutputStream salida = new FileOutputStream(archivo)
        ) {
            Sheet hoja = workbook.createSheet("Bitácora");

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
            celdaTitulo.setCellValue("Reporte de Bit\u00E1cora - LabSync");
            celdaTitulo.setCellStyle(estiloTitulo);

            hoja.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 12));

            Row filaEncabezado = hoja.createRow(2);

            String[] encabezados = {
                "Fecha",
                "Usuario",
                "Rol",
                "Carrera / Dependencia",
                "Grado",
                "Grupo",
                "Laboratorio",
                "Actividad / Materia",
                "Turno",
                "Horario",
                "Total Usuarios",
                "Estado",
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
                    rs.getString("fecha"),
                    rs.getString("nombre_usuario"),
                    rs.getString("rol_usuario"),
                    rs.getString("carrera_dependencia"),
                    rs.getString("grado"),
                    rs.getString("grupo"),
                    rs.getString("laboratorio"),
                    rs.getString("actividad_materia"),
                    rs.getString("turno"),
                    rs.getString("horario"),
                    rs.getInt("total_usuarios"),
                    rs.getString("estado"),
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
                JOptionPane.showMessageDialog(
                    this,
                    "No hay datos para exportar con los filtros actuales.",
                    "Sin datos",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            hoja.createFreezePane(0, 3);
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, filaExcel - 1, 0, encabezados.length - 1));

            for (int col = 0; col < encabezados.length; col++) {
                hoja.autoSizeColumn(col);
            }

            workbook.write(salida);

            JOptionPane.showMessageDialog(
                this,
                "La bit\u00E1cora se export\u00F3 correctamente.",
                "Exportaci\u00F3n exitosa",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                "Error al exportar bit\u00E1cora: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
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

        detalleBitacora = new javax.swing.JDialog();
        body = new javax.swing.JPanel();
        lbTituloModal = new javax.swing.JLabel();
        linea = new javax.swing.JPanel();
        lbDetalleID = new javax.swing.JLabel();
        txtDetalleID = new javax.swing.JTextField();
        lbDetalleEstado = new javax.swing.JLabel();
        txtDetalleEstado = new javax.swing.JTextField();
        lbDetalleFecha = new javax.swing.JLabel();
        txtDetalleFecha = new javax.swing.JTextField();
        lbDetalleUsuario = new javax.swing.JLabel();
        txtDetalleUsuario = new javax.swing.JTextField();
        lbDetalleRol = new javax.swing.JLabel();
        txtDetalleRol = new javax.swing.JTextField();
        lbDetalleCarrera = new javax.swing.JLabel();
        txtDetalleCarrera = new javax.swing.JTextField();
        lbDetalleLaboratorio = new javax.swing.JLabel();
        txtDetalleLaboratorio = new javax.swing.JTextField();
        lbDetalleGrado = new javax.swing.JLabel();
        txtDetalleGrado = new javax.swing.JTextField();
        lbDetalleGrupo = new javax.swing.JLabel();
        txtDetalleGrupo = new javax.swing.JTextField();
        lbDetalleTurno = new javax.swing.JLabel();
        txtDetalleTurno = new javax.swing.JTextField();
        lbDetalleTotalUsuarios = new javax.swing.JLabel();
        txtDetalleTotalUsuarios = new javax.swing.JTextField();
        lbDetalleHorario = new javax.swing.JLabel();
        txtDetalleHorario = new javax.swing.JTextField();
        lbDetalleActividad = new javax.swing.JLabel();
        scrollDetalleActividad = new javax.swing.JScrollPane();
        txtDetalleActividad = new javax.swing.JTextArea();
        lbDetalleObservaciones = new javax.swing.JLabel();
        scrollDetalleObservaciones = new javax.swing.JScrollPane();
        txtDetalleObservaciones = new javax.swing.JTextArea();
        btnCerrar = new javax.swing.JButton();
        sidebar = new javax.swing.JPanel();
        imgLabSync = new javax.swing.JLabel();
        btnInicio = new javax.swing.JButton();
        btnInventario = new javax.swing.JButton();
        btnMant = new javax.swing.JButton();
        btnReservas = new javax.swing.JButton();
        btnReporteFallas = new javax.swing.JButton();
        panelContenedor = new javax.swing.JPanel();
        header = new javax.swing.JPanel();
        txtBuscar = new javax.swing.JTextField();
        cmbEstado = new javax.swing.JComboBox<>();
        btnLimpiar = new javax.swing.JButton();
        lbEstado = new javax.swing.JLabel();
        lbTitulo = new javax.swing.JLabel();
        btnBuscar = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaBitacora = new javax.swing.JTable();
        btnVerDetalles = new javax.swing.JButton();
        btnExportar = new javax.swing.JButton();

        detalleBitacora.setTitle("Detalles de Bitácora");
        detalleBitacora.setMinimumSize(new java.awt.Dimension(630, 680));
        detalleBitacora.setModal(true);
        detalleBitacora.setResizable(false);

        body.setBackground(new java.awt.Color(245, 245, 245));
        body.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lbTituloModal.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTituloModal.setForeground(new java.awt.Color(102, 102, 102));
        lbTituloModal.setText("Detalles de Bitácora");
        body.add(lbTituloModal, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 25, -1, -1));

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

        body.add(linea, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 65, 560, 3));

        lbDetalleID.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleID.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleID.setText("ID Bitácora");
        body.add(lbDetalleID, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, -1, -1));

        txtDetalleID.setEditable(false);
        txtDetalleID.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleID.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleID.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleID.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleID, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 115, 110, 24));

        lbDetalleEstado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleEstado.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleEstado.setText("Estado");
        body.add(lbDetalleEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 90, -1, -1));

        txtDetalleEstado.setEditable(false);
        txtDetalleEstado.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleEstado.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleEstado.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleEstado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 115, 160, 24));

        lbDetalleFecha.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleFecha.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleFecha.setText("Fecha");
        body.add(lbDetalleFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 90, -1, -1));

        txtDetalleFecha.setEditable(false);
        txtDetalleFecha.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleFecha.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleFecha.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleFecha.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleFecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 115, 230, 24));

        lbDetalleUsuario.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleUsuario.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleUsuario.setText("Usuario");
        body.add(lbDetalleUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 155, -1, -1));

        txtDetalleUsuario.setEditable(false);
        txtDetalleUsuario.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleUsuario.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleUsuario.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleUsuario.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 180, 260, 24));

        lbDetalleRol.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleRol.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleRol.setText("Rol");
        body.add(lbDetalleRol, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 155, -1, -1));

        txtDetalleRol.setEditable(false);
        txtDetalleRol.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleRol.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleRol.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleRol.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleRol, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 180, 270, 24));

        lbDetalleCarrera.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleCarrera.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleCarrera.setText("Carrera");
        body.add(lbDetalleCarrera, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 220, -1, -1));

        txtDetalleCarrera.setEditable(false);
        txtDetalleCarrera.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleCarrera.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleCarrera.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleCarrera.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleCarrera, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 245, 260, 24));

        lbDetalleLaboratorio.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleLaboratorio.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleLaboratorio.setText("Laboratorio");
        body.add(lbDetalleLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 220, -1, -1));

        txtDetalleLaboratorio.setEditable(false);
        txtDetalleLaboratorio.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleLaboratorio.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleLaboratorio.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleLaboratorio.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleLaboratorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 245, 270, 24));

        lbDetalleGrado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleGrado.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleGrado.setText("Grado");
        body.add(lbDetalleGrado, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 285, -1, -1));

        txtDetalleGrado.setEditable(false);
        txtDetalleGrado.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleGrado.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleGrado.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleGrado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleGrado, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 310, 110, 24));

        lbDetalleGrupo.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleGrupo.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleGrupo.setText("Grupo");
        body.add(lbDetalleGrupo, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 285, -1, -1));

        txtDetalleGrupo.setEditable(false);
        txtDetalleGrupo.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleGrupo.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleGrupo.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleGrupo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleGrupo, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 310, 120, 24));

        lbDetalleTurno.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleTurno.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleTurno.setText("Turno");
        body.add(lbDetalleTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 285, -1, -1));

        txtDetalleTurno.setEditable(false);
        txtDetalleTurno.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleTurno.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleTurno.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleTurno.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleTurno, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 310, 120, 24));

        lbDetalleTotalUsuarios.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleTotalUsuarios.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleTotalUsuarios.setText("Total Usuarios");
        body.add(lbDetalleTotalUsuarios, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 285, -1, -1));

        txtDetalleTotalUsuarios.setEditable(false);
        txtDetalleTotalUsuarios.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleTotalUsuarios.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleTotalUsuarios.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleTotalUsuarios.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleTotalUsuarios, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 310, 120, 24));

        lbDetalleHorario.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleHorario.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleHorario.setText("Horario");
        body.add(lbDetalleHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 355, -1, -1));

        txtDetalleHorario.setEditable(false);
        txtDetalleHorario.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleHorario.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleHorario.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleHorario.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        body.add(txtDetalleHorario, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 380, 260, 24));

        lbDetalleActividad.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleActividad.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleActividad.setText("Actividad / Materia");
        body.add(lbDetalleActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 355, -1, -1));

        txtDetalleActividad.setEditable(false);
        txtDetalleActividad.setBackground(new java.awt.Color(255, 255, 255));
        txtDetalleActividad.setColumns(20);
        txtDetalleActividad.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        txtDetalleActividad.setForeground(new java.awt.Color(51, 51, 51));
        txtDetalleActividad.setLineWrap(true);
        txtDetalleActividad.setRows(5);
        txtDetalleActividad.setWrapStyleWord(true);
        txtDetalleActividad.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        scrollDetalleActividad.setViewportView(txtDetalleActividad);

        body.add(scrollDetalleActividad, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 380, 270, 80));

        lbDetalleObservaciones.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        lbDetalleObservaciones.setForeground(new java.awt.Color(102, 102, 102));
        lbDetalleObservaciones.setText("Observaciones");
        body.add(lbDetalleObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 475, -1, -1));

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

        body.add(scrollDetalleObservaciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 500, 560, 70));

        btnCerrar.setBackground(new java.awt.Color(108, 117, 125));
        btnCerrar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnCerrar.setForeground(new java.awt.Color(255, 255, 255));
        btnCerrar.setText("Cerrar");
        btnCerrar.setBorderPainted(false);
        btnCerrar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCerrar.setFocusPainted(false);
        btnCerrar.addActionListener(this::btnCerrarActionPerformed);
        body.add(btnCerrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 590, 180, 40));

        javax.swing.GroupLayout detalleBitacoraLayout = new javax.swing.GroupLayout(detalleBitacora.getContentPane());
        detalleBitacora.getContentPane().setLayout(detalleBitacoraLayout);
        detalleBitacoraLayout.setHorizontalGroup(
            detalleBitacoraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(body, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        detalleBitacoraLayout.setVerticalGroup(
            detalleBitacoraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(body, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Bitácora");
        setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        setResizable(false);

        sidebar.setBackground(new java.awt.Color(8, 173, 141));
        sidebar.setPreferredSize(new java.awt.Dimension(250, 720));
        sidebar.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        imgLabSync.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/labsync_blanco_200.png"))); // NOI18N
        imgLabSync.setPreferredSize(new java.awt.Dimension(242, 200));
        sidebar.add(imgLabSync, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 24, 179, -1));

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

        btnInventario.setBackground(new java.awt.Color(255, 255, 255));
        btnInventario.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnInventario.setForeground(new java.awt.Color(6, 140, 115));
        btnInventario.setText("Inventario");
        btnInventario.setBorderPainted(false);
        btnInventario.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnInventario.setFocusPainted(false);
        btnInventario.setPreferredSize(new java.awt.Dimension(200, 50));
        btnInventario.addActionListener(this::btnInventarioActionPerformed);
        sidebar.add(btnInventario, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 350, -1, -1));

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

        panelContenedor.setBackground(new java.awt.Color(204, 204, 204));
        panelContenedor.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        header.setBackground(new java.awt.Color(255, 255, 255));
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        header.setMaximumSize(new java.awt.Dimension(960, 60));
        header.setMinimumSize(new java.awt.Dimension(960, 60));
        header.setPreferredSize(new java.awt.Dimension(1030, 60));
        header.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txtBuscar.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtBuscar.setForeground(new java.awt.Color(102, 102, 102));
        txtBuscar.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        txtBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        txtBuscar.setPreferredSize(new java.awt.Dimension(300, 30));
        header.add(txtBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 80, 240, -1));

        cmbEstado.setBackground(new java.awt.Color(255, 255, 255));
        cmbEstado.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        cmbEstado.setForeground(new java.awt.Color(102, 102, 102));
        cmbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Todos", "Registrado", "Activo", "Salida Registrada" }));
        cmbEstado.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        cmbEstado.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cmbEstado.setPreferredSize(new java.awt.Dimension(150, 30));
        header.add(cmbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 80, -1, -1));

        btnLimpiar.setBackground(new java.awt.Color(8, 173, 141));
        btnLimpiar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnLimpiar.setForeground(new java.awt.Color(255, 255, 255));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.setPreferredSize(new java.awt.Dimension(125, 30));
        btnLimpiar.addActionListener(this::btnLimpiarActionPerformed);
        header.add(btnLimpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 40, 125, 30));

        lbEstado.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        lbEstado.setForeground(new java.awt.Color(102, 102, 102));
        lbEstado.setText("Estado");
        header.add(lbEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 60, -1, -1));

        lbTitulo.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        lbTitulo.setForeground(new java.awt.Color(102, 102, 102));
        lbTitulo.setText("Control de Bitácora");
        header.add(lbTitulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(35, 30, -1, -1));

        btnBuscar.setBackground(new java.awt.Color(8, 173, 141));
        btnBuscar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnBuscar.setForeground(new java.awt.Color(255, 255, 255));
        btnBuscar.setText("Buscar");
        btnBuscar.setBorderPainted(false);
        btnBuscar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBuscar.setFocusPainted(false);
        btnBuscar.setPreferredSize(new java.awt.Dimension(125, 30));
        btnBuscar.addActionListener(this::btnBuscarActionPerformed);
        header.add(btnBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 80, -1, -1));

        panelContenedor.add(header, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, 150));

        tablaBitacora.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        tablaBitacora.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Fecha", "Usuario", "Rol", "Laboratorio", "Actividad / Materia", "Observaciones"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaBitacora.setRowHeight(36);
        jScrollPane1.setViewportView(tablaBitacora);

        panelContenedor.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 170, 930, 420));

        btnVerDetalles.setBackground(new java.awt.Color(6, 140, 115));
        btnVerDetalles.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnVerDetalles.setForeground(new java.awt.Color(255, 255, 255));
        btnVerDetalles.setText("Ver detalles");
        btnVerDetalles.setBorderPainted(false);
        btnVerDetalles.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnVerDetalles.setFocusPainted(false);
        btnVerDetalles.addActionListener(this::btnVerDetallesActionPerformed);
        panelContenedor.add(btnVerDetalles, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 640, 180, 40));

        btnExportar.setBackground(new java.awt.Color(90, 90, 90));
        btnExportar.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        btnExportar.setForeground(new java.awt.Color(255, 255, 255));
        btnExportar.setText("Exportar");
        btnExportar.setBorderPainted(false);
        btnExportar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnExportar.setFocusPainted(false);
        btnExportar.addActionListener(this::btnExportarActionPerformed);
        panelContenedor.add(btnExportar, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 640, 130, 40));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sidebar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelContenedor, javax.swing.GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sidebar, javax.swing.GroupLayout.DEFAULT_SIZE, 731, Short.MAX_VALUE)
            .addComponent(panelContenedor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnInicioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInicioActionPerformed
        VentanaPanelLaboratorista ventanaDashboard = new VentanaPanelLaboratorista(nombreUsuario);
        
        ventanaDashboard.setVisible(true);
        ventanaDashboard.setLocationRelativeTo(null);
        
        this.dispose();
    }//GEN-LAST:event_btnInicioActionPerformed

    private void btnInventarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInventarioActionPerformed
        VentanaGestionInventario ventanaInventario = new VentanaGestionInventario(nombreUsuario);
        
        ventanaInventario.setVisible(true);
        ventanaInventario.setLocationRelativeTo(null);
        
        this.dispose();
    }//GEN-LAST:event_btnInventarioActionPerformed

    private void btnMantActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMantActionPerformed
        VentanaGestionMantenimiento ventanaMant = new VentanaGestionMantenimiento(nombreUsuario);
        
        ventanaMant.setVisible(true);
        ventanaMant.setLocationRelativeTo(null);
        
        this.dispose();
    }//GEN-LAST:event_btnMantActionPerformed

    private void btnReservasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReservasActionPerformed
        VentanaGestionReservas ventanaReserva = new VentanaGestionReservas(nombreUsuario);
        
        ventanaReserva.setVisible(true);
        ventanaReserva.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReservasActionPerformed

    private void btnReporteFallasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReporteFallasActionPerformed
        VentanaGestionReportesFallas ventanaReporteFalla = new VentanaGestionReportesFallas(nombreUsuario);
        
        ventanaReporteFalla.setVisible(true);
        ventanaReporteFalla.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnReporteFallasActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiarFiltrosBitacora();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnVerDetallesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerDetallesActionPerformed
       int filaSeleccionada = tablaBitacora.getSelectedRow();
       
        if (filaSeleccionada == -1) {
           JOptionPane.showMessageDialog(
                this,
                "Selecciona un registro de la tabla.",
                "Sin selección",
                JOptionPane.WARNING_MESSAGE
            );
           return;
        }
        
        int filaModelo = tablaBitacora.convertRowIndexToModel(filaSeleccionada);
        
        int idBitacora = Integer.parseInt(
            tablaBitacora.getModel().getValueAt(filaModelo, 0).toString()
        );
        
        cargarDetalleBitacora(idBitacora);
    }//GEN-LAST:event_btnVerDetallesActionPerformed

    private void btnCerrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarActionPerformed
        detalleBitacora.setVisible(false);
        limpiarDetalleBitacora();
    }//GEN-LAST:event_btnCerrarActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        cargarTablaBitacoraFiltrada();
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarActionPerformed
        exportarTablaBitacoraExcel();
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
        java.awt.EventQueue.invokeLater(() -> new VentanaBitacoraGeneral().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel body;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCerrar;
    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnInicio;
    private javax.swing.JButton btnInventario;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnMant;
    private javax.swing.JButton btnReporteFallas;
    private javax.swing.JButton btnReservas;
    private javax.swing.JButton btnVerDetalles;
    private javax.swing.JComboBox<String> cmbEstado;
    private javax.swing.JDialog detalleBitacora;
    private javax.swing.JPanel header;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbDetalleActividad;
    private javax.swing.JLabel lbDetalleCarrera;
    private javax.swing.JLabel lbDetalleEstado;
    private javax.swing.JLabel lbDetalleFecha;
    private javax.swing.JLabel lbDetalleGrado;
    private javax.swing.JLabel lbDetalleGrupo;
    private javax.swing.JLabel lbDetalleHorario;
    private javax.swing.JLabel lbDetalleID;
    private javax.swing.JLabel lbDetalleLaboratorio;
    private javax.swing.JLabel lbDetalleObservaciones;
    private javax.swing.JLabel lbDetalleRol;
    private javax.swing.JLabel lbDetalleTotalUsuarios;
    private javax.swing.JLabel lbDetalleTurno;
    private javax.swing.JLabel lbDetalleUsuario;
    private javax.swing.JLabel lbEstado;
    private javax.swing.JLabel lbTitulo;
    private javax.swing.JLabel lbTituloModal;
    private javax.swing.JPanel linea;
    private javax.swing.JPanel panelContenedor;
    private javax.swing.JScrollPane scrollDetalleActividad;
    private javax.swing.JScrollPane scrollDetalleObservaciones;
    private javax.swing.JPanel sidebar;
    private javax.swing.JTable tablaBitacora;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextArea txtDetalleActividad;
    private javax.swing.JTextField txtDetalleCarrera;
    private javax.swing.JTextField txtDetalleEstado;
    private javax.swing.JTextField txtDetalleFecha;
    private javax.swing.JTextField txtDetalleGrado;
    private javax.swing.JTextField txtDetalleGrupo;
    private javax.swing.JTextField txtDetalleHorario;
    private javax.swing.JTextField txtDetalleID;
    private javax.swing.JTextField txtDetalleLaboratorio;
    private javax.swing.JTextArea txtDetalleObservaciones;
    private javax.swing.JTextField txtDetalleRol;
    private javax.swing.JTextField txtDetalleTotalUsuarios;
    private javax.swing.JTextField txtDetalleTurno;
    private javax.swing.JTextField txtDetalleUsuario;
    // End of variables declaration//GEN-END:variables
}
