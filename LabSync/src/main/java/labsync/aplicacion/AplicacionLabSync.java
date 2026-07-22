package labsync.aplicacion;

import labsync.interfaz.autenticacion.VentanaInicioSesion;

/** Punto de entrada: abre la ventana de acceso de la aplicación. */
public class AplicacionLabSync {

    public static void main(String[] args) {
        VentanaInicioSesion ventanaLogin = new VentanaInicioSesion();
        
        ventanaLogin.setVisible(true);
        ventanaLogin.setLocationRelativeTo(null);
    }
}
