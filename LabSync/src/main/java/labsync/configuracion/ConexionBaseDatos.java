package labsync.configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Crea las conexiones JDBC utilizadas por los módulos de LabSync. */
public class ConexionBaseDatos {
    private static final String URL = "jdbc:mysql://localhost:3306/labsync_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
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
    
    public static void main(String[] args) {
        conectar();
    }
}
