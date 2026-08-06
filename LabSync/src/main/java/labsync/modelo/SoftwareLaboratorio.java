package labsync.modelo;

import java.time.LocalDate;

/** Registro histórico del software controlado en un laboratorio. */
public record SoftwareLaboratorio(int id, int idLaboratorio, String laboratorio,
        String nombre, String versionInstalada, String versionObjetivo,
        String usoAcademico, String estado, LocalDate fechaRevision,
        String observaciones) { }
