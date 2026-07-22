package labsync.persistencia;

import labsync.configuracion.ConexionBaseDatos;
import labsync.persistencia.ConsultaTabla;
import labsync.interfaz.comun.Recursos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Lectura breve de un único valor, siempre con recursos autocerrables. */
public final class ConsultaEscalar {
    private ConsultaEscalar() { }

    public static Object ejecutar(String sql, String campo, ConsultaTabla.Parametros parametros) {
        try (Connection conexion = ConexionBaseDatos.conectar()) {
            if (conexion == null) return null;
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                parametros.asignar(sentencia);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    return resultado.next() ? resultado.getObject(campo) : null;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Error al consultar el resumen", ex);
        }
    }
}
