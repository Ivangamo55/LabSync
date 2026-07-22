-- Horarios regulares UTJ-CCD. Compatible con la base LabSync existente (MySQL 8+).
-- Fuente del plan: https://www.utj.edu.mx/oferta-academica/licenciatura-ingenieria/32761-2/
-- No crea laboratorios: todos los horarios referencian el catálogo existente.

START TRANSACTION;

ALTER TABLE laboratorios ENGINE = InnoDB;
ALTER TABLE bitacora ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS ciclos_escolares (
  id_ciclo INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(50) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE NOT NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_ciclo),
  UNIQUE KEY uk_ciclo_nombre (nombre),
  CONSTRAINT chk_ciclo_fechas CHECK (fecha_fin >= fecha_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS trayectorias (
  id_trayectoria INT NOT NULL AUTO_INCREMENT,
  codigo VARCHAR(20) NOT NULL,
  nombre VARCHAR(150) NOT NULL,
  nivel ENUM('TSU','Ingeniería') NOT NULL,
  activa TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_trayectoria),
  UNIQUE KEY uk_trayectoria_codigo (codigo),
  UNIQUE KEY uk_trayectoria_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS materias (
  id_materia INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(150) NOT NULL,
  PRIMARY KEY (id_materia),
  UNIQUE KEY uk_materia_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plan_materias (
  id_plan_materia INT NOT NULL AUTO_INCREMENT,
  id_trayectoria INT NOT NULL,
  cuatrimestre TINYINT UNSIGNED NOT NULL,
  id_materia INT NOT NULL,
  orden TINYINT UNSIGNED NOT NULL,
  PRIMARY KEY (id_plan_materia),
  UNIQUE KEY uk_plan_materia (id_trayectoria, cuatrimestre, id_materia),
  UNIQUE KEY uk_plan_orden (id_trayectoria, cuatrimestre, orden),
  CONSTRAINT fk_plan_trayectoria FOREIGN KEY (id_trayectoria)
    REFERENCES trayectorias (id_trayectoria),
  CONSTRAINT fk_plan_materia FOREIGN KEY (id_materia)
    REFERENCES materias (id_materia),
  CONSTRAINT chk_plan_cuatrimestre CHECK (cuatrimestre BETWEEN 1 AND 11)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS grupos (
  id_grupo INT NOT NULL AUTO_INCREMENT,
  id_trayectoria INT NOT NULL,
  cuatrimestre TINYINT UNSIGNED NOT NULL,
  letra VARCHAR(10) NOT NULL,
  turno ENUM('Matutino','Vespertino') NOT NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_grupo),
  UNIQUE KEY uk_grupo (id_trayectoria, cuatrimestre, letra, turno),
  CONSTRAINT fk_grupo_trayectoria FOREIGN KEY (id_trayectoria)
    REFERENCES trayectorias (id_trayectoria),
  CONSTRAINT chk_grupo_cuatrimestre CHECK (cuatrimestre BETWEEN 1 AND 11)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS horarios_clase (
  id_horario INT NOT NULL AUTO_INCREMENT,
  id_ciclo INT NOT NULL,
  id_grupo INT NOT NULL,
  id_plan_materia INT NOT NULL,
  id_profesor INT NOT NULL,
  id_laboratorio INT NOT NULL,
  dia_semana ENUM('Lunes','Martes','Miércoles','Jueves','Viernes','Sábado') NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin TIME NOT NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_horario),
  UNIQUE KEY uk_horario_clase (id_ciclo, id_grupo, id_plan_materia, dia_semana, hora_inicio),
  KEY idx_horario_laboratorio (id_ciclo, id_laboratorio, dia_semana, hora_inicio, hora_fin),
  KEY idx_horario_profesor (id_ciclo, id_profesor, dia_semana, hora_inicio, hora_fin),
  CONSTRAINT fk_horario_ciclo FOREIGN KEY (id_ciclo) REFERENCES ciclos_escolares (id_ciclo),
  CONSTRAINT fk_horario_grupo FOREIGN KEY (id_grupo) REFERENCES grupos (id_grupo),
  CONSTRAINT fk_horario_plan FOREIGN KEY (id_plan_materia) REFERENCES plan_materias (id_plan_materia),
  CONSTRAINT fk_horario_profesor FOREIGN KEY (id_profesor) REFERENCES usuario (id),
  CONSTRAINT fk_horario_laboratorio FOREIGN KEY (id_laboratorio) REFERENCES laboratorios (id_laboratorio),
  CONSTRAINT chk_horario_horas CHECK (hora_fin > hora_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @existe_col_bitacora_horario = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='bitacora' AND column_name='id_horario');
SET @sql_col_bitacora_horario = IF(@existe_col_bitacora_horario=0,
 'ALTER TABLE bitacora ADD COLUMN id_horario INT NULL AFTER id_bitacora', 'SELECT 1');
PREPARE stmt FROM @sql_col_bitacora_horario; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @existe_col_bitacora_reserva = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='bitacora' AND column_name='id_reserva');
SET @sql_col_bitacora_reserva = IF(@existe_col_bitacora_reserva=0,
 'ALTER TABLE bitacora ADD COLUMN id_reserva INT NULL AFTER id_horario', 'SELECT 1');
PREPARE stmt FROM @sql_col_bitacora_reserva; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Los índices conservan la historia y evitan registrar dos veces una clase en la misma fecha.
SET @existe_uk_bitacora_horario = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'bitacora'
    AND index_name = 'uk_bitacora_horario_fecha'
);
SET @sql_uk_bitacora_horario = IF(@existe_uk_bitacora_horario = 0,
  'ALTER TABLE bitacora ADD UNIQUE KEY uk_bitacora_horario_fecha (id_horario, fecha)', 'SELECT 1');
PREPARE stmt FROM @sql_uk_bitacora_horario; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @existe_fk_bitacora_horario = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'bitacora'
    AND constraint_name = 'fk_bitacora_horario'
);
SET @sql_fk_bitacora_horario = IF(@existe_fk_bitacora_horario = 0,
  'ALTER TABLE bitacora ADD CONSTRAINT fk_bitacora_horario FOREIGN KEY (id_horario) REFERENCES horarios_clase (id_horario) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql_fk_bitacora_horario; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO ciclos_escolares (nombre, fecha_inicio, fecha_fin, activo)
VALUES ('MAYO - AGOSTO 2026', '2026-05-04', '2026-08-21', 1)
ON DUPLICATE KEY UPDATE fecha_inicio=VALUES(fecha_inicio), fecha_fin=VALUES(fecha_fin);

INSERT INTO trayectorias (codigo, nombre, nivel) VALUES
('DSM', 'TSU Desarrollo de Software Multiplataforma', 'TSU'),
('EVND', 'TSU Entornos Virtuales y Negocios Digitales', 'TSU'),
('ITI-ID', 'ING en Tecnologías de la Información e Innovación Digital', 'Ingeniería')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), nivel=VALUES(nivel), activa=1;

