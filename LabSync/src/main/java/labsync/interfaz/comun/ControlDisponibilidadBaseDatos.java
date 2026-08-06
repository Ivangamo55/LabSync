package labsync.interfaz.comun;

import java.sql.Connection;
import java.sql.SQLException;

/** Mantiene estable la interfaz durante una caída temporal de la base de datos. */
public final class ControlDisponibilidadBaseDatos {
    public static final String MENSAJE_USUARIO = "No fue posible conectarse con la base de datos.\n"
            + "Comprueba que MySQL/MariaDB esté iniciado e inténtalo nuevamente.";

    private boolean falloActivo;

    public static Connection requerir(Connection conexion) throws SQLException {
        if (conexion == null) {
            throw new SQLException("ConexionBaseDatos devolvió null", "08001");
        }
        return conexion;
    }

    /** Devuelve true únicamente para el primer fallo de una misma caída. */
    public boolean registrarFallo() {
        if (falloActivo) return false;
        falloActivo = true;
        return true;
    }

    /** Devuelve true si esta llamada representa una recuperación. */
    public boolean registrarExito() {
        boolean recuperado = falloActivo;
        falloActivo = false;
        return recuperado;
    }

    public boolean hayFalloActivo() {
        return falloActivo;
    }

    public static boolean esFalloConexion(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof SQLException sqlException) {
                String estado = sqlException.getSQLState();
                if (estado == null || estado.startsWith("08")) return true;
            }
        }
        return false;
    }
}
