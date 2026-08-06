-- ============================================================================
-- ADVERTENCIA: ESQUEMA CONSOLIDADO OFICIAL DE LABSYNC
-- ============================================================================
-- Este archivo es la única fuente SQL autoritativa del proyecto. Instala de
-- forma completa y determinista el esquema y los datos ficticios mínimos.
-- Antes de aplicarlo sobre una instalación existente, genere un respaldo.
-- No contiene DROP DATABASE, pero sí recrea las tablas de la base labsync_db.

SELECT 'ADVERTENCIA: labsync_db.sql recreará las tablas de labsync_db; respalde sus datos antes de continuar.'
    AS advertencia;

-- ============================================================================
-- CONFIGURACIÓN
-- ============================================================================
SET @LABSYNC_OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT;
SET @LABSYNC_OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS;
SET @LABSYNC_OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION;
SET @LABSYNC_OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET @LABSYNC_OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS;
SET @LABSYNC_OLD_SQL_MODE = @@SQL_MODE;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET SQL_MODE = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- ============================================================================
-- BASE DE DATOS
-- ============================================================================
CREATE DATABASE IF NOT EXISTS labsync_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE labsync_db;

-- Eliminar primero los objetos dependientes de instalaciones anteriores.
DROP TRIGGER IF EXISTS trg_alerta_bi_origen;
DROP TRIGGER IF EXISTS trg_alerta_bu_origen;
DROP TRIGGER IF EXISTS trg_bitacora_bi_laboratorio;
DROP TRIGGER IF EXISTS trg_bitacora_bu_laboratorio;
DROP TRIGGER IF EXISTS trg_falla_bi_laboratorio;
DROP TRIGGER IF EXISTS trg_falla_bu_laboratorio;
DROP TRIGGER IF EXISTS trg_horario_bi_integridad;
DROP TRIGGER IF EXISTS trg_horario_bu_integridad;
DROP TRIGGER IF EXISTS trg_reserva_bi_integridad;
DROP TRIGGER IF EXISTS trg_reserva_bu_integridad;

DROP TABLE IF EXISTS alertas;
DROP TABLE IF EXISTS mantenimiento;
DROP TABLE IF EXISTS reporte_fallas;
DROP TABLE IF EXISTS software_laboratorio;
DROP TABLE IF EXISTS inventario;
DROP TABLE IF EXISTS bitacora;
DROP TABLE IF EXISTS reservas;
DROP TABLE IF EXISTS horarios_clase;
DROP TABLE IF EXISTS externo;
DROP TABLE IF EXISTS laboratorista;
DROP TABLE IF EXISTS estudiante;
DROP TABLE IF EXISTS laboratorios;
DROP TABLE IF EXISTS ciclos_escolares;
DROP TABLE IF EXISTS usuario;

-- Catálogos eliminados del modelo vigente; se limpian solo si aún existen.
DROP TABLE IF EXISTS plan_materias;
DROP TABLE IF EXISTS materias;
DROP TABLE IF EXISTS grupos;
DROP TABLE IF EXISTS trayectorias;

-- ============================================================================
-- TABLAS PRINCIPALES
-- ============================================================================
-- ÍNDICES Y RESTRICCIONES se declaran junto a cada tabla para mantener visibles
-- sus PK, FK, UNIQUE, CHECK e índices en el mismo bloque de definición.
create table usuario
(
    id         int auto_increment
        primary key,
    nombre     varchar(80)                                                 not null,
    apellido_p varchar(80)                                                 not null,
    apellido_m varchar(80)                                                 null,
    rol        enum ('Estudiante', 'Profesor', 'Laboratorista', 'Externo') not null,
    correo     varchar(150)                                                not null,
    password   char(64)                                                    not null comment 'Hash SHA-256 hexadecimal heredado; migrar a Argon2id/bcrypt con sal',
    constraint uk_usuario_correo
        unique (correo)
)
    engine = InnoDB;

create table ciclos_escolares
(
    id_ciclo     int auto_increment
        primary key,
    nombre       varchar(50)          not null,
    fecha_inicio date                 not null,
    fecha_fin    date                 not null,
    activo       tinyint(1) default 1 not null,
    constraint uk_ciclo_nombre
        unique (nombre),
    constraint chk_ciclo_fechas
        check (`fecha_fin` >= `fecha_inicio`)
)
    engine = InnoDB;

