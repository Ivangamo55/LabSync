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

## Base de datos

1. Iniciar MySQL o MariaDB en `localhost:3306`.
2. Importar `LabSync/src/main/resources/DB/labsync_db.sql`.
3. Aplicar en orden los scripts de `LabSync/src/main/resources/DB/migrations`.
   Los horarios UTJ-CCD se incorporan mediante
   `20260722_agregar_horarios_escolares_ccd.sql`.
4. La configuración actual usa la base `labsync_db`, usuario `root` y
   contraseña vacía; se encuentra en
   `LabSync/src/main/java/labsync/configuracion/ConexionBaseDatos.java`.

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
