package labsync.interfaz.horarios;

import labsync.configuracion.ConexionBaseDatos;
import labsync.modelo.HorarioClase;
import labsync.servicio.ServicioHorarios;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Administración Swing de ciclos y asignaciones regulares de laboratorios. */
public final class VentanaGestionHorarios extends JFrame {
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
    private final JTable tablaHorarios = new JTable();
    private final JTable tablaCiclos = new JTable();

    public VentanaGestionHorarios() {
        setTitle("LabSync - Ciclos y horarios regulares");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1180, 680);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8,8));
        add(crearEncabezado(),BorderLayout.NORTH);
        JTabbedPane pestanas=new JTabbedPane();
        pestanas.addTab("Horarios de laboratorio",crearPanelHorarios());
        pestanas.addTab("Ciclos escolares",crearPanelCiclos());
        add(pestanas,BorderLayout.CENTER);
        cargarHorarios(); cargarCiclos();
    }

    private JPanel crearEncabezado() {
        JPanel panel=new JPanel(new BorderLayout()); panel.setBackground(new Color(8,173,141));
        JLabel titulo=new JLabel("  Administración de horarios UTJ-CCD");
        titulo.setForeground(Color.WHITE); titulo.setFont(titulo.getFont().deriveFont(20f));
        panel.add(titulo,BorderLayout.CENTER); return panel;
    }

    private JPanel crearPanelHorarios() {
        tablaHorarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel botones=new JPanel();
        JButton nuevo=new JButton("Nueva asignación"); nuevo.addActionListener(e->editarHorario(null));
        JButton editar=new JButton("Editar"); editar.addActionListener(e->editarSeleccionado());
        JButton desactivar=new JButton("Desactivar"); desactivar.addActionListener(e->desactivarSeleccionado());
        JButton actualizar=new JButton("Actualizar"); actualizar.addActionListener(e->cargarHorarios());
        botones.add(nuevo); botones.add(editar); botones.add(desactivar); botones.add(actualizar);
        JPanel panel=new JPanel(new BorderLayout()); panel.add(botones,BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaHorarios),BorderLayout.CENTER); return panel;
    }

    private JPanel crearPanelCiclos() {
        JPanel botones=new JPanel();
        JButton nuevo=new JButton("Nuevo ciclo"); nuevo.addActionListener(e->editarCiclo(null));
        JButton editar=new JButton("Editar ciclo"); editar.addActionListener(e->{Integer id=idSeleccionado(tablaCiclos); if(id!=null) editarCiclo(id);});
        JButton actualizar=new JButton("Actualizar"); actualizar.addActionListener(e->cargarCiclos());
        botones.add(nuevo); botones.add(editar); botones.add(actualizar);
        JPanel panel=new JPanel(new BorderLayout()); panel.add(botones,BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaCiclos),BorderLayout.CENTER); return panel;
    }

    private void cargarHorarios() {
        DefaultTableModel modelo=noEditable("ID","Ciclo","Trayectoria","Grupo","Turno","Materia","Profesor","Día","Horario","Laboratorio");
        try(Connection con=ConexionBaseDatos.conectar()) {
            if(con==null) throw new java.sql.SQLException("No hay conexión.");
            for(HorarioClase h:servicio.consultarTodos(con)) modelo.addRow(new Object[]{h.id(),h.ciclo(),h.trayectoria(),h.grupo(),h.turno(),h.materia(),h.profesor(),h.dia(),h.intervalo(),h.laboratorio()});
            tablaHorarios.setModel(modelo);
        } catch(Exception ex) { mostrarError(ex); }
    }

    private void cargarCiclos() {
        DefaultTableModel modelo=noEditable("ID","Ciclo","Inicio","Fin","Activo");
        try(Connection con=ConexionBaseDatos.conectar(); PreparedStatement ps=con.prepareStatement(
                "SELECT id_ciclo,nombre,fecha_inicio,fecha_fin,activo FROM ciclos_escolares ORDER BY fecha_inicio DESC"); ResultSet rs=ps.executeQuery()) {
            while(rs.next()) modelo.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getDate(3),rs.getDate(4),rs.getBoolean(5)?"Sí":"No"});
            tablaCiclos.setModel(modelo);
        } catch(Exception ex) { mostrarError(ex); }
    }

    private DefaultTableModel noEditable(String... columnas) {
        return new DefaultTableModel(columnas,0) { @Override public boolean isCellEditable(int r,int c){return false;} };
    }

    private void editarSeleccionado() { Integer id=idSeleccionado(tablaHorarios); if(id!=null) editarHorario(id); }

    private void desactivarSeleccionado() {
        Integer id=idSeleccionado(tablaHorarios); if(id==null) return;
        if(JOptionPane.showConfirmDialog(this,"¿Desactivar la asignación seleccionada?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try(Connection con=ConexionBaseDatos.conectar()) { servicio.desactivar(con,id); cargarHorarios(); }
        catch(Exception ex) { mostrarError(ex); }
    }

    private void editarHorario(Integer idHorario) {
        try(Connection con=ConexionBaseDatos.conectar()) {
            JComboBox<Item> ciclo=combo(con,"SELECT id_ciclo,nombre FROM ciclos_escolares WHERE activo=1 ORDER BY fecha_inicio DESC");
            JComboBox<Item> grupo=comboGrupos(con);
            JComboBox<Item> materia=new JComboBox<>();
            JComboBox<Item> profesor=combo(con,"SELECT id,CONCAT_WS(' ',nombre,apellido_p,apellido_m) FROM usuario WHERE rol='Profesor' ORDER BY nombre");
            JComboBox<Item> laboratorio=combo(con,"SELECT id_laboratorio,nombre FROM laboratorios WHERE estado='Disponible' ORDER BY nombre");
            JComboBox<String> dia=new JComboBox<>(new String[]{"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"});
            JComboBox<LocalTime> inicio=new JComboBox<>();
            JComboBox<LocalTime> fin=new JComboBox<>();
            grupo.addActionListener(e->{
                Item grupoSeleccionado=(Item)grupo.getSelectedItem();
                cargarMaterias(con,materia,grupoSeleccionado);
                actualizarHorasInicio(grupoSeleccionado,inicio,fin,null,null);
            });
            inicio.addActionListener(e->actualizarHorasFin((Item)grupo.getSelectedItem(),inicio,fin,null));
            cargarMaterias(con,materia,(Item)grupo.getSelectedItem());
            actualizarHorasInicio((Item)grupo.getSelectedItem(),inicio,fin,null,null);
            if(idHorario!=null) cargarHorarioEdicion(con,idHorario,ciclo,grupo,materia,profesor,laboratorio,dia,inicio,fin);
            JPanel formulario=formulario(new String[]{"Ciclo","Grupo","Materia","Profesor","Laboratorio","Día","Hora inicio","Hora fin"},
                    new java.awt.Component[]{ciclo,grupo,materia,profesor,laboratorio,dia,inicio,fin});
            if(JOptionPane.showConfirmDialog(this,formulario,idHorario==null?"Nueva asignación":"Editar asignación",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
            Item ci=(Item)ciclo.getSelectedItem(), gr=(Item)grupo.getSelectedItem(), ma=(Item)materia.getSelectedItem(), pr=(Item)profesor.getSelectedItem(), la=(Item)laboratorio.getSelectedItem();
            servicio.guardar(con,idHorario,ci.id(),gr.id(),ma.id(),pr.id(),la.id(),dia.getSelectedItem().toString(),
                    (LocalTime)inicio.getSelectedItem(),(LocalTime)fin.getSelectedItem());
            cargarHorarios();
        } catch(Exception ex) { mostrarError(ex); }
    }

    private void cargarHorarioEdicion(Connection con,int id,JComboBox<Item> ciclo,JComboBox<Item> grupo,JComboBox<Item> materia,
            JComboBox<Item> profesor,JComboBox<Item> laboratorio,JComboBox<String> dia,JComboBox<LocalTime> inicio,JComboBox<LocalTime> fin) throws Exception {
        String sql="SELECT id_ciclo,id_grupo,id_plan_materia,id_profesor,id_laboratorio,dia_semana,hora_inicio,hora_fin FROM horarios_clase WHERE id_horario=?";
        try(PreparedStatement ps=con.prepareStatement(sql)) { ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()) { if(rs.next()) {
            seleccionar(ciclo,rs.getInt(1)); seleccionar(grupo,rs.getInt(2)); cargarMaterias(con,materia,(Item)grupo.getSelectedItem());
            seleccionar(materia,rs.getInt(3)); seleccionar(profesor,rs.getInt(4)); seleccionar(laboratorio,rs.getInt(5));
            dia.setSelectedItem(rs.getString(6));
            actualizarHorasInicio((Item)grupo.getSelectedItem(),inicio,fin,
                    rs.getTime(7).toLocalTime(),rs.getTime(8).toLocalTime());
        } } }
    }

    private void cargarMaterias(Connection con,JComboBox<Item> destino,Item grupo) {
        destino.removeAllItems(); if(grupo==null) return;
        try(PreparedStatement ps=con.prepareStatement("SELECT pm.id_plan_materia,m.nombre FROM grupos g JOIN plan_materias pm ON pm.id_trayectoria=g.id_trayectoria AND pm.cuatrimestre=g.cuatrimestre JOIN materias m ON m.id_materia=pm.id_materia WHERE g.id_grupo=? ORDER BY pm.orden")) {
            ps.setInt(1,grupo.id()); try(ResultSet rs=ps.executeQuery()){while(rs.next()) destino.addItem(new Item(rs.getInt(1),rs.getString(2),null));}
        } catch(Exception ex){mostrarError(ex);}
    }

    private JComboBox<Item> comboGrupos(Connection con) throws Exception {
        JComboBox<Item> grupos=new JComboBox<>();
        String sql="SELECT g.id_grupo,CONCAT(t.codigo,' ',g.cuatrimestre,'°',g.letra,' ',g.turno),g.turno "
                + "FROM grupos g JOIN trayectorias t ON t.id_trayectoria=g.id_trayectoria "
                + "WHERE g.activo=1 ORDER BY t.codigo,g.cuatrimestre";
        try(PreparedStatement ps=con.prepareStatement(sql);ResultSet rs=ps.executeQuery()) {
            while(rs.next()) grupos.addItem(new Item(rs.getInt(1),rs.getString(2),rs.getString(3)));
        }
        return grupos;
    }

    private void actualizarHorasInicio(Item grupo,JComboBox<LocalTime> inicio,JComboBox<LocalTime> fin,
            LocalTime inicioSeleccionado,LocalTime finSeleccionado) {
        inicio.removeAllItems();
        if(grupo==null) {
            fin.removeAllItems();
            return;
        }
        List<LocalTime> horas="Matutino".equals(grupo.turno())?INICIOS_MATUTINOS:INICIOS_VESPERTINOS;
        horas.forEach(inicio::addItem);
        if(inicioSeleccionado!=null) inicio.setSelectedItem(inicioSeleccionado);
        actualizarHorasFin(grupo,inicio,fin,finSeleccionado);
    }

    private void actualizarHorasFin(Item grupo,JComboBox<LocalTime> inicio,JComboBox<LocalTime> fin,
            LocalTime seleccion) {
        LocalTime horaInicio=(LocalTime)inicio.getSelectedItem();
        fin.removeAllItems();
        if(grupo==null||horaInicio==null) return;
        List<LocalTime> limites="Matutino".equals(grupo.turno())?FINES_MATUTINOS:FINES_VESPERTINOS;
        LocalTime finTurno="Matutino".equals(grupo.turno())?LocalTime.of(14,10):LocalTime.of(21,20);
        limites.stream()
                .filter(hora->hora.isAfter(horaInicio)&&!hora.isAfter(finTurno))
                .forEach(fin::addItem);
        if(seleccion!=null) fin.setSelectedItem(seleccion);
    }

    private void editarCiclo(Integer id) {
        JTextField nombre=new JTextField(),inicio=new JTextField("2026-09-01"),fin=new JTextField("2026-12-18");
        javax.swing.JCheckBox activo=new javax.swing.JCheckBox("Activo",true);
        try(Connection con=ConexionBaseDatos.conectar()) {
            if(id!=null) try(PreparedStatement ps=con.prepareStatement("SELECT nombre,fecha_inicio,fecha_fin,activo FROM ciclos_escolares WHERE id_ciclo=?")){ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(rs.next()){nombre.setText(rs.getString(1));inicio.setText(rs.getDate(2).toString());fin.setText(rs.getDate(3).toString());activo.setSelected(rs.getBoolean(4));}}}
            JPanel form=formulario(new String[]{"Nombre","Fecha inicio","Fecha fin","Estado"},new java.awt.Component[]{nombre,inicio,fin,activo});
            if(JOptionPane.showConfirmDialog(this,form,"Ciclo escolar",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            LocalDate desde=LocalDate.parse(inicio.getText().trim()),hasta=LocalDate.parse(fin.getText().trim()); if(hasta.isBefore(desde))throw new IllegalArgumentException("La fecha final es anterior a la inicial.");
            String sql=id==null?"INSERT INTO ciclos_escolares(nombre,fecha_inicio,fecha_fin,activo) VALUES(?,?,?,?)":"UPDATE ciclos_escolares SET nombre=?,fecha_inicio=?,fecha_fin=?,activo=? WHERE id_ciclo=?";
            try(PreparedStatement ps=con.prepareStatement(sql)){ps.setString(1,nombre.getText().trim());ps.setDate(2,java.sql.Date.valueOf(desde));ps.setDate(3,java.sql.Date.valueOf(hasta));ps.setBoolean(4,activo.isSelected());if(id!=null)ps.setInt(5,id);ps.executeUpdate();}
            cargarCiclos();
        } catch(Exception ex){mostrarError(ex);}
    }

    private JComboBox<Item> combo(Connection con,String sql) throws Exception { JComboBox<Item> c=new JComboBox<>();try(PreparedStatement ps=con.prepareStatement(sql);ResultSet rs=ps.executeQuery()){while(rs.next())c.addItem(new Item(rs.getInt(1),rs.getString(2),null));}return c; }
    private JPanel formulario(String[] etiquetas,java.awt.Component[] componentes){JPanel p=new JPanel(new GridLayout(etiquetas.length,2,8,8));for(int i=0;i<etiquetas.length;i++){p.add(new JLabel(etiquetas[i]));p.add(componentes[i]);}return p;}
    private void seleccionar(JComboBox<Item> combo,int id){for(int i=0;i<combo.getItemCount();i++)if(combo.getItemAt(i).id()==id){combo.setSelectedIndex(i);break;}}
    private Integer idSeleccionado(JTable tabla){int fila=tabla.getSelectedRow();if(fila<0){JOptionPane.showMessageDialog(this,"Selecciona un registro.");return null;}return ((Number)tabla.getValueAt(fila,0)).intValue();}
    private void mostrarError(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"No se pudo completar la operación",JOptionPane.ERROR_MESSAGE);}
    private record Item(int id,String texto,String turno){@Override public String toString(){return texto;}}
}
