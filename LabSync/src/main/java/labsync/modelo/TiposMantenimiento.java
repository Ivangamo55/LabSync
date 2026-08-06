package labsync.modelo;

import java.util.Set;

/** Nombres y reglas compartidas de los tipos de mantenimiento de LabSync. */
public final class TiposMantenimiento {
    public static final String PREVENTIVO = "Preventivo";
    public static final String CORRECTIVO = "Correctivo";
    public static final String ACTUALIZACION_SOFTWARE = "Actualización de software";
    public static final String ACTUALIZACION_HARDWARE = "Actualización de hardware";
    public static final String DISPOSICION_MATERIAL_PELIGROSO = "Disposición de material peligroso";
    public static final String RETIRO_EQUIPO_OBSOLETO = "Retiro de equipo obsoleto";
    public static final String LIMPIEZA = "Limpieza";
    public static final String OTRO = "Otro";

    private static final Set<String> TIPOS_BAJA = Set.of(
            RETIRO_EQUIPO_OBSOLETO, DISPOSICION_MATERIAL_PELIGROSO);

    private TiposMantenimiento() {
    }

    public static boolean causaBaja(String tipo) {
        return TIPOS_BAJA.contains(tipo);
    }

    public static boolean requiereObservaciones(String tipo) {
        return causaBaja(tipo);
    }

    public static String[] opcionesRegistro() {
        return new String[] {"Selecciona", PREVENTIVO, CORRECTIVO,
            ACTUALIZACION_SOFTWARE, ACTUALIZACION_HARDWARE,
            DISPOSICION_MATERIAL_PELIGROSO, RETIRO_EQUIPO_OBSOLETO,
            LIMPIEZA, OTRO};
    }

    public static String[] opcionesFiltro() {
        String[] registro = opcionesRegistro();
        registro[0] = "Todos";
        return registro;
    }
}
