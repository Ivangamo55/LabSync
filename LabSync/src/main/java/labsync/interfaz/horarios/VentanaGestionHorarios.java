package labsync.interfaz.horarios;

import labsync.configuracion.ConexionBaseDatos;
import labsync.interfaz.autenticacion.VentanaInicioSesion;
import labsync.interfaz.bitacora.VentanaBitacoraGeneral;
import labsync.interfaz.fallas.VentanaGestionReportesFallas;
import labsync.interfaz.inventario.VentanaGestionInventario;
import labsync.interfaz.mantenimiento.VentanaGestionMantenimiento;
import labsync.interfaz.panel.VentanaPanelLaboratorista;
import labsync.interfaz.reservas.VentanaGestionReservas;
import labsync.modelo.HorarioClase;
import labsync.modelo.OpcionesAcademicas;
import labsync.servicio.ServicioHorarios;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.DefaultTableCellRenderer;
import labsync.interfaz.comun.ActualizacionAutomatica;
import labsync.interfaz.comun.ActualizadorModeloCombo;
import labsync.interfaz.comun.ActualizadorModeloTabla;
import labsync.interfaz.comun.ControlDisponibilidadBaseDatos;

/** Administración Swing de ciclos y asignaciones regulares de laboratorios. */
public final class VentanaGestionHorarios extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(VentanaGestionHorarios.class.getName());
    private static final Color VERDE = new Color(8,173,141);
    private static final Color VERDE_OSCURO = new Color(6,140,115);
    private static final Color FONDO = new Color(245,247,249);
    private static final Color BORDE = new Color(213,219,225);
    private static final Color TEXTO = new Color(43,48,54);
    private static final Font FUENTE = new Font("Arial",Font.PLAIN,13);
    private static final List<LocalTime> INICIOS_MATUTINOS = List.of(
            LocalTime.of(7,0), LocalTime.of(7,50), LocalTime.of(9,10), LocalTime.of(10,0),
            LocalTime.of(10,50), LocalTime.of(11,40), LocalTime.of(12,30), LocalTime.of(13,20));
    private static final List<LocalTime> INICIOS_VESPERTINOS = List.of(
            LocalTime.of(15,0), LocalTime.of(15,50), LocalTime.of(17,10), LocalTime.of(18,0),
            LocalTime.of(18,50), LocalTime.of(19,40), LocalTime.of(20,30));
    private static final List<LocalTime> FINES_MATUTINOS = List.of(
            LocalTime.of(7,50), LocalTime.of(8,40), LocalTime.of(10,0), LocalTime.of(10,50),
            LocalTime.of(11,40), LocalTime.of(12,30), LocalTime.of(13,20), LocalTime.of(14,10));
    private static final List<LocalTime> FINES_VESPERTINOS = List.of(
            LocalTime.of(15,50), LocalTime.of(16,40), LocalTime.of(18,0), LocalTime.of(18,50),
            LocalTime.of(19,40), LocalTime.of(20,30), LocalTime.of(21,20));

    private final ServicioHorarios servicio = new ServicioHorarios();
    private final DefaultTableModel modeloHorarios = noEditable("ID","Ciclo","Carrera",
            "Cuatrimestre","Grupo","Turno","Materia","Profesor","Día","Horario",
            "Laboratorio","Estado");
    private final DefaultTableModel modeloCiclos = noEditable("ID","Ciclo","Inicio","Fin","Activo");
    private final JTable tablaHorarios = new JTable(modeloHorarios);
    private final JTable tablaCiclos = new JTable(modeloCiclos);
    private final JComboBox<String> filtroCiclo = new JComboBox<>();
    private final JComboBox<String> filtroCarrera = new JComboBox<>();
    private final JComboBox<String> filtroCuatrimestre = new JComboBox<>();
    private final JComboBox<String> filtroGrupo = new JComboBox<>();
    private final JComboBox<String> filtroTurno = new JComboBox<>();
    private final JComboBox<String> filtroDia = new JComboBox<>();
    private final JComboBox<String> filtroLaboratorio = new JComboBox<>();
    private final JComboBox<String> filtroProfesor = new JComboBox<>();
    private final JTextField campoBusqueda = new JTextField();
    private final JLabel etiquetaResultados = new JLabel("0 asignaciones");
    private final JPanel panelDetalle = new JPanel();
    private final List<JButton> accionesDependientesDatos = new ArrayList<>();
    private final ControlDisponibilidadBaseDatos controlConexion =
            new ControlDisponibilidadBaseDatos();
    private TableRowSorter<DefaultTableModel> ordenadorHorarios;
    private boolean actualizandoFiltros;
    private boolean filtrosInicializados;
    private String nombreUsuario;
    private labsync.modelo.SesionUsuario sesion;

    public VentanaGestionHorarios() {
        this(new labsync.modelo.SesionUsuario(0, "Usuario", "Usuario", "Laboratorista"));
    }

    public VentanaGestionHorarios(String nombreUsuario) {
        this(labsync.modelo.SesionUsuario.buscarLaboratorista(nombreUsuario));
    }

    public VentanaGestionHorarios(labsync.modelo.SesionUsuario sesionRecibida) {
        initComponents();
        labsync.interfaz.comun.LayoutLaboratorista.aplicar(
                this, sidebarVerde, headerBlanco, panelModulo);
        labsync.interfaz.comun.LayoutLaboratorista.normalizarSidebar(sidebarVerde);
        normalizarHeaderHorarios();
        this.sesion=sesionRecibida==null
                ?new labsync.modelo.SesionUsuario(0,"Usuario","Usuario","Laboratorista")
                :sesionRecibida;
        this.nombreUsuario=this.sesion.getNombre();
        lbNombreUsuario.setText("Hola, "+this.nombreUsuario);
        labsync.interfaz.comun.NotificacionesGlobales.laboratorista(
                this, headerBlanco, this.nombreUsuario);
        setIconImage(labsync.interfaz.comun.Recursos
                .icono("/images/logo_labsync_no_background.png").getImage());
        panelModulo.setLayout(new BorderLayout());
        panelModulo.setBackground(labsync.interfaz.comun.LayoutLaboratorista.FONDO_CONTENIDO);
        panelModulo.setBorder(null);
        JTabbedPane pestanas=new JTabbedPane();
        pestanas.setFont(new Font("Arial",Font.BOLD,13));
        pestanas.setBackground(labsync.interfaz.comun.LayoutLaboratorista.FONDO_CONTENIDO);
        pestanas.setForeground(VERDE_OSCURO);
        pestanas.addTab("Horarios de laboratorio",crearPanelHorarios());
        pestanas.addTab("Ciclos escolares",crearPanelCiclos());
        panelModulo.add(pestanas,BorderLayout.CENTER);
        cargarHorarios(); cargarCiclos();
        new ActualizacionAutomatica<ResultadoCarga<List<HorarioClase>>>(this,7_000,
                this::consultarHorariosSeguro, this::aplicarResultadoHorarios);
        setLocationRelativeTo(null);
    }

    private void normalizarHeaderHorarios() {
        imgUTJ.setVisible(true);
        headerBlanco.remove(imgUTJ);
        headerBlanco.add(imgUTJ,new org.netbeans.lib.awtextra.AbsoluteConstraints(40,25,-1,-1));
        JLabel titulo=new JLabel("<html><div style='text-align:center'>Ciclos y horarios</div></html>");
        titulo.setFont(new Font("Arial",Font.BOLD,14));
        titulo.setForeground(new Color(102,102,102));
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        headerBlanco.add(titulo,new org.netbeans.lib.awtextra.AbsoluteConstraints(410,20,155,60));
        lbNombreUsuario.setFont(new Font("Arial",Font.BOLD,16));
        lbNombreUsuario.setForeground(new Color(8,173,141));
        lbNombreUsuario.setHorizontalAlignment(SwingConstants.RIGHT);
        headerBlanco.remove(lbNombreUsuario);
        headerBlanco.add(lbNombreUsuario,new org.netbeans.lib.awtextra.AbsoluteConstraints(
                675,42,170,30));
        headerBlanco.remove(btnCerrarSesion);
        headerBlanco.add(btnCerrarSesion,new org.netbeans.lib.awtextra.AbsoluteConstraints(
                870,35,130,42));
    }

    private JPanel crearPanelHorarios() {
        tablaHorarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordenadorHorarios=new TableRowSorter<>(modeloHorarios);
        tablaHorarios.setRowSorter(ordenadorHorarios);
        configurarTabla(tablaHorarios);
        ajustarColumnasHorarios();
        tablaHorarios.getSelectionModel().addListSelectionListener(e->{
            if(!e.getValueIsAdjusting()) actualizarDetalle();
        });

        JPanel filtros=crearPanelFiltros();
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        botones.setOpaque(false);
        JButton nuevo=boton("+  Nueva asignación",VERDE,Color.WHITE);
        nuevo.addActionListener(e->editarHorario(null));
        JButton editar=boton("Editar",Color.WHITE,VERDE_OSCURO);
        editar.addActionListener(e->editarSeleccionado());
        JButton desactivar=boton("Activar/desactivar",Color.WHITE,new Color(205,55,67));
        desactivar.addActionListener(e->cambiarEstadoSeleccionado());
        JButton actualizar=boton("Actualizar",Color.WHITE,new Color(61,83,101));
        actualizar.addActionListener(e->cargarHorarios());
        accionesDependientesDatos.addAll(List.of(nuevo,editar,desactivar));
        botones.add(nuevo); botones.add(editar); botones.add(desactivar); botones.add(actualizar);

        JPanel barraAcciones=new JPanel(new BorderLayout());
        barraAcciones.setOpaque(false); barraAcciones.setBorder(BorderFactory.createEmptyBorder(12,0,10,0));
        barraAcciones.add(botones,BorderLayout.WEST);
        etiquetaResultados.setFont(new Font("Arial",Font.PLAIN,12));
        etiquetaResultados.setForeground(new Color(95,103,112));
        barraAcciones.add(etiquetaResultados,BorderLayout.EAST);

        JScrollPane desplazamiento=new JScrollPane(tablaHorarios);
        desplazamiento.setBorder(BorderFactory.createLineBorder(BORDE));
        desplazamiento.getViewport().setBackground(Color.WHITE);
        crearPanelDetalle();
        JSplitPane division=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,desplazamiento,panelDetalle);
        division.setResizeWeight(.78); division.setDividerSize(8); division.setBorder(null);
        division.setContinuousLayout(true);

        JPanel panel=new JPanel(new BorderLayout()); panel.setBackground(FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(20,
                labsync.interfaz.comun.LayoutLaboratorista.CONTENT_MARGIN,20,
                labsync.interfaz.comun.LayoutLaboratorista.CONTENT_MARGIN));
        panel.add(filtros,BorderLayout.NORTH);
        JPanel contenido=new JPanel(new BorderLayout()); contenido.setOpaque(false);
        contenido.add(barraAcciones,BorderLayout.NORTH); contenido.add(division,BorderLayout.CENTER);
        panel.add(contenido,BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelFiltros() {
        JPanel panel=new JPanel(new GridBagLayout()); panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(12,14,12,14)));
        JComboBox<?>[] combos={filtroCiclo,filtroCarrera,filtroCuatrimestre,filtroGrupo,
                filtroTurno,filtroDia,filtroLaboratorio,filtroProfesor};
        for(JComboBox<?> combo:combos){
            combo.setFont(FUENTE); combo.setBackground(Color.WHITE);
            combo.addActionListener(e->{if(!actualizandoFiltros) aplicarFiltros();});
        }
        GridBagConstraints g=new GridBagConstraints(); g.fill=GridBagConstraints.HORIZONTAL;
        g.weightx=1; g.insets=new Insets(0,0,10,14);
        agregarFiltro(panel,g,0,0,"Ciclo",filtroCiclo);
        agregarFiltro(panel,g,1,0,"Carrera",filtroCarrera);
        agregarFiltro(panel,g,2,0,"Cuatrimestre",filtroCuatrimestre);
        agregarFiltro(panel,g,3,0,"Grupo",filtroGrupo);
        agregarFiltro(panel,g,0,1,"Turno",filtroTurno);
        agregarFiltro(panel,g,1,1,"Día",filtroDia);
        agregarFiltro(panel,g,2,1,"Laboratorio",filtroLaboratorio);
        campoBusqueda.setFont(FUENTE); campoBusqueda.setToolTipText("Materia o profesor");
        campoBusqueda.addActionListener(e->aplicarFiltros());
        g.gridx=3;g.gridy=1;g.gridwidth=1;g.insets=new Insets(0,0,10,0);
        panel.add(campoConEtiqueta("Profesor",filtroProfesor),g);
        g.gridx=0;g.gridy=2;g.gridwidth=3;g.insets=new Insets(0,0,10,14);
        panel.add(campoConEtiqueta("Materia o profesor",campoBusqueda),g);
        JPanel acciones=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); acciones.setOpaque(false);
        JButton limpiar=boton("Limpiar",Color.WHITE,new Color(61,83,101));
        limpiar.addActionListener(e->limpiarFiltros());
        JButton buscar=boton("Buscar",VERDE,Color.WHITE); buscar.addActionListener(e->aplicarFiltros());
        acciones.add(limpiar); acciones.add(buscar);
        g.gridx=3;g.gridy=2;g.gridwidth=1;g.weightx=1;g.insets=new Insets(0,0,0,0);
        panel.add(acciones,g);
        return panel;
    }

    private void agregarFiltro(JPanel panel,GridBagConstraints base,int x,int y,String texto,Component componente){
        GridBagConstraints g=(GridBagConstraints)base.clone(); g.gridx=x;g.gridy=y;
        if(x==3)g.insets=new Insets(0,0,10,0);
        panel.add(campoConEtiqueta(texto,componente),g);
    }

    private JPanel campoConEtiqueta(String texto,Component componente){
        JPanel panel=new JPanel(new BorderLayout(0,4)); panel.setOpaque(false);
        JLabel etiqueta=new JLabel(texto); etiqueta.setFont(new Font("Arial",Font.BOLD,11));
        etiqueta.setForeground(new Color(83,91,99)); panel.add(etiqueta,BorderLayout.NORTH);
        componente.setPreferredSize(new Dimension(190,30)); panel.add(componente,BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelCiclos() {
        configurarTabla(tablaCiclos);
        ajustarColumnasCiclos();
        JPanel botones=new JPanel(new FlowLayout(FlowLayout.LEFT,10,10)); botones.setOpaque(false);
        JButton nuevo=boton("+  Nuevo ciclo",VERDE,Color.WHITE); nuevo.addActionListener(e->editarCiclo(null));
        JButton editar=boton("Editar ciclo",Color.WHITE,VERDE_OSCURO); editar.addActionListener(e->{Integer id=idSeleccionado(tablaCiclos); if(id!=null) editarCiclo(id);});
        JButton actualizar=boton("Actualizar",Color.WHITE,new Color(61,83,101)); actualizar.addActionListener(e->cargarCiclos());
        accionesDependientesDatos.addAll(List.of(nuevo,editar));
        botones.add(nuevo); botones.add(editar); botones.add(actualizar);
        JPanel panel=new JPanel(new BorderLayout()); panel.setBackground(FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(20,
                labsync.interfaz.comun.LayoutLaboratorista.CONTENT_MARGIN,20,
                labsync.interfaz.comun.LayoutLaboratorista.CONTENT_MARGIN));
        panel.add(botones,BorderLayout.NORTH);
        JScrollPane desplazamiento=new JScrollPane(tablaCiclos); desplazamiento.setBorder(BorderFactory.createLineBorder(BORDE));
        desplazamiento.getViewport().setBackground(Color.WHITE);
        panel.add(desplazamiento,BorderLayout.CENTER); return panel;
    }

    private void cargarHorarios() {
        new javax.swing.SwingWorker<ResultadoCarga<List<HorarioClase>>,Void>(){
            @Override protected ResultadoCarga<List<HorarioClase>> doInBackground(){
                return consultarHorariosSeguro();
            }
            @Override protected void done(){
                try{aplicarResultadoHorarios(get());}
                catch(InterruptedException ex){Thread.currentThread().interrupt();}
                catch(ExecutionException ex){manejarFalloConexion(ex.getCause());}
            }
        }.execute();
    }

    private ResultadoCarga<List<HorarioClase>> consultarHorariosSeguro(){
        try(Connection recibida=ConexionBaseDatos.conectar()){
            Connection con=ControlDisponibilidadBaseDatos.requerir(recibida);
            return ResultadoCarga.exito(servicio.consultarTodos(con));
        }catch(Exception ex){return ResultadoCarga.fallo(ex);}
    }

    private void aplicarResultadoHorarios(ResultadoCarga<List<HorarioClase>> resultado){
        if(resultado.error()!=null){manejarFalloConexion(resultado.error());return;}
        registrarConexionDisponible();
        aplicarHorarios(resultado.datos());
    }

    private void aplicarHorarios(List<HorarioClase> horarios){
        DefaultTableModel nuevos=noEditable("ID","Ciclo","Carrera","Cuatrimestre","Grupo","Turno","Materia","Profesor","Día","Horario","Laboratorio","Estado");
        for(HorarioClase h:horarios)nuevos.addRow(new Object[]{h.id(),h.ciclo(),h.carrera(),h.cuatrimestre(),h.grupo(),h.turno(),h.materia(),h.profesor(),h.dia(),h.intervalo(),h.laboratorio(),h.activo()?"Activo":"Inactivo"});
        boolean datosCambiaron=ActualizadorModeloTabla.aplicar(
                tablaHorarios,nuevos,0,this::ajustarColumnasHorarios);
        if(!filtrosInicializados||datosCambiaron){
            cargarOpcionesFiltros(horarios);
            filtrosInicializados=true;
        }
        aplicarFiltros(); actualizarDetalle();
    }

    private void cargarOpcionesFiltros(List<HorarioClase> horarios){
        actualizandoFiltros=true;
        try{
            ActualizadorModeloCombo.aplicar(filtroCiclo,
                    horarios.stream().map(HorarioClase::ciclo).toList(),"Todos",null);
            ActualizadorModeloCombo.aplicar(filtroCarrera,
                    horarios.stream().map(HorarioClase::carrera).toList(),"Todas",
                    ordenCatalogo(OpcionesAcademicas.CARRERAS));
            ActualizadorModeloCombo.aplicar(filtroCuatrimestre,
                    horarios.stream().map(h->String.valueOf(h.cuatrimestre())).toList(),"Todos",
                    Comparator.comparingInt(Integer::parseInt));
            ActualizadorModeloCombo.aplicar(filtroGrupo,
                    horarios.stream().map(HorarioClase::grupo).toList(),"Todos",
                    String.CASE_INSENSITIVE_ORDER);
            ActualizadorModeloCombo.aplicar(filtroTurno,
                    horarios.stream().map(HorarioClase::turno).toList(),"Todos",
                    ordenCatalogo(OpcionesAcademicas.TURNOS));
            ActualizadorModeloCombo.aplicar(filtroDia,
                    horarios.stream().map(HorarioClase::dia).toList(),"Todos",
                    ordenCatalogo(List.of("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado")));
            ActualizadorModeloCombo.aplicar(filtroLaboratorio,
                    horarios.stream().map(HorarioClase::laboratorio).toList(),"Todos",
                    String.CASE_INSENSITIVE_ORDER);
            ActualizadorModeloCombo.aplicar(filtroProfesor,
                    horarios.stream().map(HorarioClase::profesor).toList(),"Todos",
                    String.CASE_INSENSITIVE_ORDER);
        }finally{actualizandoFiltros=false;}
    }

    private Comparator<String> ordenCatalogo(List<String> orden){
        return Comparator.comparingInt((String valor)->{int posicion=orden.indexOf(valor);return posicion<0?Integer.MAX_VALUE:posicion;})
                .thenComparing(String.CASE_INSENSITIVE_ORDER);
    }

    private void aplicarFiltros(){
        if(ordenadorHorarios==null)return;
        final String busqueda=normalizar(campoBusqueda.getText().trim());
        final String ciclo=seleccion(filtroCiclo),carrera=seleccion(filtroCarrera);
        final String cuatrimestre=seleccion(filtroCuatrimestre),grupo=seleccion(filtroGrupo);
        final String turno=seleccion(filtroTurno),dia=seleccion(filtroDia),laboratorio=seleccion(filtroLaboratorio),profesor=seleccion(filtroProfesor);
        ordenadorHorarios.setRowFilter(new RowFilter<>(){
            @Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> e){
                return coincide(e,1,ciclo)&&coincide(e,2,carrera)&&coincide(e,3,cuatrimestre)
                        &&coincide(e,4,grupo)&&coincide(e,5,turno)&&coincide(e,8,dia)
                        &&coincide(e,10,laboratorio)&&coincide(e,7,profesor)&&contieneTexto(e,busqueda);
            }
        });
        etiquetaResultados.setText(tablaHorarios.getRowCount()+
                (tablaHorarios.getRowCount()==1?" asignación encontrada":" asignaciones encontradas"));
        actualizarDetalle();
    }

    private boolean coincide(RowFilter.Entry<? extends DefaultTableModel,? extends Integer> e,int columna,String valor){
        return valor==null||valor.equals(String.valueOf(e.getValue(columna)));
    }

    private boolean contieneTexto(RowFilter.Entry<? extends DefaultTableModel,? extends Integer> e,String texto){
        if(texto.isEmpty())return true;
        return normalizar(String.valueOf(e.getValue(6))).contains(texto)
                ||normalizar(String.valueOf(e.getValue(7))).contains(texto);
    }

    private String seleccion(JComboBox<String> combo){
        return combo.getSelectedIndex()<=0?null:(String)combo.getSelectedItem();
    }

    private String normalizar(String texto){
        return Normalizer.normalize(texto,Normalizer.Form.NFD).replaceAll("\\p{M}","")
                .toLowerCase(Locale.ROOT);
    }

    private void limpiarFiltros(){
        actualizandoFiltros=true;
        for(JComboBox<String> combo:List.of(filtroCiclo,filtroCarrera,filtroCuatrimestre,
                filtroGrupo,filtroTurno,filtroDia,filtroLaboratorio))combo.setSelectedIndex(0);
        filtroProfesor.setSelectedIndex(0);
        actualizandoFiltros=false; campoBusqueda.setText(""); aplicarFiltros();
    }

    private void cargarCiclos() {
        new javax.swing.SwingWorker<ResultadoCarga<DefaultTableModel>,Void>(){
            @Override protected ResultadoCarga<DefaultTableModel> doInBackground(){
                DefaultTableModel modelo=noEditable("ID","Ciclo","Inicio","Fin","Activo");
                try(Connection recibida=ConexionBaseDatos.conectar()){
                    Connection con=ControlDisponibilidadBaseDatos.requerir(recibida);
                    try(PreparedStatement ps=con.prepareStatement(
                            "SELECT id_ciclo,nombre,fecha_inicio,fecha_fin,activo FROM ciclos_escolares ORDER BY fecha_inicio DESC"); ResultSet rs=ps.executeQuery()){
                    while(rs.next())modelo.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getDate(3),rs.getDate(4),rs.getBoolean(5)?"Sí":"No"});
                    }
                    return ResultadoCarga.exito(modelo);
                }catch(Exception ex){return ResultadoCarga.fallo(ex);}
            }
            @Override protected void done(){
                try{
                    ResultadoCarga<DefaultTableModel> resultado=get();
                    if(resultado.error()!=null){manejarFalloConexion(resultado.error());return;}
                    registrarConexionDisponible();
                    ActualizadorModeloTabla.aplicar(tablaCiclos,resultado.datos(),0,
                            VentanaGestionHorarios.this::ajustarColumnasCiclos);
                }catch(InterruptedException ex){Thread.currentThread().interrupt();}
                catch(ExecutionException ex){manejarFalloConexion(ex.getCause());}
            }
        }.execute();
    }

    private static DefaultTableModel noEditable(String... columnas) {
        return new DefaultTableModel(columnas,0) { @Override public boolean isCellEditable(int r,int c){return false;} };
    }

    private void configurarTabla(JTable tabla){
        labsync.interfaz.comun.EstiloTablaLaboratorista.aplicar(tabla);
    }

    private JButton boton(String texto,Color fondo,Color frente){
        JButton boton=new JButton(texto); boton.setFont(new Font("Arial",Font.BOLD,14));
        boton.setBackground(fondo); boton.setForeground(frente); boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR)); boton.setPreferredSize(new Dimension(145,40));
        boton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(
                fondo.equals(Color.WHITE)?BORDE:fondo),BorderFactory.createEmptyBorder(6,12,6,12)));
        return boton;
    }

    private void ajustarColumnasHorarios(){
        int[] anchos={0,135,150,95,70,100,165,145,85,105,110,85};
        for(int i=1;i<anchos.length&&i<tablaHorarios.getColumnCount();i++){
            javax.swing.table.TableColumn columna=tablaHorarios.getColumnModel().getColumn(i);
            columna.setPreferredWidth(anchos[i]);
            columna.setMinWidth(i==6||i==7?110:Math.min(anchos[i],65));
        }
        ocultarColumnaId(tablaHorarios);
        tablaHorarios.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    private void ajustarColumnasCiclos(){
        int[] anchos={0,240,180,180,120};
        for(int i=1;i<anchos.length&&i<tablaCiclos.getColumnCount();i++){
            javax.swing.table.TableColumn columna=tablaCiclos.getColumnModel().getColumn(i);
            columna.setPreferredWidth(anchos[i]);
            columna.setMinWidth(Math.min(anchos[i],90));
        }
        ocultarColumnaId(tablaCiclos);
        tablaCiclos.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    private void ocultarColumnaId(JTable tabla){
        if(tabla.getColumnModel().getColumnCount()==0)return;
        javax.swing.table.TableColumn columna=tabla.getColumnModel().getColumn(0);
        columna.setMinWidth(0); columna.setMaxWidth(0);
        columna.setPreferredWidth(0); columna.setWidth(0);
    }

    private void crearPanelDetalle(){
        panelDetalle.setLayout(new BorderLayout()); panelDetalle.setBackground(Color.WHITE);
        panelDetalle.setMinimumSize(new Dimension(245,0)); panelDetalle.setPreferredSize(new Dimension(285,0));
        panelDetalle.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(16,16,16,16)));
        mostrarDetalleVacio();
    }

    private void mostrarDetalleVacio(){
        if(panelDetalle.getLayout()==null)return;
        panelDetalle.removeAll();
        JLabel titulo=new JLabel("Detalle de la asignación");
        titulo.setFont(new Font("Arial",Font.BOLD,15)); titulo.setForeground(VERDE_OSCURO);
        panelDetalle.add(titulo,BorderLayout.NORTH);
        JLabel ayuda=new JLabel("<html><div style='text-align:center'>Selecciona una fila para consultar<br>la información completa.</div></html>",SwingConstants.CENTER);
        ayuda.setFont(FUENTE); ayuda.setForeground(new Color(125,132,139));
        panelDetalle.add(ayuda,BorderLayout.CENTER); panelDetalle.revalidate(); panelDetalle.repaint();
    }

    private void actualizarDetalle(){
        int filaVista=tablaHorarios.getSelectedRow();
        if(filaVista<0){mostrarDetalleVacio();return;}
        int fila=tablaHorarios.convertRowIndexToModel(filaVista);
        DefaultTableModel modelo=(DefaultTableModel)tablaHorarios.getModel();
        String[] etiquetas={"Ciclo","Carrera","Cuatrimestre","Grupo","Turno",
                "Materia","Profesor","Día","Horario","Laboratorio","Estado"};
        panelDetalle.removeAll();
        JLabel titulo=new JLabel("Detalle de la asignación");
        titulo.setFont(new Font("Arial",Font.BOLD,15)); titulo.setForeground(VERDE_OSCURO);
        panelDetalle.add(titulo,BorderLayout.NORTH);
        JPanel datos=new JPanel(); datos.setOpaque(false); datos.setLayout(new BoxLayout(datos,BoxLayout.Y_AXIS));
        datos.setBorder(BorderFactory.createEmptyBorder(12,0,0,0));
        for(int i=0;i<etiquetas.length;i++)datos.add(filaDetalle(
                etiquetas[i],String.valueOf(modelo.getValueAt(fila,i+1))));
        panelDetalle.add(new JScrollPaneSinBorde(datos),BorderLayout.CENTER);
        panelDetalle.revalidate(); panelDetalle.repaint();
    }

    private JPanel filaDetalle(String etiqueta,String valor){
        JPanel fila=new JPanel(new BorderLayout(8,2)); fila.setOpaque(false);
        fila.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(235,237,239)));
        JLabel nombre=new JLabel(etiqueta+":"); nombre.setFont(new Font("Arial",Font.BOLD,11));
        nombre.setForeground(new Color(100,107,114));
        JLabel dato=new JLabel("<html>"+escaparHtml(valor)+"</html>"); dato.setFont(FUENTE); dato.setForeground(TEXTO);
        fila.add(nombre,BorderLayout.NORTH); fila.add(dato,BorderLayout.CENTER);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE,52));
        fila.setPreferredSize(new Dimension(220,45)); return fila;
    }

    private String escaparHtml(String texto){
        return texto.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static final class JScrollPaneSinBorde extends JScrollPane{
        JScrollPaneSinBorde(Component contenido){
            super(contenido); setBorder(null); setOpaque(false); getViewport().setOpaque(false);
            getVerticalScrollBar().setUnitIncrement(12); setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        }
    }

    private void editarSeleccionado() { Integer id=idSeleccionado(tablaHorarios); if(id!=null) editarHorario(id); }

    private void cambiarEstadoSeleccionado() {
        Integer id=idSeleccionado(tablaHorarios); if(id==null) return;
        int fila=tablaHorarios.convertRowIndexToModel(tablaHorarios.getSelectedRow());
        boolean activar="Inactivo".equals(modeloHorarios.getValueAt(fila,11));
        if(JOptionPane.showConfirmDialog(this,"¿"+(activar?"Activar":"Desactivar")+" la asignación seleccionada?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try(Connection recibida=ConexionBaseDatos.conectar()) {
            Connection con=ControlDisponibilidadBaseDatos.requerir(recibida);
            servicio.cambiarActivo(con,id,activar); cargarHorarios();
        }
        catch(Exception ex) { mostrarError(ex); }
    }

    private void editarHorario(Integer idHorario) {
        try(Connection recibida=ConexionBaseDatos.conectar()) {
            Connection con=ControlDisponibilidadBaseDatos.requerir(recibida);
            JComboBox<Item> ciclo=combo(con,"SELECT id_ciclo,nombre FROM ciclos_escolares WHERE activo=1 ORDER BY fecha_inicio DESC");
            JComboBox<String> carrera=new JComboBox<>(OpcionesAcademicas.CARRERAS.toArray(String[]::new));
            carrera.setEditable(true);
            JComboBox<Integer> cuatrimestre=new JComboBox<>();
            for(int numero=1;numero<=11;numero++) cuatrimestre.addItem(numero);
            JTextField grupo=new JTextField();
            JComboBox<String> turno=new JComboBox<>(OpcionesAcademicas.TURNOS.toArray(String[]::new));
            JTextField materia=new JTextField();
            JComboBox<Item> profesor=combo(con,"SELECT id,CONCAT_WS(' ',nombre,apellido_p,apellido_m) FROM usuario WHERE rol='Profesor' ORDER BY nombre");
            JComboBox<Item> laboratorio=combo(con,"SELECT id_laboratorio,nombre FROM laboratorios WHERE estado='Disponible' ORDER BY nombre");
            JComboBox<String> dia=new JComboBox<>(new String[]{"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"});
            JComboBox<LocalTime> inicio=new JComboBox<>();
            JComboBox<LocalTime> fin=new JComboBox<>();
            turno.addActionListener(e->actualizarHorasInicio(
                    (String)turno.getSelectedItem(),inicio,fin,null,null));
            inicio.addActionListener(e->actualizarHorasFin(
                    (String)turno.getSelectedItem(),inicio,fin,null));
            actualizarHorasInicio((String)turno.getSelectedItem(),inicio,fin,null,null);
            if(idHorario!=null) cargarHorarioEdicion(con,idHorario,ciclo,carrera,cuatrimestre,
                    grupo,turno,materia,profesor,laboratorio,dia,inicio,fin);
            JPanel formulario=formulario(new String[]{"Ciclo","Carrera","Cuatrimestre","Grupo","Turno","Materia","Profesor","Laboratorio","Día","Hora inicio","Hora fin"},
                    new java.awt.Component[]{ciclo,carrera,cuatrimestre,grupo,turno,materia,profesor,laboratorio,dia,inicio,fin});
            if(JOptionPane.showConfirmDialog(this,formulario,idHorario==null?"Nueva asignación":"Editar asignación",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
            Item ci=(Item)ciclo.getSelectedItem(), pr=(Item)profesor.getSelectedItem(), la=(Item)laboratorio.getSelectedItem();
            servicio.guardar(con,idHorario,ci.id(),textoSeleccionado(carrera),
                    (Integer)cuatrimestre.getSelectedItem(),grupo.getText(),
                    (String)turno.getSelectedItem(),materia.getText(),pr.id(),la.id(),dia.getSelectedItem().toString(),
                    (LocalTime)inicio.getSelectedItem(),(LocalTime)fin.getSelectedItem());
            cargarHorarios();
        } catch(Exception ex) { mostrarError(ex); }
    }

    private void cargarHorarioEdicion(Connection con,int id,JComboBox<Item> ciclo,
            JComboBox<String> carrera,JComboBox<Integer> cuatrimestre,JTextField grupo,
            JComboBox<String> turno,JTextField materia,JComboBox<Item> profesor,
            JComboBox<Item> laboratorio,JComboBox<String> dia,JComboBox<LocalTime> inicio,
            JComboBox<LocalTime> fin) throws Exception {
        String sql="SELECT id_ciclo,carrera,cuatrimestre,grupo,turno,materia,id_profesor,id_laboratorio,dia_semana,hora_inicio,hora_fin FROM horarios_clase WHERE id_horario=?";
        try(PreparedStatement ps=con.prepareStatement(sql)) { ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()) { if(rs.next()) {
            seleccionar(ciclo,rs.getInt(1)); seleccionarTexto(carrera,rs.getString(2));
            cuatrimestre.setSelectedItem(rs.getInt(3)); grupo.setText(rs.getString(4));
            turno.setSelectedItem(rs.getString(5)); materia.setText(rs.getString(6));
            seleccionar(profesor,rs.getInt(7)); seleccionar(laboratorio,rs.getInt(8));
            dia.setSelectedItem(rs.getString(9));
            actualizarHorasInicio(rs.getString(5),inicio,fin,
                    rs.getTime(10).toLocalTime(),rs.getTime(11).toLocalTime());
        } } }
    }

    private void actualizarHorasInicio(String turno,JComboBox<LocalTime> inicio,JComboBox<LocalTime> fin,
            LocalTime inicioSeleccionado,LocalTime finSeleccionado) {
        inicio.removeAllItems();
        if(turno==null) {
            fin.removeAllItems();
            return;
        }
        List<LocalTime> horas="Matutino".equals(turno)?INICIOS_MATUTINOS:INICIOS_VESPERTINOS;
        horas.forEach(inicio::addItem);
        if(inicioSeleccionado!=null) inicio.setSelectedItem(inicioSeleccionado);
        actualizarHorasFin(turno,inicio,fin,finSeleccionado);
    }

    private void actualizarHorasFin(String turno,JComboBox<LocalTime> inicio,JComboBox<LocalTime> fin,
            LocalTime seleccion) {
        LocalTime horaInicio=(LocalTime)inicio.getSelectedItem();
        fin.removeAllItems();
        if(turno==null||horaInicio==null) return;
        List<LocalTime> limites="Matutino".equals(turno)?FINES_MATUTINOS:FINES_VESPERTINOS;
        LocalTime finTurno="Matutino".equals(turno)?LocalTime.of(14,10):LocalTime.of(21,20);
        limites.stream()
                .filter(hora->hora.isAfter(horaInicio)&&!hora.isAfter(finTurno))
                .forEach(fin::addItem);
        if(seleccion!=null) fin.setSelectedItem(seleccion);
    }

    private void editarCiclo(Integer id) {
        JTextField nombre=new JTextField(),inicio=new JTextField("2026-09-01"),fin=new JTextField("2026-12-18");
        javax.swing.JCheckBox activo=new javax.swing.JCheckBox("Activo",true);
        try(Connection recibida=ConexionBaseDatos.conectar()) {
            Connection con=ControlDisponibilidadBaseDatos.requerir(recibida);
            if(id!=null) try(PreparedStatement ps=con.prepareStatement("SELECT nombre,fecha_inicio,fecha_fin,activo FROM ciclos_escolares WHERE id_ciclo=?")){ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(rs.next()){nombre.setText(rs.getString(1));inicio.setText(rs.getDate(2).toString());fin.setText(rs.getDate(3).toString());activo.setSelected(rs.getBoolean(4));}}}
            JPanel form=formulario(new String[]{"Nombre","Fecha inicio","Fecha fin","Estado"},new java.awt.Component[]{nombre,inicio,fin,activo});
            if(JOptionPane.showConfirmDialog(this,form,"Ciclo escolar",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            LocalDate desde=LocalDate.parse(inicio.getText().trim()),hasta=LocalDate.parse(fin.getText().trim()); if(hasta.isBefore(desde))throw new IllegalArgumentException("La fecha final es anterior a la inicial.");
            String sql=id==null?"INSERT INTO ciclos_escolares(nombre,fecha_inicio,fecha_fin,activo) VALUES(?,?,?,?)":"UPDATE ciclos_escolares SET nombre=?,fecha_inicio=?,fecha_fin=?,activo=? WHERE id_ciclo=?";
            try(PreparedStatement ps=con.prepareStatement(sql)){ps.setString(1,nombre.getText().trim());ps.setDate(2,java.sql.Date.valueOf(desde));ps.setDate(3,java.sql.Date.valueOf(hasta));ps.setBoolean(4,activo.isSelected());if(id!=null)ps.setInt(5,id);ps.executeUpdate();}
            cargarCiclos();
        } catch(Exception ex){mostrarError(ex);}
    }

    private void abrirVentana(JFrame ventana){
        labsync.interfaz.comun.NavegacionLaboratorista.abrir(this,ventana);
    }

    private void btnInicioActionPerformed(java.awt.event.ActionEvent evt){abrirVentana(new VentanaPanelLaboratorista(sesion));}
    private void btnBitacoraActionPerformed(java.awt.event.ActionEvent evt){abrirVentana(new VentanaBitacoraGeneral(sesion));}
    private void btnInventarioActionPerformed(java.awt.event.ActionEvent evt){abrirVentana(new VentanaGestionInventario(sesion));}
    private void btnMantenimientoActionPerformed(java.awt.event.ActionEvent evt){abrirVentana(new VentanaGestionMantenimiento(sesion));}
    private void btnReservasActionPerformed(java.awt.event.ActionEvent evt){abrirVentana(new VentanaGestionReservas(sesion));}
    private void btnFallasActionPerformed(java.awt.event.ActionEvent evt){abrirVentana(new VentanaGestionReportesFallas(sesion));}

    private void btnCerrarSesionActionPerformed(java.awt.event.ActionEvent evt){
        int respuesta=JOptionPane.showConfirmDialog(this,"¿Estás seguro que deseas cerrar sesión?",
                "Cerrar Sesión",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(respuesta==JOptionPane.YES_OPTION){
            VentanaInicioSesion login=new VentanaInicioSesion();
            login.setLocationRelativeTo(null); login.setVisible(true); dispose();
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents(){
        sidebarVerde=new javax.swing.JPanel();
        imgLabSync=new javax.swing.JLabel();
        btnInicio=new javax.swing.JButton();
        btnBitacora=new javax.swing.JButton();
        btnInventario=new javax.swing.JButton();
        btnMantenimiento=new javax.swing.JButton();
        btnReservas=new javax.swing.JButton();
        btnFallas=new javax.swing.JButton();
        headerBlanco=new javax.swing.JPanel();
        imgUTJ=new javax.swing.JLabel();
        lbNombreUsuario=new javax.swing.JLabel();
        btnCerrarSesion=new javax.swing.JButton();
        panelModulo=new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LabSync - Ciclos y horarios regulares");
        setMinimumSize(new java.awt.Dimension(1280,720));
        setResizable(false);

        sidebarVerde.setBackground(new java.awt.Color(8,173,141));
        sidebarVerde.setPreferredSize(new java.awt.Dimension(250,731));
        sidebarVerde.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        imgLabSync.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/labsync_blanco_200.png")));
        sidebarVerde.add(imgLabSync,new org.netbeans.lib.awtextra.AbsoluteConstraints(25,20,200,200));

        configurarBotonMenu(btnInicio,"Inicio"); btnInicio.addActionListener(this::btnInicioActionPerformed);
        sidebarVerde.add(btnInicio,new org.netbeans.lib.awtextra.AbsoluteConstraints(20,290,200,50));
        configurarBotonMenu(btnBitacora,"Bitácora"); btnBitacora.addActionListener(this::btnBitacoraActionPerformed);
        sidebarVerde.add(btnBitacora,new org.netbeans.lib.awtextra.AbsoluteConstraints(20,350,200,50));
        configurarBotonMenu(btnInventario,"Inventario"); btnInventario.addActionListener(this::btnInventarioActionPerformed);
        sidebarVerde.add(btnInventario,new org.netbeans.lib.awtextra.AbsoluteConstraints(20,410,200,50));
        configurarBotonMenu(btnMantenimiento,"Mantenimiento"); btnMantenimiento.addActionListener(this::btnMantenimientoActionPerformed);
        sidebarVerde.add(btnMantenimiento,new org.netbeans.lib.awtextra.AbsoluteConstraints(20,470,200,50));
        configurarBotonMenu(btnReservas,"Reservas"); btnReservas.addActionListener(this::btnReservasActionPerformed);
        sidebarVerde.add(btnReservas,new org.netbeans.lib.awtextra.AbsoluteConstraints(20,530,200,50));
        configurarBotonMenu(btnFallas,"Reporte de Fallas"); btnFallas.addActionListener(this::btnFallasActionPerformed);
        sidebarVerde.add(btnFallas,new org.netbeans.lib.awtextra.AbsoluteConstraints(20,590,200,50));

        headerBlanco.setBackground(java.awt.Color.WHITE);
        headerBlanco.setPreferredSize(new java.awt.Dimension(1030,100));
        headerBlanco.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        imgUTJ.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/UTJ_color.png")));
        headerBlanco.add(imgUTJ,new org.netbeans.lib.awtextra.AbsoluteConstraints(35,20,-1,-1));
        lbNombreUsuario.setFont(new java.awt.Font("Arial",java.awt.Font.BOLD,16));
        lbNombreUsuario.setForeground(new java.awt.Color(8,173,141)); lbNombreUsuario.setText("Hola, Usuario");
        headerBlanco.add(lbNombreUsuario,new org.netbeans.lib.awtextra.AbsoluteConstraints(740,42,145,25));
        btnCerrarSesion.setBackground(new java.awt.Color(220,53,69)); btnCerrarSesion.setForeground(java.awt.Color.WHITE);
        btnCerrarSesion.setFont(new java.awt.Font("Arial",java.awt.Font.BOLD,14)); btnCerrarSesion.setText("Cerrar Sesión");
        btnCerrarSesion.setBorderPainted(false); btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCerrarSesion.addActionListener(this::btnCerrarSesionActionPerformed);
        headerBlanco.add(btnCerrarSesion,new org.netbeans.lib.awtextra.AbsoluteConstraints(870,32,130,38));

        panelModulo.setBackground(new java.awt.Color(245,245,245));
        panelModulo.setPreferredSize(new java.awt.Dimension(1030,625));

        javax.swing.GroupLayout layout=new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(sidebarVerde,250,250,250)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(headerBlanco,1030,1030,1030)
                        .addComponent(panelModulo,1030,1030,1030)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(sidebarVerde,731,731,731)
                .addGroup(layout.createSequentialGroup().addComponent(headerBlanco,100,100,100)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelModulo,625,625,625)));
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void configurarBotonMenu(JButton boton,String texto){
        boton.setBackground(java.awt.Color.WHITE); boton.setForeground(new java.awt.Color(6,140,115));
        boton.setFont(new java.awt.Font("Arial",java.awt.Font.BOLD,14)); boton.setText(texto);
        boton.setBorderPainted(false); boton.setFocusPainted(false);
        boton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private JComboBox<Item> combo(Connection con,String sql) throws Exception { JComboBox<Item> c=new JComboBox<>();try(PreparedStatement ps=con.prepareStatement(sql);ResultSet rs=ps.executeQuery()){while(rs.next())c.addItem(new Item(rs.getInt(1),rs.getString(2)));}return c; }
    private JPanel formulario(String[] etiquetas,java.awt.Component[] componentes){JPanel p=new JPanel(new GridLayout(etiquetas.length,2,8,8));for(int i=0;i<etiquetas.length;i++){p.add(new JLabel(etiquetas[i]));p.add(componentes[i]);}return p;}
    private void seleccionar(JComboBox<Item> combo,int id){for(int i=0;i<combo.getItemCount();i++)if(combo.getItemAt(i).id()==id){combo.setSelectedIndex(i);break;}}
    private void seleccionarTexto(JComboBox<String> combo,String texto){for(int i=0;i<combo.getItemCount();i++)if(combo.getItemAt(i).equals(texto)){combo.setSelectedIndex(i);return;}combo.addItem(texto);combo.setSelectedItem(texto);}
    private String textoSeleccionado(JComboBox<String> combo){Object valor=combo.isEditable()?combo.getEditor().getItem():combo.getSelectedItem();return valor==null?null:valor.toString();}
    private Integer idSeleccionado(JTable tabla){int fila=tabla.getSelectedRow();if(fila<0){JOptionPane.showMessageDialog(this,"Selecciona un registro.");return null;}return ((Number)tabla.getValueAt(fila,0)).intValue();}
    private void manejarFalloConexion(Throwable error){
        LOGGER.log(Level.WARNING,"No fue posible consultar la base de datos",error);
        accionesDependientesDatos.forEach(accion->accion.setEnabled(false));
        if(controlConexion.registrarFallo()){
            JOptionPane.showMessageDialog(this,ControlDisponibilidadBaseDatos.MENSAJE_USUARIO,
                    "Base de datos no disponible",JOptionPane.WARNING_MESSAGE);
        }
    }

    private void registrarConexionDisponible(){
        controlConexion.registrarExito();
        accionesDependientesDatos.forEach(accion->accion.setEnabled(true));
    }

    private void mostrarError(Exception ex){
        Throwable causa=ex instanceof ExecutionException&&ex.getCause()!=null?ex.getCause():ex;
        if(ControlDisponibilidadBaseDatos.esFalloConexion(causa)){
            manejarFalloConexion(causa);
            return;
        }
        LOGGER.log(Level.WARNING,"No se pudo completar una operación de horarios",causa);
        String mensaje=causa instanceof IllegalArgumentException&&causa.getMessage()!=null
                ?causa.getMessage():"No fue posible completar la operación solicitada.";
        JOptionPane.showMessageDialog(this,mensaje,"No se pudo completar la operación",
                JOptionPane.ERROR_MESSAGE);
    }
    private record ResultadoCarga<T>(T datos,Throwable error){
        static <T> ResultadoCarga<T> exito(T datos){return new ResultadoCarga<>(datos,null);}
        static <T> ResultadoCarga<T> fallo(Throwable error){return new ResultadoCarga<>(null,error);}
    }
    private record Item(int id,String texto){@Override public String toString(){return texto;}}

    static final class RenderHorario extends DefaultTableCellRenderer{
        private static final int COLUMNA_ESTADO_MODELO=11;
        @Override public Component getTableCellRendererComponent(JTable tabla,Object valor,boolean seleccionado,boolean foco,int fila,int columna){
            super.getTableCellRendererComponent(tabla,valor,seleccionado,foco,fila,columna);
            int filaModelo=tabla.convertRowIndexToModel(fila);
            Object valorEstado=tabla.getModel().getValueAt(filaModelo,COLUMNA_ESTADO_MODELO);
            if(!seleccionado){setBackground(fila%2==0?Color.WHITE:new Color(248,250,251));String estado=String.valueOf(valorEstado);setForeground("Inactivo".equals(estado)?new Color(145,145,145):TEXTO);}
            setToolTipText(valor==null?null:valor.toString());return this;
        }
    }

    static final class RenderCiclo extends DefaultTableCellRenderer{
        private static final int COLUMNA_ACTIVO_MODELO=4;
        @Override public Component getTableCellRendererComponent(JTable tabla,Object valor,boolean seleccionado,boolean foco,int fila,int columna){
            super.getTableCellRendererComponent(tabla,valor,seleccionado,foco,fila,columna);
            int filaModelo=tabla.convertRowIndexToModel(fila);
            Object activo=tabla.getModel().getValueAt(filaModelo,COLUMNA_ACTIVO_MODELO);
            if(!seleccionado){setBackground(fila%2==0?Color.WHITE:new Color(248,250,251));setForeground(esActivo(activo)?TEXTO:new Color(145,145,145));}
            setToolTipText(valor==null?null:valor.toString());return this;
        }
        private static boolean esActivo(Object valor){
            return Boolean.TRUE.equals(valor)||"1".equals(String.valueOf(valor))
                    ||"Activo".equalsIgnoreCase(String.valueOf(valor));
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBitacora;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnFallas;
    private javax.swing.JButton btnInicio;
    private javax.swing.JButton btnInventario;
    private javax.swing.JButton btnMantenimiento;
    private javax.swing.JButton btnReservas;
    private javax.swing.JPanel headerBlanco;
    private javax.swing.JLabel imgLabSync;
    private javax.swing.JLabel imgUTJ;
    private javax.swing.JLabel lbNombreUsuario;
    private javax.swing.JPanel panelModulo;
    private javax.swing.JPanel sidebarVerde;
    // End of variables declaration//GEN-END:variables
}
