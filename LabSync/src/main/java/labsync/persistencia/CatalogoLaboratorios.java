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

    private static void cargar(JComboBox<String> combo, String primeraOpcion, boolean soloDisponibles) {
        combo.removeAllItems();
        combo.addItem(primeraOpcion);

        String sql = "SELECT nombre FROM laboratorios"
                + (soloDisponibles ? " WHERE estado = 'Disponible'" : "")
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
