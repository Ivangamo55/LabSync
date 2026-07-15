/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-11.4.12-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: labsync_db
-- ------------------------------------------------------
-- Server version	11.4.12-MariaDB-ubu2404

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Current Database: `labsync_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `labsync_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `labsync_db`;

--
-- Table structure for table `bitacora`
--

DROP TABLE IF EXISTS `bitacora`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `bitacora` (
  `id_bitacora` int(11) NOT NULL AUTO_INCREMENT,
  `fecha` date NOT NULL,
  `nombre_usuario` varchar(100) NOT NULL,
  `rol_usuario` varchar(50) NOT NULL,
  `carrera_dependencia` varchar(100) NOT NULL,
  `grado` varchar(20) DEFAULT NULL,
  `grupo` varchar(20) DEFAULT NULL,
  `laboratorio` varchar(50) NOT NULL,
  `actividad_materia` varchar(150) NOT NULL,
  `turno` varchar(50) NOT NULL,
  `horario` varchar(50) NOT NULL,
  `total_usuarios` int(11) NOT NULL,
  `observaciones` text DEFAULT NULL,
  `estado` varchar(50) DEFAULT 'Registrado',
  `fecha_registro` timestamp NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_bitacora`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bitacora`
--

LOCK TABLES `bitacora` WRITE;
/*!40000 ALTER TABLE `bitacora` DISABLE KEYS */;
/*!40000 ALTER TABLE `bitacora` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `estudiante`
--

DROP TABLE IF EXISTS `estudiante`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `estudiante` (
  `id_usuario` int(11) NOT NULL,
  `matricula` varchar(50) NOT NULL,
  `carrera` varchar(100) NOT NULL,
  `turno` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `matricula` (`matricula`),
  CONSTRAINT `fk_estudiante_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estudiante`
--

LOCK TABLES `estudiante` WRITE;
/*!40000 ALTER TABLE `estudiante` DISABLE KEYS */;
INSERT INTO `estudiante` VALUES
(20,'2125100191','TSU - DSM','Matutino');
/*!40000 ALTER TABLE `estudiante` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `externo`
--

DROP TABLE IF EXISTS `externo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `externo` (
  `id_usuario` int(11) NOT NULL,
  `institucion_origen` varchar(150) NOT NULL,
  `motivo_visita` text DEFAULT NULL,
  PRIMARY KEY (`id_usuario`),
  CONSTRAINT `fk_externo_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `externo`
--

LOCK TABLES `externo` WRITE;
/*!40000 ALTER TABLE `externo` DISABLE KEYS */;
/*!40000 ALTER TABLE `externo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventario`
--

DROP TABLE IF EXISTS `inventario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventario` (
  `id_inventario` int(11) NOT NULL AUTO_INCREMENT,
  `codigo` varchar(50) NOT NULL,
  `nombre_equipo` varchar(100) NOT NULL,
  `tipo_dispositivo` varchar(50) NOT NULL,
  `marca` varchar(80) DEFAULT NULL,
  `modelo` varchar(80) DEFAULT NULL,
  `no_serie` varchar(100) DEFAULT NULL,
  `laboratorio` varchar(100) DEFAULT NULL,
  `estado` varchar(50) DEFAULT NULL,
  `ultimo_mantenimiento` date DEFAULT NULL,
  `observaciones` text DEFAULT NULL,
  `fecha_registro` timestamp NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_inventario`),
  UNIQUE KEY `codigo` (`codigo`),
  UNIQUE KEY `no_serie` (`no_serie`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventario`
--

LOCK TABLES `inventario` WRITE;
/*!40000 ALTER TABLE `inventario` DISABLE KEYS */;
INSERT INTO `inventario` VALUES
(1,'2026A123456','leooo','Computadora','olll','52100','1','PB-05','Disponible','2026-07-14','','2026-07-14 19:25:57');
/*!40000 ALTER TABLE `inventario` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `laboratorista`
--

DROP TABLE IF EXISTS `laboratorista`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `laboratorista` (
  `id_usuario` int(11) NOT NULL,
  `turno` varchar(20) NOT NULL,
  `piso_encargado` varchar(50) NOT NULL,
  PRIMARY KEY (`id_usuario`),
  CONSTRAINT `fk_laboratorista_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `laboratorista`
--

LOCK TABLES `laboratorista` WRITE;
/*!40000 ALTER TABLE `laboratorista` DISABLE KEYS */;
INSERT INTO `laboratorista` VALUES
(11,'Matutino','PB - M');
/*!40000 ALTER TABLE `laboratorista` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mantenimiento`
--

DROP TABLE IF EXISTS `mantenimiento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `mantenimiento` (
  `id_mantenimiento` int(11) NOT NULL AUTO_INCREMENT,
  `id_falla` int(11) DEFAULT NULL,
  `codigo_equipo` varchar(50) DEFAULT NULL,
  `nombre_equipo` varchar(100) DEFAULT NULL,
  `laboratorio` varchar(100) DEFAULT NULL,
  `tipo_mantenimiento` varchar(80) DEFAULT NULL,
  `fecha_programada` date DEFAULT NULL,
  `estado` varchar(50) DEFAULT NULL,
  `responsable` varchar(100) DEFAULT NULL,
  `observaciones` text DEFAULT NULL,
  `fecha_registro` timestamp NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_mantenimiento`),
  KEY `fk_mantenimiento_inventario_codigo` (`codigo_equipo`),
  KEY `idx_mantenimiento_id_falla` (`id_falla`),
  CONSTRAINT `fk_mantenimiento_inventario_codigo` FOREIGN KEY (`codigo_equipo`) REFERENCES `inventario` (`codigo`) ON DELETE NO ACTION ON UPDATE CASCADE,
  CONSTRAINT `fk_mantenimiento_reporte_falla` FOREIGN KEY (`id_falla`) REFERENCES `reporte_fallas` (`id_falla`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mantenimiento`
--

LOCK TABLES `mantenimiento` WRITE;
/*!40000 ALTER TABLE `mantenimiento` DISABLE KEYS */;
INSERT INTO `mantenimiento` VALUES
(9,NULL,NULL,'Lenovo','M-11','Preventivo','2026-07-13','Cancelado','Antonio','','2026-07-09 16:53:34'),
(10,NULL,NULL,'Lenovo','M-13','Preventivo','2026-07-10','Cancelado','Antonio','','2026-07-10 03:17:11'),
(11,NULL,'2026A123456','leooo','PB-05','Actualización de software','2026-07-15','Realizado','Antonio','','2026-07-14 19:26:40');
/*!40000 ALTER TABLE `mantenimiento` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reporte_fallas`
--

DROP TABLE IF EXISTS `reporte_fallas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `reporte_fallas` (
  `id_falla` int(11) NOT NULL AUTO_INCREMENT,
  `id_usuario` int(11) DEFAULT NULL,
  `id_inventario` int(11) DEFAULT NULL,
  `id_reserva` int(11) DEFAULT NULL,
  `codigo_equipo` varchar(50) DEFAULT NULL,
  `nombre_equipo` varchar(100) DEFAULT NULL,
  `laboratorio` varchar(100) NOT NULL,
  `reportado_por` varchar(100) NOT NULL,
  `rol_reportante` varchar(50) NOT NULL,
  `descripcion_falla` text NOT NULL,
  `prioridad` varchar(50) DEFAULT 'Media',
  `estado` varchar(50) DEFAULT 'Pendiente',
  `fecha_reporte` timestamp NULL DEFAULT current_timestamp(),
  `fecha_revision` date DEFAULT NULL,
  `observaciones` text DEFAULT NULL,
  PRIMARY KEY (`id_falla`),
  KEY `fk_rf_usuario` (`id_usuario`),
  KEY `fk_rf_inventario` (`id_inventario`),
  KEY `fk_rf_reserva` (`id_reserva`),
  CONSTRAINT `fk_rf_inventario` FOREIGN KEY (`id_inventario`) REFERENCES `inventario` (`id_inventario`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rf_reserva` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rf_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reporte_fallas`
--

LOCK TABLES `reporte_fallas` WRITE;
/*!40000 ALTER TABLE `reporte_fallas` DISABLE KEYS */;
INSERT INTO `reporte_fallas` VALUES
(1,NULL,NULL,NULL,'PC-M05-001','Computadora de escritorio HP','M-05','Juan Pérez López','Profesor','El equipo no enciende correctamente.','Alta','Pendiente','2026-07-10 01:49:13','2026-07-09','Se reportó durante una práctica de programación.'),
(2,20,NULL,NULL,'2026A21255',NULL,'5-03','Ivan Garcia Moreno X','Estudiante','[Tipo: Hardware] El equipo no enciende','Baja','Atendida','2026-07-10 18:00:00','2026-07-14',NULL),
(3,NULL,NULL,NULL,'2026A1550',NULL,'5-06','Ivan Garcia Moreno X','Estudiante','[Tipo: Hardware] La computadora sacó humo al intentar encender.','Media','Pendiente','2026-07-13 18:00:00',NULL,NULL);
/*!40000 ALTER TABLE `reporte_fallas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservas`
--

DROP TABLE IF EXISTS `reservas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservas` (
  `id_reserva` int(11) NOT NULL AUTO_INCREMENT,
  `id_usuario` int(11) DEFAULT NULL,
  `nombre_solicitante` varchar(100) NOT NULL,
  `rol_solicitante` varchar(50) NOT NULL,
  `laboratorio` varchar(100) NOT NULL,
  `actividad` varchar(150) NOT NULL,
  `grado` varchar(20) NOT NULL,
  `grupo` varchar(20) NOT NULL,
  `turno` varchar(50) NOT NULL,
  `fecha` date NOT NULL,
  `horario` varchar(50) NOT NULL,
  `cantidad_alumnos` int(11) NOT NULL,
  `estado` varchar(50) NOT NULL DEFAULT 'Pendiente',
  `observaciones` text DEFAULT NULL,
  `fecha_registro` timestamp NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_reserva`),
  KEY `fk_reservas_usuario` (`id_usuario`),
  KEY `idx_reservas_disponibilidad` (`laboratorio`,`fecha`,`horario`,`estado`),
  CONSTRAINT `fk_reservas_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservas`
--

LOCK TABLES `reservas` WRITE;
/*!40000 ALTER TABLE `reservas` DISABLE KEYS */;
INSERT INTO `reservas` VALUES
(1,NULL,'Juan Pérez López','Profesor','M-05','Práctica de programación','3','A','Matutino','2026-07-07','08:00 - 10:00',25,'Aprobada','Se solicita laboratorio para práctica de Java.','2026-07-06 16:21:05'),
(2,20,'Ivan Garcia Moreno X','Estudiante','M-05','Examen de programación','N/A','N/A','Matutino','2026-07-31','7:50 - 8:40',1,'Finalizada',NULL,'2026-07-14 18:04:09'),
(3,NULL,'Ivan Garcia Moreno X','Estudiante','M-13','Practicas de redes','N/A','N/A','Matutino','2026-07-30','10:50 - 11:40',1,'Aprobada',NULL,'2026-07-15 01:09:04'),
(4,NULL,'Ivan Garcia Moreno X','Estudiante','PB-05','Examen de POO','N/A','N/A','Matutino','2026-07-30','19:40 - 20:30',1,'Rechazada',NULL,'2026-07-15 01:09:30');
/*!40000 ALTER TABLE `reservas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  `apellido_p` varchar(20) NOT NULL,
  `apellido_m` varchar(20) NOT NULL,
  `rol` varchar(15) NOT NULL,
  `correo` varchar(75) NOT NULL,
  `password` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES
(11,'Antonio','Gonzalez','Torres','Laboratorista','labo@utj.edu.mx','03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4'),
(19,'Marlene','Mora','Olmos','Profesor','marmora@utj.edu.mx','5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5'),
(20,'Ivan','Garcia Moreno','X','Estudiante','2125100191@soy.utj.edu.mx','03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4');
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'labsync_db'
--

--
-- Dumping routines for database 'labsync_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-07-14 21:04:59
