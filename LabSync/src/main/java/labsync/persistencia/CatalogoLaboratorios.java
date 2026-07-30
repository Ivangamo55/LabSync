package labsync.persistencia;

import labsync.configuracion.ConexionBaseDatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;

/** Utilidades para poblar controles con los laboratorios registrados. */
public final class CatalogoLaboratorios {

    private static final Logger LOGGER = Logger.getLogger(CatalogoLaboratorios.class.getName());

    private CatalogoLaboratorios() {
    }

    public static void cargarDisponibles(JComboBox<String> combo, String primeraOpcion) {
        cargar(combo, primeraOpcion, true);
    }

    public static void cargarTodos(JComboBox<String> combo, String primeraOpcion) {
        cargar(combo, primeraOpcion, false);
    }

    /**
     * Resuelve y bloquea un laboratorio disponible antes de insertar una reserva.
     * La inserción posterior debe usar VALUES para que el trigger pueda consultar
     * laboratorios sin que esa tabla forme parte de la sentencia que lo invoca.
     */
    public static int buscarIdDisponible(Connection conexion, String nombre) throws SQLException {
        String sql = "SELECT id_laboratorio FROM laboratorios "
                + "WHERE nombre=? AND estado='Disponible' FOR UPDATE";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_laboratorio");
                }
            }
        }
        throw new SQLException("El laboratorio seleccionado no existe o no está disponible.");
    }

    private static void cargar(JComboBox<String> combo, String primeraOpcion, boolean soloDisponibles) {
        combo.removeAllItems();
        combo.addItem(primeraOpcion);

        String sql = "SELECT nombre FROM laboratorios WHERE nombre NOT IN ('PB-05', 'M-19')"
                + (soloDisponibles ? " AND estado = 'Disponible'" : "")
                + " ORDER BY nombre";

        try (Connection con = ConexionBaseDatos.conectar()) {
            if (con == null) {
                return;
            }
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    combo.addItem(rs.getString("nombre"));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "No se cargaron los laboratorios", ex);
        }
    }
}
