package labsync.interfaz.comun;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class ControlDisponibilidadBaseDatosTest {
    @Test
    void conexionNullProduceFalloControladoYNoNullPointerException() {
        SQLException error = assertThrows(SQLException.class,
                () -> ControlDisponibilidadBaseDatos.requerir(null));
        assertTrue(ControlDisponibilidadBaseDatos.esFalloConexion(error));
        assertDoesNotThrow(() -> ControlDisponibilidadBaseDatos.esFalloConexion(error));
    }

    @Test
    void reconoceExcepcionSqlDeConexion() {
        assertTrue(ControlDisponibilidadBaseDatos.esFalloConexion(
                new SQLException("Servidor no disponible", "08006")));
        assertFalse(ControlDisponibilidadBaseDatos.esFalloConexion(
                new SQLException("Dato duplicado", "23000")));
    }

    @Test
    void evitaAvisosRepetidosYPermiteAvisarTrasRecuperacion() {
        ControlDisponibilidadBaseDatos control = new ControlDisponibilidadBaseDatos();
        assertTrue(control.registrarFallo());
        assertFalse(control.registrarFallo());
        assertTrue(control.hayFalloActivo());
        assertTrue(control.registrarExito());
        assertFalse(control.hayFalloActivo());
        assertTrue(control.registrarFallo());
    }
}