create table laboratorios
(
    id_laboratorio     int auto_increment
        primary key,
    nombre             varchar(20)                                                                   not null,
    total_equipos      int unsigned                                                                  not null comment 'Estaciones de cómputo disponibles para reservas individuales',
    capacidad_personas int unsigned                                                                  not null comment 'Aforo físico máximo para reservas grupales',
    estado             enum ('Disponible', 'No disponible', 'En mantenimiento') default 'Disponible' not null,
    constraint uk_laboratorio_nombre
        unique (nombre),
    constraint chk_laboratorio_capacidad
        check (`capacidad_personas` > 0),
    constraint chk_laboratorio_equipos
        check (`total_equipos` > 0)
)
    engine = InnoDB;

create table estudiante
(
    id_usuario int                                      not null
        primary key,
    matricula  varchar(50)                              not null,
    carrera    varchar(100)                             not null,
    turno      enum ('Matutino', 'Vespertino', 'Mixto') null,
    constraint uk_estudiante_matricula
        unique (matricula),
    constraint fk_estudiante_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete cascade
)
    engine = InnoDB;

create table laboratorista
(
    id_usuario     int                                      not null
        primary key,
    turno          enum ('Matutino', 'Vespertino', 'Mixto') not null,
    piso_encargado varchar(50)                              not null,
    constraint fk_laboratorista_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete cascade
)
    engine = InnoDB;

create table externo
(
    id_usuario         int          not null
        primary key,
    institucion_origen varchar(150) not null,
    motivo_visita      text         null,
    constraint fk_externo_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete cascade
)
    engine = InnoDB;

create table horarios_clase
(
    id_horario     int auto_increment
        primary key,
    id_ciclo       int                                                                  not null,
    id_profesor    int                                                                  not null,
    id_laboratorio int                                                                  not null,
    carrera        varchar(100)                                                         not null,
    cuatrimestre   tinyint unsigned                                                     not null,
    grupo          varchar(10)                                                          not null,
    turno          enum ('Matutino', 'Vespertino')                                      not null,
    materia        varchar(150)                                                         not null,
    dia_semana     enum ('Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado') not null,
    hora_inicio    time                                                                 not null,
    hora_fin       time                                                                 not null,
    activo         tinyint(1) default 1                                                 not null,
    index idx_horario_laboratorio
        (id_ciclo, id_laboratorio, dia_semana, hora_inicio, hora_fin),
    index idx_horario_profesor
        (id_ciclo, id_profesor, dia_semana, hora_inicio, hora_fin),
    index idx_horario_grupo_academico
        (id_ciclo, carrera, cuatrimestre, grupo, turno, dia_semana, hora_inicio, hora_fin),
    constraint fk_horario_ciclo
        foreign key (id_ciclo) references ciclos_escolares (id_ciclo)
            on update cascade,
    constraint fk_horario_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on update cascade,
    constraint fk_horario_profesor
        foreign key (id_profesor) references usuario (id)
            on update cascade,
    constraint chk_horario_cuatrimestre
        check (`cuatrimestre` between 1 and 11),
    constraint chk_horario_horas
        check (`hora_fin` > `hora_inicio`)
)
    engine = InnoDB;

create table reservas
(
    id_reserva       int auto_increment
        primary key,
    id_usuario       int                                                                                              not null,
    id_laboratorio   int                                                                                              not null,
    actividad        varchar(150)                                                                                     not null,
    carrera          varchar(100)                                                                                     null,
    grado            varchar(20)                                                                                      null,
    grupo            varchar(20)                                                                                      null,
    turno            varchar(50)                                                                                      null,
    fecha            date                                                                                             not null,
    hora_inicio      time                                                                                             not null,
    hora_fin         time                                                                                             not null,
    cantidad_alumnos int unsigned                                                                                     not null,
    estado           enum ('Pendiente', 'Aprobada', 'Rechazada', 'Cancelada', 'Finalizada') default 'Pendiente'       not null,
    observaciones    text                                                                                             null,
    fecha_registro   timestamp                                                              default CURRENT_TIMESTAMP not null,
    constraint fk_reservas_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on update cascade,
    constraint fk_reservas_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade,
    constraint chk_reservas_horas
        check (`hora_fin` > `hora_inicio`),
    constraint chk_reservas_personas_positivas
        check (`cantidad_alumnos` > 0)
)
    engine = InnoDB;

create index idx_reservas_fecha_estado
    on reservas (fecha, estado);

create index idx_reservas_laboratorio_fecha_estado
    on reservas (id_laboratorio, fecha, estado);

create index idx_reservas_usuario_fecha
    on reservas (id_usuario, fecha);