INSERT IGNORE INTO materias (nombre) VALUES
('Fundamentos matemáticos'),('Fundamentos de programación'),('Física'),
('Desarrollo humano y valores'),('Inglés I'),('Comunicación y habilidades digitales'),
('Fundamentos de redes'),('Inglés II'),('Habilidades socioemocionales y manejo de conflictos'),
('Cálculo diferencial'),('Conmutación y enrutamiento de redes'),('Probabilidad y estadística'),
('Programación estructurada'),('Sistemas operativos'),('Inglés III'),
('Desarrollo del pensamiento y toma de decisiones'),('Cálculo integral'),
('Tópicos de calidad para el diseño de software'),('Bases de datos'),
('Programación orientada a objetos'),('Proyecto integrador I'),('Inglés IV'),
('Ética profesional'),('Cálculo de varias variables'),('Aplicaciones web'),
('Estructura de datos'),('Desarrollo de aplicaciones móviles'),('Análisis y diseño de software'),
('Inglés V'),('Liderazgo de equipos de alto desempeño'),('Ecuaciones diferenciales'),
('Aplicaciones web orientadas a servicios'),('Base de datos avanzadas'),
('Estándares y métricas para el desarrollo de software'),('Proyecto integrador II'),
('Modelado y animación digital'),('Diseño digital y producción audiovisual'),
('Mercadotecnia digital'),('Aplicaciones para realidad aumentada'),
('Aplicaciones para realidad virtual'),('Frameworks para desarrollo de web'),
('Inglés VI'),('Formulación de proyectos de tecnología'),
('Fundamentos de inteligencia artificial'),('Administración de servidores'),('Optativa I'),
('Electrónica digital'),('Gestión de proyectos de tecnología'),
('Ética y legislación en tecnologías de la información'),('Tecnologías disruptivas'),
('Habilidades gerenciales'),('Informática forense'),('Inglés VII'),('Internet de las cosas'),
('Evaluación de proyectos de tecnología'),('Programación para inteligencia artificial'),
('Optativa II'),('Inglés VIII'),('Ciencia de datos'),('Seguridad informática'),
('Optativa III'),('Proyecto integrador III');

