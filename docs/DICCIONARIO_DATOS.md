# Diccionario de datos

Este diccionario corresponde a la fuente de verdad
[`labsync_db/labsync_db.sql`](../labsync_db/labsync_db.sql). Es el único archivo
de instalación y define exactamente 14 tablas y 130 columnas, todas InnoDB.
“—” indica que el DDL no declara valor predeterminado.

## Usuarios

### `usuario`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id` | `INT`, auto incremento | PK | No | auto incremento | Identificador del usuario. |
| `nombre` | `VARCHAR(80)` | — | No | — | Nombre del usuario. |
| `apellido_p` | `VARCHAR(80)` | — | No | — | Apellido paterno. |
| `apellido_m` | `VARCHAR(80)` | — | Sí | — | Apellido materno. |
| `rol` | `ENUM('Estudiante','Profesor','Laboratorista','Externo')` | — | No | — | Rol que controla el acceso y las validaciones por tipo de usuario. |
| `correo` | `VARCHAR(150)` | UK | No | — | Correo usado para identificar la cuenta; no se repite. |
| `password` | `CHAR(64)` | — | No | — | Hash SHA-256 hexadecimal heredado usado por la autenticación actual. |

### `estudiante`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_usuario` | `INT` | PK, FK → `usuario.id` | No | — | Usuario que tiene el perfil de estudiante; actualiza en cascada y se elimina con él. |
| `matricula` | `VARCHAR(50)` | UK | No | — | Matrícula única capturada durante el registro. |
| `carrera` | `VARCHAR(100)` | — | No | — | Carrera descriptiva seleccionada durante el registro, por ejemplo `TSU - DSM`. |
| `turno` | `ENUM('Matutino','Vespertino','Mixto')` | — | Sí | — | Turno académico del estudiante. |

### `laboratorista`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_usuario` | `INT` | PK, FK → `usuario.id` | No | — | Usuario que tiene el perfil de laboratorista; actualiza en cascada y se elimina con él. |
| `turno` | `ENUM('Matutino','Vespertino','Mixto')` | — | No | — | Turno laboral del laboratorista. |
| `piso_encargado` | `VARCHAR(50)` | — | No | — | Piso o conjunto de pisos bajo su responsabilidad. |

### `externo`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_usuario` | `INT` | PK, FK → `usuario.id` | No | — | Usuario que tiene el perfil externo; actualiza en cascada y se elimina con él. |
| `institucion_origen` | `VARCHAR(150)` | — | No | — | Institución de procedencia capturada en el registro. |
| `motivo_visita` | `TEXT` | — | Sí | — | Motivo declarado para la visita. |

## Operación

### `laboratorios`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_laboratorio` | `INT`, auto incremento | PK | No | auto incremento | Identificador del laboratorio. |
| `nombre` | `VARCHAR(20)` | UK | No | — | Nombre único mostrado por la interfaz. |
| `total_equipos` | `INT UNSIGNED` | — | No | — | Estaciones de cómputo disponibles para reservas individuales; debe ser mayor que cero. |
| `capacidad_personas` | `INT UNSIGNED` | — | No | — | Aforo máximo usado en reservas grupales; debe ser mayor que cero. |
| `estado` | `ENUM('Disponible','No disponible','En mantenimiento')` | — | No | `Disponible` | Estado operativo que participa en la disponibilidad. |

### `ciclos_escolares`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_ciclo` | `INT`, auto incremento | PK | No | auto incremento | Identificador del ciclo escolar. |
| `nombre` | `VARCHAR(50)` | UK | No | — | Nombre único visible del ciclo. |
| `fecha_inicio` | `DATE` | — | No | — | Primer día de vigencia. |
| `fecha_fin` | `DATE` | — | No | — | Último día de vigencia; no puede ser anterior al inicio. |
| `activo` | `TINYINT(1)` | — | No | `1` | Indica si el ciclo participa en consultas operativas. |