create table bitacora
(
    id_bitacora         int auto_increment
        primary key,
    id_usuario          int                                   null,
    id_reserva          int                                   null,
    id_horario          int                                   null,
    id_laboratorio      int                                   null,
    fecha               date                                  not null,
    nombre_usuario      varchar(240)                          not null comment 'Fotografía histórica',
    rol_usuario         varchar(50)                           not null comment 'Fotografía histórica',
    carrera_dependencia varchar(100)                          not null,
    grado               varchar(20)                           null,
    grupo               varchar(20)                           null,
    laboratorio         varchar(50)                           not null comment 'Fotografía histórica del recurso usado',
    actividad_materia   varchar(150)                          not null,
    turno               varchar(50)                           not null,
    horario             varchar(50)                           not null,
    total_usuarios      int unsigned                          not null,
    observaciones       text                                  null,
    estado              varchar(50) default 'Registrado'      not null,
    fecha_registro      timestamp   default CURRENT_TIMESTAMP not null,
    constraint uk_bitacora_horario_fecha
        unique (id_horario, fecha),
    constraint fk_bitacora_horario
        foreign key (id_horario) references horarios_clase (id_horario)
            on update cascade on delete set null,
    constraint fk_bitacora_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on delete set null,
    constraint fk_bitacora_reserva
        foreign key (id_reserva) references reservas (id_reserva)
            on update cascade on delete set null,
    constraint fk_bitacora_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete set null,
    constraint chk_bitacora_personas_positivas
        check (`total_usuarios` > 0)
)
    engine = InnoDB;

create index idx_bitacora_fecha_registro
    on bitacora (fecha_registro);

create index idx_bitacora_usuario_fecha
    on bitacora (id_usuario, fecha);

create table inventario
(
    id_inventario        int auto_increment
        primary key,
    id_laboratorio       int                                                                                    not null,
    codigo               varchar(50)                                                                            not null,
    nombre_equipo        varchar(100)                                                                           not null,
    tipo_dispositivo     varchar(50)                                                                            not null,
    marca                varchar(80)                                                                            null,
    modelo               varchar(80)                                                                            null,
    no_serie             varchar(100)                                                                           null,
    estado               enum ('Disponible', 'En mantenimiento', 'Con falla', 'Baja') default 'Disponible'      not null,
    ultimo_mantenimiento date                                                                                   null,
    observaciones        text                                                                                   null,
    fecha_registro       timestamp                                                    default CURRENT_TIMESTAMP not null,
    constraint uk_inventario_codigo
        unique (codigo),
    constraint uk_inventario_serie
        unique (no_serie),
    constraint fk_inventario_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on update cascade,
    constraint chk_inventario_tipo
        check (`tipo_dispositivo` in ('Computadora', 'Monitor', 'Teclado', 'Mouse',
            'Proyector', 'Extensión', 'HDMI', 'Material peligroso', 'Batería',
            'Batería de UPS', 'Tóner', 'Residuo electrónico', 'Otro'))
)
    engine = InnoDB
    default character set = utf8mb4
    collate = utf8mb4_unicode_ci;

create index idx_inventario_laboratorio
    on inventario (id_laboratorio);

