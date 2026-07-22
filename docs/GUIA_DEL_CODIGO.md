# Guía breve del código de LabSync

Esta guía complementa los comentarios Javadoc del código. Los archivos `package-info.java`
describen cada paquete y cada clase pública indica su responsabilidad principal. En las
ventanas creadas con NetBeans, el bloque `initComponents` es código generado por el diseñador:
su función es construir y acomodar controles, por lo que no debe editarse manualmente.

## Arranque, configuración y modelo

| Archivo | Función |
|---|---|
| `AplicacionLabSync` | Inicia el programa y muestra la pantalla de acceso. |
| `ConexionBaseDatos` | Abre conexiones JDBC hacia MySQL/MariaDB. |
| `SesionUsuario` | Conserva la identidad y el rol durante la navegación. |
| `Alerta` | Representa una alerta guardada en la base de datos. |

## Persistencia y servicios

| Archivo | Función |
|---|---|
| `CatalogoLaboratorios` | Consulta laboratorios y llena listas desplegables. |
| `ConsultaEscalar` | Ejecuta consultas que devuelven un único valor. |
| `ConsultaTabla` | Convierte resultados SQL en modelos para tablas Swing. |
| `RepositorioAlertas` | Lee, crea y actualiza alertas persistidas. |
| `ServicioAlertas` | Detecta eventos que deben producir notificaciones. |
| `ServicioDisponibilidad` | Aplica las reglas de ocupación y bloqueo de laboratorios. |
| `ServicioMantenimiento` | Coordina cambios de mantenimiento e inventario en transacciones. |

## Autenticación y paneles

| Archivo | Función |
|---|---|
| `VentanaInicioSesion` | Autentica y dirige al panel del rol encontrado. |
| `VentanaRegistroUsuario` | Valida y registra una cuenta y su perfil. |
| `VentanaPanelAlumno` | Ofrece al alumno accesos a reservas y reportes. |
| `VentanaPanelProfesor` | Ofrece al profesor accesos a reservas, bitácora y fallas. |
| `VentanaPanelLaboratorista` | Centraliza las operaciones administrativas. |

## Reservas y bitácora

| Archivo | Función |
|---|---|
| `VentanaReservasAlumno` | Solicita una computadora en un horario disponible. |
| `VentanaReservasProfesor` | Solicita un laboratorio completo para un grupo. |
| `VentanaMisReservasProfesor` | Consulta y cancela reservas del profesor. |
| `VentanaGestionReservas` | Revisa y resuelve solicitudes pendientes. |
| `VentanaBitacoraProfesor` | Registra el uso real de una sesión de laboratorio. |
| `VentanaBitacoraGeneral` | Consulta y exporta el historial completo. |

## Fallas, inventario y mantenimiento

| Archivo | Función |
|---|---|
| `VentanaReporteFallaAlumno` | Registra y consulta fallas reportadas por un alumno. |
| `VentanaReporteFallaProfesor` | Registra y consulta fallas reportadas por un profesor. |
| `VentanaGestionReportesFallas` | Atiende reportes y puede derivarlos a mantenimiento. |
| `VentanaGestionInventario` | Administra equipos y su estado operativo. |
| `VentanaGestionMantenimiento` | Programa, modifica y concluye mantenimientos. |

## Utilidades de interfaz

| Archivo | Función |
|---|---|
| `ActualizacionAutomatica` | Ejecuta lecturas periódicas sin bloquear Swing. |
| `ControlNotificacionesLaboratorista` | Gestiona la campana de alertas administrativas. |
| `ControlNotificacionesReserva` | Notifica al solicitante cambios en sus reservas. |
| `DialogoAlertas` | Presenta y permite atender alertas. |
| `NotificacionesGlobales` | Instala el control de notificaciones adecuado al rol. |
| `Recursos` | Carga imágenes empaquetadas con la aplicación. |
| `SonidosNotificacion` | Genera sonidos de aviso sin archivos externos. |
| `ValidacionFechas` | Comparte restricciones de fecha entre calendarios. |

## Recursos y construcción

| Elemento | Función |
|---|---|
| Archivos `.form` | Guardan la definición visual editable por NetBeans. |
| `resources/images` | Contiene logotipos e iconos usados por Swing. |
| `labsync_db.sql` | Define el esquema y los datos iniciales de la base. |
| `DB/migrations` | Contiene cambios incrementales del esquema. |
| `pom.xml` | Declara módulos, dependencias y empaquetado Maven. |
| `mvnw` y `mvnw.cmd` | Permiten compilar sin instalar Maven globalmente. |
| `Ejecutar-LabSync.cmd` | Facilita la ejecución del JAR en Windows. |

Para entender los recorridos completos entre estas piezas, consulta
`docs/ARQUITECTURA_Y_FLUJOS.md`.
