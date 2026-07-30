# LabSync

> [Documentación visual de arquitectura y flujos](docs/ARQUITECTURA_Y_FLUJOS.md)

Aplicación de escritorio Java Swing para administrar laboratorios, reservas,
bitácoras, inventario y reportes de fallas. El repositorio se puede compilar
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
`LabSync/target/site/jacoco/index.html`. El reporte actual contiene 41 pruebas:
ninguna falla, ningún error y ninguna prueba omitida.

La cobertura actual está concentrada en la lógica que administra
disponibilidad, horarios, mantenimiento y alertas:

| Paquete | Instrucciones | Ramas |
| --- | ---: | ---: |
| `labsync.servicio` | 65 % | 45 % |
| `labsync.persistencia` | 31 % | 21 % |
| Proyecto completo | 2 % | 6 % |

El porcentaje global es bajo porque la mayoría de las ventanas Swing aún no
tiene pruebas automatizadas. Por ello, el reporte no debe interpretarse como
una validación completa de la interfaz ni de todos los flujos de la aplicación.

## Base de datos

1. Iniciar MySQL o MariaDB en `localhost:3306`.
2. Importar `LabSync/src/main/resources/DB/labsync_db.sql`.
   Este archivo es la única fuente de verdad y crea el esquema completo para
   una instalación nueva.
3. La conexión se centraliza en `ConexionBaseDatos` y admite las variables
   `LABSYNC_DB_URL`, `LABSYNC_DB_USER` y `LABSYNC_DB_PASSWORD`. Los valores
   locales predeterminados conservan la instalación de desarrollo existente.

El script completo recrea tablas y carga datos iniciales. No debe reimportarse
sobre una base existente cuando se necesite conservar sus datos; antes de
cualquier cambio manual se recomienda crear un respaldo.

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