CREATE TABLE software_laboratorio
(
    id_software       INT AUTO_INCREMENT PRIMARY KEY,
    id_laboratorio    INT NOT NULL,
    nombre            VARCHAR(150) NOT NULL,
    version_instalada VARCHAR(80) NULL,
    version_objetivo  VARCHAR(80) NULL,
    uso_academico     VARCHAR(200) NOT NULL DEFAULT 'General',
    estado            ENUM ('Actualizado', 'Desactualizado',
                            'Pendiente de instalación', 'Pendiente de eliminación',
                            'Eliminado') NOT NULL DEFAULT 'Actualizado',
    fecha_revision    DATE NULL,
    observaciones     TEXT NULL,
    fecha_registro    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_software_laboratorio_nombre UNIQUE (id_laboratorio, nombre),
    CONSTRAINT fk_software_laboratorio_laboratorio
        FOREIGN KEY (id_laboratorio) REFERENCES laboratorios (id_laboratorio)
            ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_software_laboratorio (id_laboratorio),
    INDEX idx_software_estado (estado)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

create table reporte_fallas
(
    id_falla          int auto_increment
        primary key,
    id_usuario        int                                                                                  null,
    id_inventario     int                                                                                  null,
    id_reserva        int                                                                                  null,
    id_laboratorio    int                                                                                  not null,
    descripcion_falla text                                                                                 not null,
    prioridad         enum ('Baja', 'Media', 'Alta', 'Crítica')                  default 'Media'           not null,
    estado            enum ('Pendiente', 'En revisión', 'Atendida', 'Cancelada') default 'Pendiente'       not null,
    fecha_reporte     timestamp                                                  default CURRENT_TIMESTAMP not null,
    fecha_revision    date                                                                                 null,
    observaciones     text                                                                                 null,
    constraint fk_falla_inventario
        foreign key (id_inventario) references inventario (id_inventario)
            on update cascade on delete set null,
    constraint fk_falla_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on update cascade,
    constraint fk_falla_reserva
        foreign key (id_reserva) references reservas (id_reserva)
            on update cascade on delete set null,
    constraint fk_falla_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete set null
)
    engine = InnoDB;

create index idx_falla_estado_fecha
    on reporte_fallas (estado, fecha_reporte);

create index idx_falla_inventario
    on reporte_fallas (id_inventario);

create index idx_falla_laboratorio_estado
    on reporte_fallas (id_laboratorio, estado);

create table mantenimiento
(
    id_mantenimiento   int auto_increment
        primary key,
    id_inventario      int                                                                                  not null,
    id_falla           int                                                                                  null,
    tipo_mantenimiento varchar(100)                                                                         not null,
    fecha_programada   date                                                                                 not null,
    estado             enum ('Pendiente', 'En proceso', 'Realizado', 'Cancelado') default 'Pendiente'       not null,
    responsable        varchar(150)                                                                         not null,
    observaciones      text                                                                                 null,
    fecha_registro     timestamp                                                  default CURRENT_TIMESTAMP not null,
    constraint fk_mantenimiento_falla
        foreign key (id_falla) references reporte_fallas (id_falla)
            on update cascade on delete set null,
    constraint fk_mantenimiento_inventario
        foreign key (id_inventario) references inventario (id_inventario)
            on update cascade,
    constraint chk_mantenimiento_tipo
        check (`tipo_mantenimiento` in ('Preventivo', 'Correctivo',
            'Actualización de software', 'Actualización de hardware',
            'Disposición de material peligroso', 'Retiro de equipo obsoleto',
            'Limpieza', 'Otro'))
)
    engine = InnoDB;

create index idx_mantenimiento_estado_fecha
    on mantenimiento (estado, fecha_programada);

create index idx_mantenimiento_inventario
    on mantenimiento (id_inventario);

create table alertas
(
    id_alerta        int auto_increment
        primary key,
    id_reserva       int                                                                 null,
    id_falla         int                                                                 null,
    id_mantenimiento int                                                                 null,
    id_inventario    int                                                                 null,
    tipo             varchar(40)                                                         not null,
    referencia       varchar(50)                                                         not null,
    titulo           varchar(120)                                                        not null,
    detalle          varchar(500)                                                        not null,
    prioridad        enum ('Baja', 'Media', 'Alta', 'Crítica') default 'Media'           not null,
    estado           enum ('Nueva', 'Leída', 'Atendida')       default 'Nueva'           not null,
    fecha_creacion   timestamp                                 default CURRENT_TIMESTAMP not null,
    fecha_lectura    timestamp                                                           null,
    fecha_atencion   timestamp                                                           null,
    constraint uk_alerta_origen
        unique (tipo, referencia),
    constraint fk_alerta_falla
        foreign key (id_falla) references reporte_fallas (id_falla)
            on delete cascade,
    constraint fk_alerta_inventario
        foreign key (id_inventario) references inventario (id_inventario)
            on delete cascade,
    constraint fk_alerta_mantenimiento
        foreign key (id_mantenimiento) references mantenimiento (id_mantenimiento)
            on delete cascade,
    constraint fk_alerta_reserva
        foreign key (id_reserva) references reservas (id_reserva)
            on delete cascade,
    constraint chk_alerta_un_origen
        check (((((`id_reserva` is not null) + (`id_falla` is not null)) + (`id_mantenimiento` is not null)) +
                (`id_inventario` is not null)) = 1)
)
    engine = InnoDB
    default character set = utf8mb4
    collate = utf8mb4_unicode_ci;

create index idx_alerta_estado_prioridad
    on alertas (estado, prioridad);

-- ============================================================================
-- TRIGGERS
-- ============================================================================
DELIMITER //

create trigger trg_horario_bi_integridad
    before insert
    on horarios_clase
    for each row
BEGIN
  IF NOT EXISTS (SELECT 1 FROM usuario WHERE id=NEW.id_profesor AND rol='Profesor') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El usuario asignado al horario no es profesor';
  END IF;
  IF NEW.activo=1 AND EXISTS (
    SELECT 1 FROM horarios_clase h WHERE h.activo=1 AND h.id_ciclo=NEW.id_ciclo
      AND h.dia_semana=NEW.dia_semana
      AND (h.id_laboratorio=NEW.id_laboratorio OR h.id_profesor=NEW.id_profesor
           OR (h.carrera=NEW.carrera AND h.cuatrimestre=NEW.cuatrimestre
               AND h.grupo=NEW.grupo AND h.turno=NEW.turno))
      AND NEW.hora_inicio < h.hora_fin AND NEW.hora_fin > h.hora_inicio
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existe un horario activo traslapado';
  END IF;
END//

create trigger trg_horario_bu_integridad
    before update
    on horarios_clase
    for each row
BEGIN
  IF NOT EXISTS (SELECT 1 FROM usuario WHERE id=NEW.id_profesor AND rol='Profesor') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El usuario asignado al horario no es profesor';
  END IF;
  IF NEW.activo=1 AND EXISTS (
    SELECT 1 FROM horarios_clase h WHERE h.id_horario<>OLD.id_horario
      AND h.activo=1 AND h.id_ciclo=NEW.id_ciclo AND h.dia_semana=NEW.dia_semana
      AND (h.id_laboratorio=NEW.id_laboratorio OR h.id_profesor=NEW.id_profesor
           OR (h.carrera=NEW.carrera AND h.cuatrimestre=NEW.cuatrimestre
               AND h.grupo=NEW.grupo AND h.turno=NEW.turno))
      AND NEW.hora_inicio < h.hora_fin AND NEW.hora_fin > h.hora_inicio
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existe un horario activo traslapado';
  END IF;
END//

create trigger trg_reserva_bi_integridad
    before insert
    on reservas
    for each row
BEGIN
  DECLARE v_capacidad INT;
  DECLARE v_ocupacion INT DEFAULT 0;
  DECLARE v_rol VARCHAR(50);
  SELECT u.rol,
         CASE WHEN u.rol='Profesor' THEN l.capacidad_personas ELSE l.total_equipos END
    INTO v_rol,v_capacidad FROM laboratorios l JOIN usuario u ON u.id=NEW.id_usuario
   WHERE l.id_laboratorio=NEW.id_laboratorio FOR UPDATE;
  IF NEW.cantidad_alumnos>v_capacidad THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='La cantidad de usuarios excede la capacidad del laboratorio';
  END IF;
  IF NEW.estado IN ('Pendiente','Aprobada') THEN
    IF v_rol='Profesor' AND EXISTS (
      SELECT 1 FROM reservas r WHERE r.id_laboratorio=NEW.id_laboratorio
        AND r.fecha=NEW.fecha AND r.estado IN ('Pendiente','Aprobada')
        AND NEW.hora_inicio<r.hora_fin AND NEW.hora_fin>r.hora_inicio
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT='El laboratorio ya tiene ocupación activa en ese horario';
    ELSEIF v_rol<>'Profesor' AND EXISTS (
      SELECT 1 FROM reservas r JOIN usuario u ON u.id=r.id_usuario
       WHERE r.id_laboratorio=NEW.id_laboratorio AND r.fecha=NEW.fecha
         AND r.estado IN ('Pendiente','Aprobada') AND u.rol='Profesor'
         AND NEW.hora_inicio<r.hora_fin AND NEW.hora_fin>r.hora_inicio
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT='El laboratorio está reservado por un profesor en ese horario';
    ELSEIF v_rol<>'Profesor' THEN
      SELECT COALESCE(SUM(r.cantidad_alumnos),0) INTO v_ocupacion
        FROM reservas r JOIN usuario u ON u.id=r.id_usuario
       WHERE r.id_laboratorio=NEW.id_laboratorio AND r.fecha=NEW.fecha
         AND r.estado IN ('Pendiente','Aprobada') AND u.rol<>'Profesor'
         AND NEW.hora_inicio<r.hora_fin AND NEW.hora_fin>r.hora_inicio;
      IF v_ocupacion+NEW.cantidad_alumnos>v_capacidad THEN
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT='No hay suficientes equipos disponibles en ese horario';
      END IF;
    END IF;
  END IF;
END//

create trigger trg_reserva_bu_integridad
    before update
    on reservas
    for each row
BEGIN
  DECLARE v_capacidad INT;
  DECLARE v_ocupacion INT DEFAULT 0;
  DECLARE v_rol VARCHAR(50);
  SELECT u.rol,
         CASE WHEN u.rol='Profesor' THEN l.capacidad_personas ELSE l.total_equipos END
    INTO v_rol,v_capacidad FROM laboratorios l JOIN usuario u ON u.id=NEW.id_usuario
   WHERE l.id_laboratorio=NEW.id_laboratorio FOR UPDATE;
  IF NEW.cantidad_alumnos>v_capacidad THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='La cantidad de usuarios excede la capacidad del laboratorio';
  END IF;
  IF NEW.estado IN ('Pendiente','Aprobada') THEN
    IF v_rol='Profesor' AND EXISTS (
      SELECT 1 FROM reservas r WHERE r.id_reserva<>OLD.id_reserva
        AND r.id_laboratorio=NEW.id_laboratorio AND r.fecha=NEW.fecha
        AND r.estado IN ('Pendiente','Aprobada')
        AND NEW.hora_inicio<r.hora_fin AND NEW.hora_fin>r.hora_inicio
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT='El laboratorio ya tiene ocupación activa en ese horario';
    ELSEIF v_rol<>'Profesor' AND EXISTS (
      SELECT 1 FROM reservas r JOIN usuario u ON u.id=r.id_usuario
       WHERE r.id_reserva<>OLD.id_reserva
         AND r.id_laboratorio=NEW.id_laboratorio AND r.fecha=NEW.fecha
         AND r.estado IN ('Pendiente','Aprobada') AND u.rol='Profesor'
         AND NEW.hora_inicio<r.hora_fin AND NEW.hora_fin>r.hora_inicio
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT='El laboratorio está reservado por un profesor en ese horario';
    ELSEIF v_rol<>'Profesor' THEN
      SELECT COALESCE(SUM(r.cantidad_alumnos),0) INTO v_ocupacion
        FROM reservas r JOIN usuario u ON u.id=r.id_usuario
       WHERE r.id_reserva<>OLD.id_reserva
         AND r.id_laboratorio=NEW.id_laboratorio AND r.fecha=NEW.fecha
         AND r.estado IN ('Pendiente','Aprobada') AND u.rol<>'Profesor'
         AND NEW.hora_inicio<r.hora_fin AND NEW.hora_fin>r.hora_inicio;
      IF v_ocupacion+NEW.cantidad_alumnos>v_capacidad THEN
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT='No hay suficientes equipos disponibles en ese horario';
      END IF;
    END IF;
  END IF;
END//

create trigger trg_bitacora_bi_laboratorio
    before insert
    on bitacora
    for each row
BEGIN
  IF NEW.id_laboratorio IS NULL THEN
    SET NEW.id_laboratorio=(SELECT id_laboratorio FROM laboratorios
      WHERE nombre=NEW.laboratorio LIMIT 1);
  END IF;
  IF NEW.id_laboratorio IS NULL OR NOT EXISTS (
    SELECT 1 FROM laboratorios WHERE id_laboratorio=NEW.id_laboratorio
      AND nombre=NEW.laboratorio
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='El nombre y el ID del laboratorio de bitácora no corresponden';
  END IF;
END//

create trigger trg_bitacora_bu_laboratorio
    before update
    on bitacora
    for each row
BEGIN
  IF NEW.id_laboratorio IS NULL THEN
    SET NEW.id_laboratorio=(SELECT id_laboratorio FROM laboratorios
      WHERE nombre=NEW.laboratorio LIMIT 1);
  END IF;
  IF NEW.id_laboratorio IS NULL OR NOT EXISTS (
    SELECT 1 FROM laboratorios WHERE id_laboratorio=NEW.id_laboratorio
      AND nombre=NEW.laboratorio
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='El nombre y el ID del laboratorio de bitácora no corresponden';
  END IF;
END//

create trigger trg_falla_bi_laboratorio
    before insert
    on reporte_fallas
    for each row
BEGIN
  IF NEW.id_inventario IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM inventario WHERE id_inventario=NEW.id_inventario
      AND id_laboratorio=NEW.id_laboratorio
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El equipo reportado no pertenece al laboratorio indicado';
  END IF;
  IF NEW.id_reserva IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM reservas WHERE id_reserva=NEW.id_reserva
      AND id_laboratorio=NEW.id_laboratorio
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La reserva reportada no pertenece al laboratorio indicado';
  END IF;
END//

create trigger trg_falla_bu_laboratorio
    before update
    on reporte_fallas
    for each row
BEGIN
  IF NEW.id_inventario IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM inventario WHERE id_inventario=NEW.id_inventario
      AND id_laboratorio=NEW.id_laboratorio
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El equipo reportado no pertenece al laboratorio indicado';
  END IF;
  IF NEW.id_reserva IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM reservas WHERE id_reserva=NEW.id_reserva
      AND id_laboratorio=NEW.id_laboratorio
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La reserva reportada no pertenece al laboratorio indicado';
  END IF;
END//

DELIMITER ;

-- ============================================================================
-- DATOS DE DEMOSTRACIÓN
-- ============================================================================
-- Todas las identidades son ficticias. La contraseña documentada se almacena
-- mediante el mismo SHA-256 hexadecimal usado por la aplicación.
INSERT INTO laboratorios (nombre, total_equipos, capacidad_personas, estado)
VALUES ('LAB-DEMO-1', 24, 32, 'Disponible'),
       ('LAB-DEMO-2', 20, 28, 'Disponible')
ON DUPLICATE KEY UPDATE total_equipos = VALUES(total_equipos),
    capacidad_personas = VALUES(capacidad_personas), estado = VALUES(estado);

INSERT INTO ciclos_escolares (nombre, fecha_inicio, fecha_fin, activo)
VALUES ('DEMO-2026-B', '2026-08-01', '2026-12-15', 1)
ON DUPLICATE KEY UPDATE fecha_inicio = VALUES(fecha_inicio),
    fecha_fin = VALUES(fecha_fin), activo = VALUES(activo);

INSERT INTO usuario (nombre, apellido_p, apellido_m, rol, correo, password)
VALUES ('Alex', 'Demo', NULL, 'Laboratorista', 'laboratorista.demo@labsync.example',
        'cdd0891e7b6f5d06af998dd0bb8e31771c5ee9094b900c7ad67df56bbbf9978c'),
       ('Pat', 'Docente', NULL, 'Profesor', 'profesor.demo@labsync.example',
        'cdd0891e7b6f5d06af998dd0bb8e31771c5ee9094b900c7ad67df56bbbf9978c'),
       ('Sam', 'Estudiante', NULL, 'Estudiante', 'estudiante.demo@labsync.example',
        'cdd0891e7b6f5d06af998dd0bb8e31771c5ee9094b900c7ad67df56bbbf9978c')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), apellido_p = VALUES(apellido_p),
    apellido_m = VALUES(apellido_m), rol = VALUES(rol), password = VALUES(password);

INSERT INTO laboratorista (id_usuario, turno, piso_encargado)
SELECT id, 'Matutino', 'Piso de demostración' FROM usuario
WHERE correo = 'laboratorista.demo@labsync.example'
ON DUPLICATE KEY UPDATE turno = VALUES(turno), piso_encargado = VALUES(piso_encargado);

INSERT INTO estudiante (id_usuario, matricula, carrera, turno)
SELECT id, 'DEMO-0001', 'Ingeniería de demostración', 'Matutino' FROM usuario
WHERE correo = 'estudiante.demo@labsync.example'
ON DUPLICATE KEY UPDATE matricula = VALUES(matricula), carrera = VALUES(carrera),
    turno = VALUES(turno);

INSERT INTO horarios_clase (id_ciclo, id_profesor, id_laboratorio, carrera,
                            cuatrimestre, grupo, turno, materia, dia_semana,
                            hora_inicio, hora_fin, activo)
SELECT c.id_ciclo, u.id, l.id_laboratorio, 'Ingeniería de demostración',
       1, 'A', 'Matutino', 'Fundamentos de laboratorio', 'Lunes',
       '09:00:00', '10:00:00', 1
FROM ciclos_escolares c, usuario u, laboratorios l
WHERE c.nombre = 'DEMO-2026-B'
  AND u.correo = 'profesor.demo@labsync.example'
  AND l.nombre = 'LAB-DEMO-1'
  AND NOT EXISTS (
      SELECT 1 FROM horarios_clase h
      WHERE h.id_ciclo = c.id_ciclo AND h.id_profesor = u.id
        AND h.id_laboratorio = l.id_laboratorio
        AND h.dia_semana = 'Lunes' AND h.hora_inicio = '09:00:00'
        AND h.hora_fin = '10:00:00'
  );

INSERT INTO inventario (id_laboratorio, codigo, nombre_equipo, tipo_dispositivo,
                        marca, modelo, no_serie, estado, ultimo_mantenimiento,
                        observaciones)
SELECT id_laboratorio, 'DEMO-PC-001', 'Equipo de demostración', 'Computadora',
       'Marca ficticia', 'Modelo ficticio', 'SERIE-DEMO-001', 'Disponible',
       '2026-07-15', 'Registro ficticio para demostración'
FROM laboratorios WHERE nombre = 'LAB-DEMO-1'
ON DUPLICATE KEY UPDATE nombre_equipo = VALUES(nombre_equipo),
    tipo_dispositivo = VALUES(tipo_dispositivo), estado = VALUES(estado),
    ultimo_mantenimiento = VALUES(ultimo_mantenimiento),
    observaciones = VALUES(observaciones);

INSERT INTO software_laboratorio (id_laboratorio, nombre, version_instalada,
                                  version_objetivo, uso_academico, estado,
                                  fecha_revision, observaciones)
SELECT id_laboratorio, 'Suite Demo', '1.0', '1.0', 'Prácticas ficticias',
       'Actualizado', '2026-08-01', 'Software ficticio de demostración'
FROM laboratorios WHERE nombre = 'LAB-DEMO-1'
ON DUPLICATE KEY UPDATE version_instalada = VALUES(version_instalada),
    version_objetivo = VALUES(version_objetivo), uso_academico = VALUES(uso_academico),
    estado = VALUES(estado), fecha_revision = VALUES(fecha_revision),
    observaciones = VALUES(observaciones);

SET @LABSYNC_DEMO_PROFESOR_ID = (
    SELECT id FROM usuario WHERE correo = 'profesor.demo@labsync.example'
);
SET @LABSYNC_DEMO_LAB_ID = (
    SELECT id_laboratorio FROM laboratorios WHERE nombre = 'LAB-DEMO-1'
);
INSERT INTO reservas (id_usuario, id_laboratorio, actividad, carrera, grado,
                      grupo, turno, fecha, hora_inicio, hora_fin,
                      cantidad_alumnos, estado, observaciones)
SELECT @LABSYNC_DEMO_PROFESOR_ID, @LABSYNC_DEMO_LAB_ID,
       'Presentación de LabSync', 'Ingeniería de demostración', '1', 'A',
       'Matutino', '2026-10-06', '11:00:00', '12:00:00', 12, 'Aprobada',
       'Reserva ficticia de demostración'
WHERE NOT EXISTS (
    SELECT 1 FROM reservas r
    WHERE r.id_usuario = @LABSYNC_DEMO_PROFESOR_ID
      AND r.id_laboratorio = @LABSYNC_DEMO_LAB_ID
      AND r.actividad = 'Presentación de LabSync' AND r.fecha = '2026-10-06'
      AND r.hora_inicio = '11:00:00' AND r.hora_fin = '12:00:00'
);

INSERT INTO reporte_fallas (id_usuario, id_inventario, id_reserva,
                            id_laboratorio, descripcion_falla, prioridad,
                            estado, fecha_revision, observaciones)
SELECT e.id, i.id_inventario, NULL, i.id_laboratorio,
       'Falla ficticia de periférico para demostración', 'Baja', 'En revisión',
       '2026-08-05', 'Caso ficticio; no corresponde a equipo real'
FROM usuario e JOIN inventario i ON i.codigo = 'DEMO-PC-001'
WHERE e.correo = 'estudiante.demo@labsync.example'
  AND NOT EXISTS (
      SELECT 1 FROM reporte_fallas f
      WHERE f.id_inventario = i.id_inventario
        AND f.descripcion_falla = 'Falla ficticia de periférico para demostración'
  );

INSERT INTO mantenimiento (id_inventario, id_falla, tipo_mantenimiento,
                           fecha_programada, estado, responsable, observaciones)
SELECT i.id_inventario, f.id_falla, 'Correctivo', '2026-10-08', 'Pendiente',
       'Responsable Demo', 'Mantenimiento ficticio de demostración'
FROM inventario i JOIN reporte_fallas f ON f.id_inventario = i.id_inventario
WHERE i.codigo = 'DEMO-PC-001'
  AND f.descripcion_falla = 'Falla ficticia de periférico para demostración'
  AND NOT EXISTS (
      SELECT 1 FROM mantenimiento m
      WHERE m.id_inventario = i.id_inventario AND m.id_falla = f.id_falla
        AND m.tipo_mantenimiento = 'Correctivo'
        AND m.fecha_programada = '2026-10-08'
  );

-- ============================================================================
-- VALIDACIÓN FINAL Y RESTAURACIÓN DE CONFIGURACIONES DE SESIÓN
-- ============================================================================
SELECT COUNT(*) AS total_tablas
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE';

SELECT COUNT(*) AS total_columnas
FROM information_schema.columns
WHERE table_schema = DATABASE();

SET SQL_MODE = @LABSYNC_OLD_SQL_MODE;
SET UNIQUE_CHECKS = @LABSYNC_OLD_UNIQUE_CHECKS;
SET FOREIGN_KEY_CHECKS = @LABSYNC_OLD_FOREIGN_KEY_CHECKS;
SET CHARACTER_SET_CLIENT = @LABSYNC_OLD_CHARACTER_SET_CLIENT;
SET CHARACTER_SET_RESULTS = @LABSYNC_OLD_CHARACTER_SET_RESULTS;
SET COLLATION_CONNECTION = @LABSYNC_OLD_COLLATION_CONNECTION;
