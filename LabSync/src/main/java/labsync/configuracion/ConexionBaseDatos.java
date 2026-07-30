package labsync.configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Crea las conexiones JDBC utilizadas por los módulos de LabSync. */
public class ConexionBaseDatos {
    private static final String URL = configurar(
            "LABSYNC_DB_URL", "labsync.db.url", "jdbc:mysql://localhost:3306/labsync_db");
    private static final String USER = configurar("LABSYNC_DB_USER", "labsync.db.user", "root");
    private static final String PASSWORD = configurar(
            "LABSYNC_DB_PASSWORD", "labsync.db.password", "");
    
    public static Connection conectar() {
        Connection conexion = null;
        
        try {
            conexion = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(ConexionBaseDatos.class.getName()).log(
                    java.util.logging.Level.SEVERE, "Error fatal de conexion", e);
        }
        return conexion;
    }

    private static String configurar(String variableEntorno, String propiedad, String predeterminado) {
        String valor = System.getenv(variableEntorno);
        if (valor == null || valor.isBlank()) {
            valor = System.getProperty(propiedad);
        }
        return valor == null || valor.isBlank() ? predeterminado : valor;
    }
    
    public static void main(String[] args) {
        conectar();
    }
}
