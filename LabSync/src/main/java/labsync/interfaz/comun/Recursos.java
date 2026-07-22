package labsync.interfaz.comun;

/** Carga recursos gráficos sin provocar fallos cuando el IDE omite copiarlos. */
public final class Recursos {

    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(Recursos.class.getName());

    private Recursos() {
    }

    public static javax.swing.ImageIcon icono(String ruta) {
        java.net.URL recurso = Recursos.class.getResource(ruta);
        if (recurso != null) {
            return new javax.swing.ImageIcon(recurso);
        }

        String rutaRelativa = ruta.startsWith("/") ? ruta.substring(1) : ruta;
        java.nio.file.Path archivo = java.nio.file.Paths.get("src", "main", "resources")
                .resolve(rutaRelativa);
        if (java.nio.file.Files.isRegularFile(archivo)) {
            return new javax.swing.ImageIcon(archivo.toAbsolutePath().toString());
        }

        LOGGER.warning("No se encontró el recurso gráfico: " + ruta);
        return new javax.swing.ImageIcon();
    }
}