### `horarios_clase`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_horario` | `INT`, auto incremento | PK | No | auto incremento | Identificador de la asignación regular. |
| `id_ciclo` | `INT` | FK → `ciclos_escolares.id_ciclo` | No | — | Ciclo durante el cual está vigente el horario; actualiza en cascada. |
| `id_profesor` | `INT` | FK → `usuario.id` | No | — | Profesor responsable; el trigger exige rol `Profesor`. |
| `id_laboratorio` | `INT` | FK → `laboratorios.id_laboratorio` | No | — | Laboratorio asignado; actualiza en cascada. |
| `carrera` | `VARCHAR(100)` | — | No | — | Carrera descriptiva del grupo. |
| `cuatrimestre` | `TINYINT UNSIGNED` | — | No | — | Cuatrimestre del 1 al 11. |
| `grupo` | `VARCHAR(10)` | — | No | — | Nombre o letra del grupo. |
| `turno` | `ENUM('Matutino','Vespertino')` | — | No | — | Turno que también delimita los módulos horarios permitidos en Java. |
| `materia` | `VARCHAR(150)` | — | No | — | Actividad o materia descriptiva de la clase. |
| `dia_semana` | `ENUM('Lunes','Martes','Miércoles','Jueves','Viernes','Sábado')` | — | No | — | Día semanal de la clase. |
| `hora_inicio` | `TIME` | — | No | — | Inicio del intervalo. |
| `hora_fin` | `TIME` | — | No | — | Fin del intervalo; debe ser posterior al inicio. |
| `activo` | `TINYINT(1)` | — | No | `1` | Habilita la clase para consultas, bitácora y conflictos. |

La identidad del grupo académico es `carrera + cuatrimestre + grupo + turno`.
Los índices cubren conflictos por laboratorio, profesor y esa identidad dentro
de ciclo, día e intervalo.

### `reservas`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_reserva` | `INT`, auto incremento | PK | No | auto incremento | Identificador de la reserva extraordinaria. |
| `id_usuario` | `INT` | FK → `usuario.id` | No | — | Usuario solicitante; actualiza en cascada. |
| `id_laboratorio` | `INT` | FK → `laboratorios.id_laboratorio` | No | — | Laboratorio solicitado; actualiza en cascada. |
| `actividad` | `VARCHAR(150)` | — | No | — | Actividad declarada para la reserva. |
| `carrera` | `VARCHAR(100)` | — | Sí | — | Carrera descriptiva aplicable a una reserva grupal. |
| `grado` | `VARCHAR(20)` | — | Sí | — | Grado o cuatrimestre descriptivo de la reserva. |
| `grupo` | `VARCHAR(20)` | — | Sí | — | Grupo descriptivo de la reserva. |
| `turno` | `VARCHAR(50)` | — | Sí | — | Turno descriptivo de la reserva. |
| `fecha` | `DATE` | — | No | — | Fecha solicitada. |
| `hora_inicio` | `TIME` | — | No | — | Inicio de la reserva. |
| `hora_fin` | `TIME` | — | No | — | Fin de la reserva; debe ser posterior al inicio. |
| `cantidad_alumnos` | `INT UNSIGNED` | — | No | — | Personas/equipos requeridos; debe ser mayor que cero. |
| `estado` | `ENUM('Pendiente','Aprobada','Rechazada','Cancelada','Finalizada')` | — | No | `Pendiente` | Estado del flujo de aprobación y uso. |
| `observaciones` | `TEXT` | — | Sí | — | Notas sobre la solicitud o resolución. |
| `fecha_registro` | `TIMESTAMP` | — | No | `CURRENT_TIMESTAMP` | Momento de creación. |

### `bitacora`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_bitacora` | `INT`, auto incremento | PK | No | auto incremento | Identificador de la entrada histórica. |
| `id_usuario` | `INT` | FK → `usuario.id` | Sí | — | Usuario asociado; queda `NULL` si se elimina. |
| `id_reserva` | `INT` | FK → `reservas.id_reserva` | Sí | — | Reserva extraordinaria de origen; queda `NULL` si se elimina. |
| `id_horario` | `INT` | FK → `horarios_clase.id_horario` | Sí | — | Clase regular de origen; queda `NULL` si se elimina. Junto con `fecha` es único. |
| `id_laboratorio` | `INT` | FK → `laboratorios.id_laboratorio` | Sí | — | Laboratorio relacionado; el trigger verifica que coincida con su fotografía textual. |
| `fecha` | `DATE` | — | No | — | Fecha del uso registrado. |
| `nombre_usuario` | `VARCHAR(240)` | — | No | — | Fotografía histórica del nombre. |
| `rol_usuario` | `VARCHAR(50)` | — | No | — | Fotografía histórica del rol. |
| `carrera_dependencia` | `VARCHAR(100)` | — | No | — | Carrera o dependencia conservada históricamente. |
| `grado` | `VARCHAR(20)` | — | Sí | — | Grado/cuatrimestre histórico. |
| `grupo` | `VARCHAR(20)` | — | Sí | — | Grupo histórico. |
| `laboratorio` | `VARCHAR(50)` | — | No | — | Fotografía histórica del nombre del laboratorio. |
| `actividad_materia` | `VARCHAR(150)` | — | No | — | Actividad o materia realizada. |
| `turno` | `VARCHAR(50)` | — | No | — | Turno registrado. |
| `horario` | `VARCHAR(50)` | — | No | — | Intervalo textual conservado históricamente. |
| `total_usuarios` | `INT UNSIGNED` | — | No | — | Total de asistentes; debe ser mayor que cero. |
| `observaciones` | `TEXT` | — | Sí | — | Notas del registro de uso. |
| `estado` | `VARCHAR(50)` | — | No | `Registrado` | Estado textual de la entrada. |
| `fecha_registro` | `TIMESTAMP` | — | No | `CURRENT_TIMESTAMP` | Momento en que se guardó la entrada. |