-- Relación exacta trayectoria/cuatrimestre. Sexto y décimo primero se omiten por estadía.
DROP TEMPORARY TABLE IF EXISTS tmp_plan_ccd;
CREATE TEMPORARY TABLE tmp_plan_ccd (
  trayectoria VARCHAR(20), cuatrimestre TINYINT, orden TINYINT, materia VARCHAR(150)
);
INSERT INTO tmp_plan_ccd VALUES
('COMUN',1,1,'Fundamentos matemáticos'),('COMUN',1,2,'Fundamentos de programación'),('COMUN',1,3,'Física'),('COMUN',1,4,'Desarrollo humano y valores'),('COMUN',1,5,'Inglés I'),('COMUN',1,6,'Comunicación y habilidades digitales'),('COMUN',1,7,'Fundamentos de redes'),
('COMUN',2,1,'Inglés II'),('COMUN',2,2,'Habilidades socioemocionales y manejo de conflictos'),('COMUN',2,3,'Cálculo diferencial'),('COMUN',2,4,'Conmutación y enrutamiento de redes'),('COMUN',2,5,'Probabilidad y estadística'),('COMUN',2,6,'Programación estructurada'),('COMUN',2,7,'Sistemas operativos'),
('COMUN',3,1,'Inglés III'),('COMUN',3,2,'Desarrollo del pensamiento y toma de decisiones'),('COMUN',3,3,'Cálculo integral'),('COMUN',3,4,'Tópicos de calidad para el diseño de software'),('COMUN',3,5,'Bases de datos'),('COMUN',3,6,'Programación orientada a objetos'),('COMUN',3,7,'Proyecto integrador I'),
('DSM',4,1,'Inglés IV'),('DSM',4,2,'Ética profesional'),('DSM',4,3,'Cálculo de varias variables'),('DSM',4,4,'Aplicaciones web'),('DSM',4,5,'Estructura de datos'),('DSM',4,6,'Desarrollo de aplicaciones móviles'),('DSM',4,7,'Análisis y diseño de software'),
('DSM',5,1,'Inglés V'),('DSM',5,2,'Liderazgo de equipos de alto desempeño'),('DSM',5,3,'Ecuaciones diferenciales'),('DSM',5,4,'Aplicaciones web orientadas a servicios'),('DSM',5,5,'Base de datos avanzadas'),('DSM',5,6,'Estándares y métricas para el desarrollo de software'),('DSM',5,7,'Proyecto integrador II'),
('EVND',4,1,'Inglés IV'),('EVND',4,2,'Ética profesional'),('EVND',4,3,'Cálculo de varias variables'),('EVND',4,4,'Modelado y animación digital'),('EVND',4,5,'Diseño digital y producción audiovisual'),('EVND',4,6,'Aplicaciones web'),('EVND',4,7,'Mercadotecnia digital'),
('EVND',5,1,'Inglés V'),('EVND',5,2,'Liderazgo de equipos de alto desempeño'),('EVND',5,3,'Ecuaciones diferenciales'),('EVND',5,4,'Aplicaciones para realidad aumentada'),('EVND',5,5,'Aplicaciones para realidad virtual'),('EVND',5,6,'Frameworks para desarrollo de web'),('EVND',5,7,'Proyecto integrador II'),
('ITI-ID',7,1,'Inglés VI'),('ITI-ID',7,2,'Formulación de proyectos de tecnología'),('ITI-ID',7,3,'Fundamentos de inteligencia artificial'),('ITI-ID',7,4,'Administración de servidores'),('ITI-ID',7,5,'Optativa I'),
('ITI-ID',8,1,'Electrónica digital'),('ITI-ID',8,2,'Gestión de proyectos de tecnología'),('ITI-ID',8,3,'Ética y legislación en tecnologías de la información'),('ITI-ID',8,4,'Tecnologías disruptivas'),('ITI-ID',8,5,'Habilidades gerenciales'),('ITI-ID',8,6,'Informática forense'),
('ITI-ID',9,1,'Inglés VII'),('ITI-ID',9,2,'Internet de las cosas'),('ITI-ID',9,3,'Evaluación de proyectos de tecnología'),('ITI-ID',9,4,'Programación para inteligencia artificial'),('ITI-ID',9,5,'Optativa II'),
('ITI-ID',10,1,'Inglés VIII'),('ITI-ID',10,2,'Ciencia de datos'),('ITI-ID',10,3,'Seguridad informática'),('ITI-ID',10,4,'Optativa III'),('ITI-ID',10,5,'Proyecto integrador III');

