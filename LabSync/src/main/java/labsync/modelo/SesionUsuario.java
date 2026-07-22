package labsync.modelo;

import labsync.configuracion.ConexionBaseDatos;
import labsync.interfaz.autenticacion.VentanaInicioSesion;

/**
 * Identidad inmutable del usuario autenticado.
 *
 * Mantener el id de la base de datos durante toda la navegación evita asociar
 * reservas y reportes únicamente por el nombre, que no es un dato único.
 */
public final class SesionUsuario {

    private final int id;
    private final String nombre;
    private final String nombreCompleto;
    private final String rol;

    public SesionUsuario(int id, String nombre, String nombreCompleto, String rol) {
        this.id = id;
        this.nombre = normalizar(nombre, "Usuario");
        this.nombreCompleto = normalizar(nombreCompleto, this.nombre);
        this.rol = normalizar(rol, "");
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    public boolean estaIdentificada() {
        return id > 0;
    }

    /**
     * Compatibilidad con constructores usados por el diseñador y los main.
     * El acceso normal crea la sesión desde VentanaInicioSesion con el id ya autenticado.
     */
    public static SesionUsuario buscarProfesor(String nombre) {
        return buscarPorNombreYRol(nombre, "Profesor");
    }

    public static SesionUsuario buscarEstudiante(String nombre) {
        return buscarPorNombreYRol(nombre, "Estudiante");
    }

    private static SesionUsuario buscarPorNombreYRol(String nombre, String rolBuscado) {
        String nombreSeguro = normalizar(nombre, "Profesor");
        String sql = "SELECT id, nombre, CONCAT_WS(' ', nombre, apellido_p, apellido_m) nombre_completo, rol "
                + "FROM usuario WHERE nombre = ? AND rol = ? ORDER BY id LIMIT 1";
        try (java.sql.Connection con = ConexionBaseDatos.conectar()) {
            if (con != null) {
                try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, nombreSeguro);
                    ps.setString(2, rolBuscado);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new SesionUsuario(rs.getInt("id"), rs.getString("nombre"),
                                    rs.getString("nombre_completo"), rs.getString("rol"));
                        }
                    }
                }
            }
        } catch (java.sql.SQLException ex) {
            java.util.logging.Logger.getLogger(SesionUsuario.class.getName()).log(
                    java.util.logging.Level.WARNING, "No se pudo reconstruir la sesión del profesor", ex);
        }
        return new SesionUsuario(0, nombreSeguro, nombreSeguro, rolBuscado);
    }

    private static String normalizar(String valor, String respaldo) {
        return valor == null || valor.isBlank() ? respaldo : valor.trim();
    }
}
