package labsync.persistencia;

import labsync.configuracion.ConexionBaseDatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;

/** Crea modelos desconectados; el modelo sólo debe asociarse a JTable desde el EDT. */
public final class ConsultaTabla {
    @FunctionalInterface
    public interface Parametros {
        void asignar(PreparedStatement sentencia) throws SQLException;
    }

    private ConsultaTabla() { }

    public static DefaultTableModel ejecutar(String sql, String[] columnas, String[] campos) {
        return ejecutar(sql, columnas, campos, sentencia -> { });
    }

    public static DefaultTableModel ejecutar(String sql, String[] columnas, String[] campos,
            Parametros parametros) {
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        try (Connection conexion = ConexionBaseDatos.conectar()) {
            if (conexion == null) return modelo;
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                parametros.asignar(sentencia);
                try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    Object[] fila = new Object[campos.length];
                    for (int i = 0; i < campos.length; i++) fila[i] = resultado.getObject(campos[i]);
                    modelo.addRow(fila);
                }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Error al consultar datos para actualización", ex);
        }
        return modelo;
    }
}