INSERT IGNORE INTO plan_materias (id_trayectoria, cuatrimestre, id_materia, orden)
SELECT t.id_trayectoria, p.cuatrimestre, m.id_materia, p.orden
FROM tmp_plan_ccd p
JOIN trayectorias t ON t.codigo IN ('DSM','EVND') AND p.trayectoria='COMUN'
JOIN materias m ON m.nombre=p.materia;

INSERT IGNORE INTO plan_materias (id_trayectoria, cuatrimestre, id_materia, orden)
SELECT t.id_trayectoria, p.cuatrimestre, m.id_materia, p.orden
FROM tmp_plan_ccd p JOIN trayectorias t ON t.codigo=p.trayectoria
JOIN materias m ON m.nombre=p.materia
WHERE p.trayectoria<>'COMUN';

-- El correo es la identidad de acceso y debe ser único antes de sembrar usuarios.
SET @existe_uk_usuario_correo = (SELECT COUNT(*) FROM information_schema.statistics
 WHERE table_schema=DATABASE() AND table_name='usuario' AND index_name='uk_usuario_correo');
SET @sql_uk_usuario_correo = IF(@existe_uk_usuario_correo=0,
 'ALTER TABLE usuario ADD UNIQUE KEY uk_usuario_correo (correo)', 'SELECT 1');
PREPARE stmt FROM @sql_uk_usuario_correo; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Cuentas ficticias. La contraseña usa el SHA-256 del sistema de autenticación existente.
INSERT INTO usuario (nombre, apellido_p, apellido_m, rol, correo, password) VALUES
('Alicia','Torres','Núñez','Profesor','prueba.dsm1@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Bruno','López','Vega','Profesor','prueba.dsm2@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Carla','Méndez','Ríos','Profesor','prueba.dsm3@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Diego','Santos','Lara','Profesor','prueba.dsm4@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Elena','Ruiz','Campos','Profesor','prueba.dsm5@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Fabio','Ortega','Cruz','Profesor','prueba.evnd1@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Gabriela','Pérez','Solís','Profesor','prueba.evnd2@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Hugo','Navarro','Gil','Profesor','prueba.evnd3@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Irene','Castro','León','Profesor','prueba.evnd4@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Jorge','Flores','Mora','Profesor','prueba.evnd5@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Karla','Reyes','Díaz','Profesor','prueba.iti7@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Leonardo','Silva','Paz','Profesor','prueba.iti8@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Marina','Vargas','Ibarra','Profesor','prueba.iti9@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80'),
('Nicolás','Ramos','Peña','Profesor','prueba.iti10@utj.edu.mx','5fe2df120a334cb453a307ec3f3dc4c5dcf326e83fcb61ed4a0c3a95d28c0e80')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), apellido_p=VALUES(apellido_p),
  apellido_m=VALUES(apellido_m), rol='Profesor', password=VALUES(password);

DROP TEMPORARY TABLE IF EXISTS tmp_grupos_ccd;
CREATE TEMPORARY TABLE tmp_grupos_ccd (
 codigo VARCHAR(20), cuatrimestre TINYINT, letra VARCHAR(10), turno VARCHAR(15),
 correo VARCHAR(75), laboratorio VARCHAR(20)
);
INSERT INTO tmp_grupos_ccd VALUES
('DSM',1,'A','Matutino','prueba.dsm1@utj.edu.mx','M-11'),('DSM',2,'A','Matutino','prueba.dsm2@utj.edu.mx','M-12'),
('DSM',3,'A','Matutino','prueba.dsm3@utj.edu.mx','M-14'),('DSM',4,'A','Matutino','prueba.dsm4@utj.edu.mx','M-02'),
('DSM',5,'A','Matutino','prueba.dsm5@utj.edu.mx','M-05'),('EVND',1,'A','Matutino','prueba.evnd1@utj.edu.mx','5-06'),
('EVND',2,'A','Matutino','prueba.evnd2@utj.edu.mx','5-03'),('EVND',3,'A','Matutino','prueba.evnd3@utj.edu.mx','M-13'),
('EVND',4,'A','Matutino','prueba.evnd4@utj.edu.mx','PB-07'),('EVND',5,'B','Vespertino','prueba.evnd5@utj.edu.mx','M-11'),
('ITI-ID',7,'B','Vespertino','prueba.iti7@utj.edu.mx','M-12'),('ITI-ID',8,'B','Vespertino','prueba.iti8@utj.edu.mx','M-14'),
('ITI-ID',9,'B','Vespertino','prueba.iti9@utj.edu.mx','M-02'),('ITI-ID',10,'B','Vespertino','prueba.iti10@utj.edu.mx','M-05');