## Control técnico

### `inventario`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_inventario` | `INT`, auto incremento | PK | No | auto incremento | Identificador del equipo. |
| `id_laboratorio` | `INT` | FK → `laboratorios.id_laboratorio` | No | — | Laboratorio al que pertenece; actualiza en cascada. |
| `codigo` | `VARCHAR(50)` | UK | No | — | Código interno único del equipo. |
| `nombre_equipo` | `VARCHAR(100)` | — | No | — | Nombre descriptivo del equipo. |
| `tipo_dispositivo` | `VARCHAR(50)` con `CHECK` | — | No | — | Tipo permitido por la interfaz, incluidos material peligroso, batería, batería de UPS, tóner y residuo electrónico. |
| `marca` | `VARCHAR(80)` | — | Sí | — | Marca, cuando se conoce. |
| `modelo` | `VARCHAR(80)` | — | Sí | — | Modelo, cuando se conoce. |
| `no_serie` | `VARCHAR(100)` | UK | Sí | — | Número de serie único, cuando se registra. |
| `estado` | `ENUM('Disponible','En mantenimiento','Con falla','Baja')` | — | No | `Disponible` | Estado técnico del equipo. |
| `ultimo_mantenimiento` | `DATE` | — | Sí | — | Fecha del último mantenimiento realizado. |
| `observaciones` | `TEXT` | — | Sí | — | Notas técnicas del equipo. |
| `fecha_registro` | `TIMESTAMP` | — | No | `CURRENT_TIMESTAMP` | Momento de alta en inventario. |

### `software_laboratorio`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_software` | `INT`, auto incremento | PK | No | auto incremento | Identificador del registro de software. |
| `id_laboratorio` | `INT` | FK → `laboratorios.id_laboratorio`, UK compuesta | No | — | Laboratorio donde se controla; actualiza en cascada y restringe el borrado del laboratorio. |
| `nombre` | `VARCHAR(150)` | UK compuesta | No | — | Nombre único dentro del mismo laboratorio. |
| `version_instalada` | `VARCHAR(80)` | — | Sí | — | Versión presente; puede ser nula para una instalación pendiente. |
| `version_objetivo` | `VARCHAR(80)` | — | Sí | — | Versión requerida; es obligatoria funcionalmente cuando está desactualizado. |
| `uso_academico` | `VARCHAR(200)` | — | No | `General` | Carrera o combinación de usos académicos separada por punto y coma. |
| `estado` | `ENUM('Actualizado','Desactualizado','Pendiente de instalación','Pendiente de eliminación','Eliminado')` | — | No | `Actualizado` | Acción o condición requerida. `Eliminado` conserva el historial. |
| `fecha_revision` | `DATE` | — | Sí | — | Última fecha de revisión registrada. |
| `observaciones` | `TEXT` | — | Sí | — | Justificación o notas; obligatorias para eliminación pendiente. |
| `fecha_registro` | `TIMESTAMP` | — | No | `CURRENT_TIMESTAMP` | Momento del alta. |

La clave única es `id_laboratorio + nombre`. Existen índices explícitos por
laboratorio y por estado. No hay borrado físico ni relación directa con
`mantenimiento`; el estado indica la acción necesaria y el laboratorista puede
programar una “Actualización de software” en el módulo existente.

