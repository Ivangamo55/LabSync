package labsync.labsync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {
    private static final String URL = "jdbc:mysql://localhost:3306/labsync_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    public static Connection conectar() {
        Connection conexion = null;
        
        try {
            conexion = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("OK");
        } catch (SQLException e) {
            System.out.println("Error fatal de conexion: " + e.getMessage());
        }
        return conexion;
    }
    
    public static void main(String[] args) {
        conectar();
    }
}