INSERT IGNORE INTO grupos (id_trayectoria, cuatrimestre, letra, turno)
SELECT t.id_trayectoria, g.cuatrimestre, g.letra, g.turno
FROM tmp_grupos_ccd g JOIN trayectorias t ON t.codigo=g.codigo;

INSERT IGNORE INTO horarios_clase
 (id_ciclo,id_grupo,id_plan_materia,id_profesor,id_laboratorio,dia_semana,hora_inicio,hora_fin,activo)
SELECT c.id_ciclo, gr.id_grupo, pm.id_plan_materia, u.id, l.id_laboratorio,
 CASE pm.orden WHEN 1 THEN 'Lunes' WHEN 2 THEN 'Martes' WHEN 3 THEN 'Miércoles'
   WHEN 4 THEN 'Jueves' WHEN 5 THEN 'Viernes' WHEN 6 THEN 'Lunes' ELSE 'Martes' END,
 CASE WHEN g.turno='Matutino' THEN IF(pm.orden<=5,'07:00:00','09:10:00')
      ELSE IF(pm.orden<=5,'15:00:00','18:00:00') END,
 CASE WHEN g.turno='Matutino' THEN IF(pm.orden<=5,'08:40:00','10:50:00')
      ELSE IF(pm.orden<=5,'16:40:00','19:40:00') END, 1
FROM tmp_grupos_ccd g
JOIN trayectorias t ON t.codigo=g.codigo
JOIN grupos gr ON gr.id_trayectoria=t.id_trayectoria AND gr.cuatrimestre=g.cuatrimestre
 AND gr.letra=g.letra AND gr.turno=g.turno
JOIN plan_materias pm ON pm.id_trayectoria=t.id_trayectoria AND pm.cuatrimestre=g.cuatrimestre
JOIN materias mat ON mat.id_materia=pm.id_materia
JOIN usuario u ON u.correo COLLATE utf8mb4_unicode_ci=g.correo
JOIN laboratorios l ON l.nombre COLLATE utf8mb4_unicode_ci=g.laboratorio
JOIN ciclos_escolares c ON c.nombre='MAYO - AGOSTO 2026'
WHERE mat.nombre NOT IN (
 'Fundamentos matemáticos','Física','Desarrollo humano y valores',
 'Inglés I','Inglés II','Inglés III','Inglés IV','Inglés V','Inglés VI','Inglés VII','Inglés VIII',
 'Habilidades socioemocionales y manejo de conflictos',
 'Desarrollo del pensamiento y toma de decisiones',
 'Ética profesional','Ética y legislación en tecnologías de la información',
 'Liderazgo de equipos de alto desempeño','Habilidades gerenciales',
 'Cálculo diferencial','Cálculo integral','Cálculo de varias variables',
 'Probabilidad y estadística','Ecuaciones diferenciales'
);

-- Si la migración ya se ejecutó con datos anteriores, elimina únicamente sus
-- asignaciones regulares; las materias permanecen intactas en el plan oficial.
DELETE h FROM horarios_clase h
JOIN plan_materias pm ON pm.id_plan_materia=h.id_plan_materia
JOIN materias m ON m.id_materia=pm.id_materia
JOIN ciclos_escolares c ON c.id_ciclo=h.id_ciclo
WHERE c.nombre='MAYO - AGOSTO 2026' AND m.nombre IN (
 'Fundamentos matemáticos','Física','Desarrollo humano y valores',
 'Inglés I','Inglés II','Inglés III','Inglés IV','Inglés V','Inglés VI','Inglés VII','Inglés VIII',
 'Habilidades socioemocionales y manejo de conflictos',
 'Desarrollo del pensamiento y toma de decisiones',
 'Ética profesional','Ética y legislación en tecnologías de la información',
 'Liderazgo de equipos de alto desempeño','Habilidades gerenciales',
 'Cálculo diferencial','Cálculo integral','Cálculo de varias variables',
 'Probabilidad y estadística','Ecuaciones diferenciales'
);

DROP TEMPORARY TABLE tmp_plan_ccd;
DROP TEMPORARY TABLE tmp_grupos_ccd;
COMMIT;