### `reporte_fallas`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_falla` | `INT`, auto incremento | PK | No | auto incremento | Identificador del reporte. |
| `id_usuario` | `INT` | FK → `usuario.id` | Sí | — | Usuario reportante; queda `NULL` si se elimina. |
| `id_inventario` | `INT` | FK → `inventario.id_inventario` | Sí | — | Equipo afectado; queda `NULL` si se elimina. |
| `id_reserva` | `INT` | FK → `reservas.id_reserva` | Sí | — | Reserva durante la que ocurrió; queda `NULL` si se elimina. |
| `id_laboratorio` | `INT` | FK → `laboratorios.id_laboratorio` | No | — | Laboratorio afectado; actualiza en cascada. |
| `descripcion_falla` | `TEXT` | — | No | — | Descripción del problema informado. |
| `prioridad` | `ENUM('Baja','Media','Alta','Crítica')` | — | No | `Media` | Prioridad de atención. |
| `estado` | `ENUM('Pendiente','En revisión','Atendida','Cancelada')` | — | No | `Pendiente` | Estado de seguimiento. |
| `fecha_reporte` | `TIMESTAMP` | — | No | `CURRENT_TIMESTAMP` | Momento del reporte. |
| `fecha_revision` | `DATE` | — | Sí | — | Fecha en que se revisó, cuando aplica. |
| `observaciones` | `TEXT` | — | Sí | — | Notas de seguimiento o resolución. |

### `mantenimiento`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_mantenimiento` | `INT`, auto incremento | PK | No | auto incremento | Identificador del mantenimiento. |
| `id_inventario` | `INT` | FK → `inventario.id_inventario` | No | — | Equipo intervenido; actualiza en cascada. |
| `id_falla` | `INT` | FK → `reporte_fallas.id_falla` | Sí | — | Reporte que originó la intervención; queda `NULL` si se elimina. |
| `tipo_mantenimiento` | `VARCHAR(100)` con `CHECK` | — | No | — | Preventivo, correctivo, actualización de software o hardware, disposición de material peligroso, retiro de equipo obsoleto, limpieza u otro. |
| `fecha_programada` | `DATE` | — | No | — | Fecha prevista; interviene en el bloqueo de disponibilidad. |
| `estado` | `ENUM('Pendiente','En proceso','Realizado','Cancelado')` | — | No | `Pendiente` | Estado de ejecución. |
| `responsable` | `VARCHAR(150)` | — | No | — | Nombre o identificación textual del responsable. |
| `observaciones` | `TEXT` | — | Sí | — | Notas sobre el mantenimiento. |
| `fecha_registro` | `TIMESTAMP` | — | No | `CURRENT_TIMESTAMP` | Momento de creación. |

### `alertas`

| Campo | Tipo / longitud o valores | Clave | Nulos | Predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id_alerta` | `INT`, auto incremento | PK | No | auto incremento | Identificador de la alerta. |
| `id_reserva` | `INT` | FK → `reservas.id_reserva` | Sí | — | Reserva de origen, cuando el tipo corresponde; se elimina en cascada. |
| `id_falla` | `INT` | FK → `reporte_fallas.id_falla` | Sí | — | Falla de origen, cuando corresponde; se elimina en cascada. |
| `id_mantenimiento` | `INT` | FK → `mantenimiento.id_mantenimiento` | Sí | — | Mantenimiento de origen, cuando corresponde; se elimina en cascada. |
| `id_inventario` | `INT` | FK → `inventario.id_inventario` | Sí | — | Equipo de origen, cuando corresponde; se elimina en cascada. |
| `tipo` | `VARCHAR(40)` | UK compuesta | No | — | Descripción funcional de la clase de alerta. |
| `referencia` | `VARCHAR(50)` | UK compuesta | No | — | Referencia descriptiva para identificar el asunto ante el usuario; única en combinación con `tipo`. |
| `titulo` | `VARCHAR(120)` | — | No | — | Título mostrado en notificaciones. |
| `detalle` | `VARCHAR(500)` | — | No | — | Detalle mostrado en notificaciones. |
| `prioridad` | `ENUM('Baja','Media','Alta','Crítica')` | — | No | `Media` | Prioridad de la alerta. |
| `estado` | `ENUM('Nueva','Leída','Atendida')` | — | No | `Nueva` | Estado de atención. |
| `fecha_creacion` | `TIMESTAMP` | — | No | `CURRENT_TIMESTAMP` | Momento de generación. |
| `fecha_lectura` | `TIMESTAMP` | — | Sí | — | Momento en que se marcó leída. |
| `fecha_atencion` | `TIMESTAMP` | — | Sí | — | Momento en que se marcó atendida. |

La restricción `chk_alerta_un_origen` exige exactamente uno de los cuatro
identificadores de origen. `tipo` y `referencia` conservan su función
descriptiva; el origen relacional verdadero es la FK no nula.
