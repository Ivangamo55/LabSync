# LabSync

> [Documentación visual de arquitectura y flujos](docs/ARQUITECTURA_Y_FLUJOS.md)

Aplicación de escritorio Java Swing para administrar laboratorios, reservas,
bitácoras, inventario, software instalado y reportes de fallas. El repositorio se puede compilar
desde esta carpeta raíz; no es necesario instalar Maven globalmente.

## Requisitos

- JDK 17 o posterior (JDK, no solamente JRE).
- MySQL o MariaDB para usar las funciones que consultan datos.
- Un entorno gráfico para abrir la interfaz Swing.

Maven se descarga automáticamente mediante Maven Wrapper. La primera
compilación necesita acceso a Internet para descargar Maven y dependencias.

## Compilar y verificar

Desde la raíz del repositorio:

```powershell
.\mvnw.cmd clean verify
```

En Linux/macOS o en un contenedor:

```sh
./mvnw clean verify
```

El JAR ejecutable queda en `LabSync/target/LabSync-1.0.jar`.

## Pruebas y cobertura

`clean verify` ejecuta las pruebas unitarias con JUnit 5 y genera el reporte de
cobertura de JaCoCo en
`LabSync/target/site/jacoco/index.html`. El reporte actual contiene 102 pruebas:
ninguna falla, ningún error y ninguna prueba omitida.

La cobertura está concentrada en la lógica que administra disponibilidad,
horarios, mantenimiento y alertas. El porcentaje global es bajo porque la
mayoría de las ventanas Swing aún no
tiene pruebas automatizadas. Por ello, el reporte no debe interpretarse como
una validación completa de la interfaz ni de todos los flujos de la aplicación.

## Base de datos e instalación

1. Iniciar MariaDB/XAMPP.
2. Importar [`labsync_db/labsync_db.sql`](labsync_db/labsync_db.sql).
3. Configurar la conexión si es necesario mediante `LABSYNC_DB_URL`,
   `LABSYNC_DB_USER` y `LABSYNC_DB_PASSWORD`.
4. Iniciar LabSync.
5. Ingresar con la cuenta ficticia de
   [`docs/CREDENCIALES_PRUEBA.md`](docs/CREDENCIALES_PRUEBA.md).

El archivo indicado es el único SQL del repositorio y la única fuente de verdad:
crea de forma determinista 14 tablas, 130 columnas, restricciones, índices,
disparadores y datos mínimos de demostración. Como recrea las tablas, se debe
respaldar cualquier instalación existente antes de importarlo. No elimina la
base de datos completa.

La pestaña `Software por laboratorio` del inventario controla nombre, versión
instalada, versión objetivo, uso académico y fecha de revisión. Sus estados
identifican software actualizado, desactualizado, con instalación o eliminación
pendiente y eliminado; `Eliminado` conserva la fila como historial.

El modelo académico se limita a `ciclos_escolares`, `horarios_clase` y los
datos descriptivos del perfil `estudiante`. Carrera, cuatrimestre, grupo,
turno y materia se almacenan directamente donde LabSync los necesita; no se
administra un plan de estudios.

La autenticación actual conserva hashes SHA-256 hexadecimales por
compatibilidad. Antes de producción debe migrarse a Argon2id o bcrypt con sal
individual y actualización gradual de hashes.

## Reglas principales de reservación

- Las reservas se asocian al identificador del usuario autenticado, no al
  nombre mostrado en la interfaz.
- Una reserva de profesor ocupa el laboratorio de forma exclusiva durante su
  horario.
- Los alumnos pueden compartir un laboratorio mientras existan equipos
  disponibles; cada reserva consume un equipo y nunca debe superar
  `total_equipos`.
- Una clase programada, una reserva exclusiva o el mantenimiento del
  laboratorio bloquean nuevas reservas incompatibles.
- La disponibilidad se vuelve a validar dentro de la transacción y también se
  protege mediante restricciones y disparadores de la base de datos.

## Mantenimiento técnico y disposición

LabSync programa y alerta mantenimientos preventivos, correctivos,
actualizaciones de software y hardware, disposición de material peligroso y
retiro de equipos obsoletos. Los dos últimos requieren observaciones y, al
finalizar, conservan la fila histórica del inventario con estado `Baja`; los
demás dejan el equipo `Disponible` y actualizan la fecha del último
mantenimiento.

El inventario admite material peligroso, baterías, baterías de UPS, tóner y
residuos electrónicos. Cantidad, ubicación, condición e indicaciones
administrativas se registran en observaciones. LabSync únicamente registra,
programa y avisa; no prescribe procedimientos físicos o químicos de manejo.

## Ejecutar la interfaz

En Windows se puede hacer doble clic en `Ejecutar-LabSync.cmd`, o ejecutar:

```powershell
java -jar .\LabSync\target\LabSync-1.0.jar
```

En Linux/macOS:

```sh
java -jar ./LabSync/target/LabSync-1.0.jar
```

La aplicación es de escritorio. Un entorno remoto o navegador sin servidor
gráfico puede compilarla y analizarla, pero no mostrar sus ventanas Swing.

## Guía para asistentes y entornos automatizados

- El `pom.xml` de la raíz es el punto de entrada y contiene el módulo
  `LabSync`.
- Usar siempre `mvnw`/`mvnw.cmd`; no exigir una instalación global de Maven.
- Para validar cambios usar `clean verify`. No intentar abrir la interfaz en
  un entorno *headless*.
- Java 17 es la versión mínima de compilación.
- No hace falta una base de datos para compilar; sí para probar los flujos que
  acceden a ella.
