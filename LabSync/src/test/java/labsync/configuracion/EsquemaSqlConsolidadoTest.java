package labsync.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class EsquemaSqlConsolidadoTest {
    private static final Path REPOSITORIO = Path.of("..").normalize();
    private static final Path SQL = REPOSITORIO.resolve("labsync_db/labsync_db.sql");
    private static final Set<String> TABLAS = Set.of(
            "alertas", "bitacora", "ciclos_escolares", "estudiante", "externo",
            "horarios_clase", "inventario", "laboratorios", "laboratorista",
            "mantenimiento", "reporte_fallas", "reservas", "software_laboratorio",
            "usuario");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?is)create\\s+table\\s+([a-z_]+)\\s*\\((.*?)\\n\\)");
    private static final Pattern COLUMNA = Pattern.compile("(?m)^    ([a-z_]+)\\s+");

    @Test
    void soloExisteElArchivoSqlAutoritativo() throws IOException {
        List<Path> archivosSql = new ArrayList<>();
        try (var rutas = Files.walk(REPOSITORIO)) {
            rutas.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
                            .endsWith(".sql"))
                    .filter(path -> !contieneSegmento(path, "target"))
                    .filter(path -> !contieneSegmento(path, ".git"))
                    .forEach(archivosSql::add);
        }
        assertEquals(List.of(SQL.normalize()), archivosSql.stream().map(Path::normalize).toList());
    }

    @Test
    void defineExactamenteLasCatorceTablasYCientoTreintaColumnas() throws IOException {
        String sql = Files.readString(SQL);
        Matcher tablas = CREATE_TABLE.matcher(sql);
        Set<String> encontradas = new java.util.HashSet<>();
        int totalColumnas = 0;
        int columnasSoftware = 0;
        while (tablas.find()) {
            String tabla = tablas.group(1).toLowerCase(Locale.ROOT);
            encontradas.add(tabla);
            int columnas = contarColumnas(tablas.group(2));
            totalColumnas += columnas;
            if (tabla.equals("software_laboratorio")) {
                columnasSoftware = columnas;
            }
        }
        assertEquals(TABLAS, encontradas);
        assertEquals(14, encontradas.size());
        assertEquals(130, totalColumnas);
        assertEquals(10, columnasSoftware);
        for (String eliminada : List.of("trayectorias", "grupos", "materias", "plan_materias")) {
            assertFalse(Pattern.compile("(?i)create\\s+table\\s+" + eliminada)
                    .matcher(sql).find());
        }
    }

    @Test
    void conservaRestriccionesIndicesYTriggersVigentes() throws IOException {
        String sql = Files.readString(SQL).toLowerCase(Locale.ROOT);
        for (String texto : List.of(
                "constraint fk_software_laboratorio_laboratorio",
                "on update cascade on delete restrict",
                "constraint uk_software_laboratorio_nombre unique",
                "index idx_software_laboratorio", "index idx_software_estado",
                "constraint chk_alerta_un_origen", "constraint uk_alerta_origen",
                "constraint chk_mantenimiento_tipo", "constraint chk_inventario_tipo",
                "create trigger trg_horario_bi_integridad",
                "create trigger trg_horario_bu_integridad",
                "create trigger trg_reserva_bi_integridad",
                "create trigger trg_reserva_bu_integridad",
                "create trigger trg_bitacora_bi_laboratorio",
                "create trigger trg_bitacora_bu_laboratorio",
                "create trigger trg_falla_bi_laboratorio",
                "create trigger trg_falla_bu_laboratorio")) {
            assertTrue(sql.contains(texto), () -> "Falta en el esquema: " + texto);
        }
        assertFalse(sql.contains("create trigger trg_alerta_bi_origen"));
        assertFalse(sql.contains("create trigger trg_alerta_bu_origen"));
    }

    @Test
    void documentacionSoloMencionaElSqlConsolidado() throws IOException {
        Pattern referenciaSql = Pattern.compile("(?i)([a-z0-9_./-]+\\.sql)");
        List<String> referenciasInvalidas = new ArrayList<>();
        try (var rutas = Files.walk(REPOSITORIO)) {
            for (Path path : rutas.filter(Files::isRegularFile)
                    .filter(p -> !contieneSegmento(p, "target"))
                    .filter(p -> !contieneSegmento(p, ".git"))
                    .filter(p -> !contieneSegmento(p, ".idea"))
                    .filter(p -> {
                        String nombre = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return nombre.endsWith(".md") || nombre.endsWith(".html");
                    })
                    .filter(p -> !p.equals(SQL))
                    .filter(p -> !p.normalize().equals(REPOSITORIO.resolve(
                            "LabSync/src/test/java/labsync/configuracion/EsquemaSqlConsolidadoTest.java")
                            .normalize()))
                    .toList()) {
                String contenido;
                try {
                    contenido = Files.readString(path);
                } catch (IOException ex) {
                    continue;
                }
                Matcher matcher = referenciaSql.matcher(contenido);
                while (matcher.find()) {
                    if (!matcher.group(1).endsWith("labsync_db.sql")) {
                        referenciasInvalidas.add(path + ": " + matcher.group(1));
                    }
                }
            }
        }
        assertTrue(referenciasInvalidas.isEmpty(), referenciasInvalidas::toString);
    }

    private int contarColumnas(String cuerpoTabla) {
        int total = 0;
        Matcher columnas = COLUMNA.matcher(cuerpoTabla);
        while (columnas.find()) {
            if (!Set.of("constraint", "index", "primary", "foreign", "unique", "check")
                    .contains(columnas.group(1).toLowerCase(Locale.ROOT))) {
                total++;
            }
        }
        return total;
    }

    private static boolean contieneSegmento(Path path, String segmento) {
        for (Path parte : path) {
            if (parte.toString().equals(segmento)) return true;
        }
        return false;
    }
}
