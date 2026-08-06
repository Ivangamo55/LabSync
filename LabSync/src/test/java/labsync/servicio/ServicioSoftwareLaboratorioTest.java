package labsync.servicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServicioSoftwareLaboratorioTest {
    private final ServicioSoftwareLaboratorio servicio = new ServicioSoftwareLaboratorio();

    @Test void registrarSoftwareValido() throws Exception {
        Connection con=mock(Connection.class); PreparedStatement ps=mock(PreparedStatement.class);
        ResultSet claves=mock(ResultSet.class);
        when(con.prepareStatement(anyString(),eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);when(ps.getGeneratedKeys()).thenReturn(claves);
        when(claves.next()).thenReturn(true);when(claves.getInt(1)).thenReturn(14);
        assertEquals(14,servicio.guardar(con,null,2,"IntelliJ IDEA","2026.1",null,
                "TSU - DSM","Actualizado",null,null));
    }

    @Test void rechazaDuplicadoPorLaboratorioYNombre() throws Exception {
        Connection con=mock(Connection.class); PreparedStatement ps=mock(PreparedStatement.class);
        when(con.prepareStatement(anyString(),anyInt())).thenReturn(ps);
        when(ps.executeUpdate()).thenThrow(new SQLException("Duplicate", "23000", 1062));
        assertThrows(IllegalArgumentException.class,()->servicio.guardar(con,null,2,
                "NetBeans",null,null,"General","Actualizado",null,null));
    }

    @Test void desactualizadoSinObjetivoSeRechaza() {
        assertThrows(IllegalArgumentException.class,()->servicio.validar(1,"Java","17",null,
                "General","Desactualizado",null));
    }

    @Test void eliminacionPendienteSinObservacionesSeRechaza() {
        assertThrows(IllegalArgumentException.class,()->servicio.validar(1,"Java","17",null,
                "General","Pendiente de eliminación"," "));
    }

    @Test void instalacionPendienteSinVersionInstaladaSePermite() {
        assertDoesNotThrow(()->servicio.validar(1,"MariaDB",null,"11.8",
                "General","Pendiente de instalación",null));
    }

    @Test void editarConservaElIdentificador() throws Exception {
        Connection con=mock(Connection.class);PreparedStatement ps=mock(PreparedStatement.class);
        when(con.prepareStatement(anyString(),anyInt())).thenReturn(ps);when(ps.executeUpdate()).thenReturn(1);
        assertEquals(9,servicio.guardar(con,9,2,"JDK","17","21","TSU - DSM",
                "Desactualizado",null,null));
        verify(ps).setInt(9,9);
    }

    @Test void cambiarEstadoEliminadoActualizaSinBorrar() throws Exception {
        Connection con=mock(Connection.class);PreparedStatement ps=mock(PreparedStatement.class);
        when(con.prepareStatement(anyString())).thenReturn(ps);when(ps.executeUpdate()).thenReturn(1);
        servicio.cambiarEstado(con,7,"Eliminado",null,null);
        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);verify(con).prepareStatement(sql.capture());
        assertTrue(sql.getValue().startsWith("UPDATE"));
        verify(ps).setString(1,"Eliminado");verify(ps).setInt(2,7);
    }

    @Test void filtrosCombinadosUsanParametrosYNoConcatenanValores() throws Exception {
        Connection con=mock(Connection.class);PreparedStatement ps=mock(PreparedStatement.class);
        ResultSet rs=mock(ResultSet.class);when(con.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);when(rs.next()).thenReturn(false);
        servicio.consultar(con,3,"Desactualizado","TSU - DSM","Java");
        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);verify(con).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("s.id_laboratorio=?"));
        assertTrue(sql.getValue().contains("s.estado=?"));
        verify(ps).setObject(1,3);verify(ps).setObject(2,"Desactualizado");
        verify(ps).setObject(3,"%TSU - DSM%");verify(ps).setObject(4,"%Java%");
    }
}
