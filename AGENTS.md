# Instrucciones del repositorio

Este es un proyecto Maven multimódulo cuyo código está en el módulo `LabSync`.

- Compila desde la raíz con `./mvnw clean verify` (Linux/macOS) o
  `.\mvnw.cmd clean verify` (Windows).
- No asumas que Maven está instalado globalmente: usa Maven Wrapper.
- El proyecto requiere JDK 17 o posterior.
- La aplicación usa Java Swing. En entornos sin pantalla valida la compilación
  y el empaquetado, pero no intentes iniciar el JAR.
- El JAR generado está en `LabSync/target/LabSync-1.0.jar`.
- La base de datos no es necesaria para compilar. Para pruebas de integración,
  importa `LabSync/src/main/resources/DB/labsync_db.sql` en MySQL/MariaDB.
