-- MySQL dump 10.13  Distrib 8.4.7, for Win64 (x86_64)
--
-- Host: localhost    Database: labsync_db
-- ------------------------------------------------------
-- Server version	8.4.7

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `alertas`
--

DROP TABLE IF EXISTS `alertas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alertas` (
  `id_alerta` int NOT NULL AUTO_INCREMENT,
  `tipo` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `referencia` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `titulo` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detalle` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `prioridad` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Media',
  `estado` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Nueva',
  `fecha_creacion` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_lectura` timestamp NULL DEFAULT NULL,
  `fecha_atencion` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id_alerta`),
  UNIQUE KEY `uk_alerta_origen` (`tipo`,`referencia`),
  KEY `idx_alerta_estado_prioridad` (`estado`,`prioridad`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alertas`
--

LOCK TABLES `alertas` WRITE;
/*!40000 ALTER TABLE `alertas` DISABLE KEYS */;
/*!40000 ALTER TABLE `alertas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bitacora`
--

DROP TABLE IF EXISTS `bitacora`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bitacora` (
  `id_bitacora` int NOT NULL AUTO_INCREMENT,
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
  `total_usuarios` int NOT NULL,
  `observaciones` text,
  `estado` varchar(50) DEFAULT 'Registrado',
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_bitacora`)
) ENGINE=MyISAM AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bitacora`
--

LOCK TABLES `bitacora` WRITE;
/*!40000 ALTER TABLE `bitacora` DISABLE KEYS */;
INSERT INTO `bitacora` VALUES (1,'2026-07-13','Omar Méndez López','Profesor','TSU Desarrollo de Software','3','A','M-12','Programación','Matutino','7:00 - 8:30',30,'Práctica de clase','Registrado','2026-07-14 03:48:19'),(2,'2026-07-20','Luis Manuel Perez Arce','Profesor','DSM','1°','A','PB-05','Topicos de calidad','Matutino','7:00 - 7:50',20,NULL,'Registrado','2026-07-20 18:06:51'),(3,'2026-07-20','Luis Manuel Perez Arce','Profesor','DSM','6°','A','5-03','Topicos de calidad para el diseño de software','Matutino','19:40 - 20:30',20,NULL,'Registrado','2026-07-21 01:30:51');
/*!40000 ALTER TABLE `bitacora` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `estudiante`
--

DROP TABLE IF EXISTS `estudiante`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estudiante` (
  `id_usuario` int NOT NULL,
  `matricula` varchar(50) NOT NULL,
  `carrera` varchar(100) NOT NULL,
  `turno` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `matricula` (`matricula`),
  CONSTRAINT `fk_estudiante_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estudiante`
--

LOCK TABLES `estudiante` WRITE;
/*!40000 ALTER TABLE `estudiante` DISABLE KEYS */;
INSERT INTO `estudiante` VALUES (20,'2125100178','TSU - DSM','Matutino'),(21,'2125100177','TSU - DSM','Matutino'),(22,'2125100192','TSU - DSM','Matutino'),(23,'12345','TSU - ENV','Matutino'),(26,'2125100191','TSU - DSM','Matutino');
/*!40000 ALTER TABLE `estudiante` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `externo`
--

DROP TABLE IF EXISTS `externo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `externo` (
  `id_usuario` int NOT NULL,
  `institucion_origen` varchar(150) NOT NULL,
  `motivo_visita` text,
  PRIMARY KEY (`id_usuario`),
  CONSTRAINT `fk_externo_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventario` (
  `id_inventario` int NOT NULL AUTO_INCREMENT,
  `codigo` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_equipo` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_dispositivo` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `marca` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `modelo` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `no_serie` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `laboratorio` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ultimo_mantenimiento` date DEFAULT NULL,
  `observaciones` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_inventario`),
  UNIQUE KEY `codigo` (`codigo`),
  UNIQUE KEY `no_serie` (`no_serie`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventario`
--

LOCK TABLES `inventario` WRITE;
/*!40000 ALTER TABLE `inventario` DISABLE KEYS */;
INSERT INTO `inventario` VALUES (4,'2026A123456','Lenovo','Computadora','hp','1','25','M-13','En mantenimiento','2026-07-08','','2026-07-01 19:14:39'),(5,'24521','labsync','Computadora','hp','lenovo','4168','M-02','En mantenimiento',NULL,'','2026-07-07 13:34:15'),(6,'2219A401753','Lenovo','Computadora','Lenovo','15957','123456789','M-11','Disponible','2026-07-20','Es unacomputadora Only One','2026-07-08 16:12:00'),(7,'2125A123456','N/A','Computadora','Lenovo','Prichos','23241989','M-11','Disponible',NULL,'','2026-07-15 16:35:24');
/*!40000 ALTER TABLE `inventario` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `laboratorios`
--

DROP TABLE IF EXISTS `laboratorios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `laboratorios` (
  `id_laboratorio` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(20) NOT NULL,
  `total_equipos` int NOT NULL,
  `estado` varchar(30) NOT NULL DEFAULT 'Disponible',
  PRIMARY KEY (`id_laboratorio`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `laboratorios`
--

LOCK TABLES `laboratorios` WRITE;
/*!40000 ALTER TABLE `laboratorios` DISABLE KEYS */;
INSERT INTO `laboratorios` VALUES (1,'PB-05',20,'Disponible'),(2,'M-11',20,'Disponible'),(3,'M-12',20,'Disponible'),(4,'M-14',20,'Disponible'),(5,'M-19',20,'Disponible'),(6,'M-02',30,'Disponible'),(7,'M-05',20,'Disponible'),(8,'5-06',20,'Disponible'),(9,'5-03',20,'Disponible'),(10,'M-13',20,'Disponible');
/*!40000 ALTER TABLE `laboratorios` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `laboratorista`
--

DROP TABLE IF EXISTS `laboratorista`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `laboratorista` (
  `id_usuario` int NOT NULL,
  `turno` varchar(20) NOT NULL,
  `piso_encargado` varchar(50) NOT NULL,
  PRIMARY KEY (`id_usuario`),
  CONSTRAINT `fk_laboratorista_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `laboratorista`
--

LOCK TABLES `laboratorista` WRITE;
/*!40000 ALTER TABLE `laboratorista` DISABLE KEYS */;
INSERT INTO `laboratorista` VALUES (11,'Matutino','PB - M'),(25,'Matutino','1 - 5');
/*!40000 ALTER TABLE `laboratorista` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mantenimiento`
--

DROP TABLE IF EXISTS `mantenimiento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mantenimiento` (
  `id_mantenimiento` int NOT NULL AUTO_INCREMENT,
  `id_falla` int DEFAULT NULL,
  `codigo_equipo` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre_equipo` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `laboratorio` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo_mantenimiento` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_programada` date DEFAULT NULL,
  `estado` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `responsable` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `observaciones` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_mantenimiento`),
  KEY `fk_mantenimiento_inventario_codigo` (`codigo_equipo`),
  KEY `idx_mantenimiento_id_falla` (`id_falla`),
  CONSTRAINT `fk_mantenimiento_inventario_codigo` FOREIGN KEY (`codigo_equipo`) REFERENCES `inventario` (`codigo`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_mantenimiento_reporte_falla` FOREIGN KEY (`id_falla`) REFERENCES `reporte_fallas` (`id_falla`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mantenimiento`
--

LOCK TABLES `mantenimiento` WRITE;
/*!40000 ALTER TABLE `mantenimiento` DISABLE KEYS */;
INSERT INTO `mantenimiento` VALUES (11,NULL,'2219A401753','Lenovo','M-11','Preventivo','2026-07-17','Realizado','Antonio','','2026-07-15 02:41:46'),(12,NULL,'2026A123456','Lenovo','M-13','Actualización de software','2026-07-16','En proceso','Marlene Laboratorista','Se está clonando el software','2026-07-15 16:16:40'),(13,NULL,'24521','N/A','M-02','Correctivo','2026-07-16','Pendiente','Antonio','','2026-07-15 18:13:04');
/*!40000 ALTER TABLE `mantenimiento` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reporte_fallas`
--

DROP TABLE IF EXISTS `reporte_fallas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reporte_fallas` (
  `id_falla` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int DEFAULT NULL,
  `id_inventario` int DEFAULT NULL,
  `id_reserva` int DEFAULT NULL,
  `codigo_equipo` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre_equipo` varchar(100) DEFAULT NULL,
  `laboratorio` varchar(100) NOT NULL,
  `reportado_por` varchar(100) NOT NULL,
  `rol_reportante` varchar(50) NOT NULL,
  `descripcion_falla` text NOT NULL,
  `prioridad` varchar(50) DEFAULT 'Media',
  `estado` varchar(50) DEFAULT 'Pendiente',
  `fecha_reporte` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_revision` date DEFAULT NULL,
  `observaciones` text,
  PRIMARY KEY (`id_falla`),
  KEY `fk_rf_usuario` (`id_usuario`),
  KEY `fk_rf_inventario` (`id_inventario`),
  KEY `fk_rf_reserva` (`id_reserva`),
  CONSTRAINT `fk_rf_inventario` FOREIGN KEY (`id_inventario`) REFERENCES `inventario` (`id_inventario`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rf_reserva` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rf_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reporte_fallas`
--

LOCK TABLES `reporte_fallas` WRITE;
/*!40000 ALTER TABLE `reporte_fallas` DISABLE KEYS */;
INSERT INTO `reporte_fallas` VALUES (6,NULL,NULL,NULL,'123456',NULL,'PB-05','Daniel Gonzalez Hernadnez','Estudiante','[Tipo: Hardware] Test','Media','En revisión','2026-07-13 18:00:00','2026-07-20',NULL),(7,27,NULL,NULL,'2026A252000',NULL,'5-06','Luis Manuel Perez Arce','Profesor','[Tipo: Hardware] La computadora sacó húmo al encender.','Media','Atendida','2026-07-20 18:04:12','2026-07-20',NULL),(8,27,NULL,NULL,'2026A52213',NULL,'M-11','Luis Manuel Perez Arce','Profesor','[Tipo: Software] La comptadora marcaba que no contenia OS','Media','En revisión','2026-07-03 18:00:00','2026-07-20',NULL);
/*!40000 ALTER TABLE `reporte_fallas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservas`
--

DROP TABLE IF EXISTS `reservas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservas` (
  `id_reserva` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int DEFAULT NULL,
  `nombre_solicitante` varchar(100) NOT NULL,
  `rol_solicitante` varchar(50) NOT NULL,
  `laboratorio` varchar(100) NOT NULL,
  `actividad` varchar(150) NOT NULL,
  `carrera` varchar(100) NOT NULL DEFAULT 'N/A',
  `grado` varchar(20) NOT NULL,
  `grupo` varchar(20) NOT NULL,
  `turno` varchar(50) NOT NULL,
  `fecha` date NOT NULL,
  `horario` varchar(50) NOT NULL,
  `cantidad_alumnos` int NOT NULL,
  `estado` varchar(50) NOT NULL DEFAULT 'Pendiente',
  `observaciones` text,
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_reserva`),
  KEY `fk_reservas_usuario` (`id_usuario`),
  CONSTRAINT `fk_reservas_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservas`
--

LOCK TABLES `reservas` WRITE;
/*!40000 ALTER TABLE `reservas` DISABLE KEYS */;
INSERT INTO `reservas` VALUES (15,NULL,'Daniel Gonzalez Hernadnez','Estudiante','5-06','AAA','N/A','N/A','N/A','Matutino','2026-07-17','7:00 - 7:50',1,'Aprobada',NULL,'2026-07-17 01:52:36'),(16,NULL,'Iván Garcia Moreno X','Estudiante','5-03','Estudio para examen de programación','N/A','N/A','N/A','Matutino','2026-07-20','7:00 - 7:50',1,'Rechazada',NULL,'2026-07-20 16:26:29'),(17,27,'Luis Manuel Perez Arce','Profesor','PB-05','Topicos de calidad','DSM - Desarrollo de Software Multiplataforma','1°','A','Matutino','2026-07-21','7:00 - 7:50',20,'Cancelada',NULL,'2026-07-20 18:02:05'),(18,27,'Luis Manuel Perez Arce','Profesor','5-03','Topicos de calidad para el diseño de software','DSM - Desarrollo de Software Multiplataforma','6°','A','Vespertino','2026-07-20','19:40 - 20:30',20,'Aprobada','Se usarán todas los equipos-','2026-07-21 01:28:08');
/*!40000 ALTER TABLE `reservas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  `apellido_p` varchar(20) NOT NULL,
  `apellido_m` varchar(20) NOT NULL,
  `rol` varchar(15) NOT NULL,
  `correo` varchar(75) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES (11,'Antonio','Gonzalez','Torres','Laboratorista','labo@utj.edu.mx','03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4'),(19,'Marlene','Mora','Olmos','Profesor','marmora@utj.edu.mx','5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5'),(20,'Daniel','Gonzalez','Hernadnez','Estudiante','2125100178@soy.utj.edu.mx','03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4'),(21,'Naaaaaaaaaaami','Coba','P3','Estudiante','2125100177@soy.utj.edu.mx','e55b271187d8385f31e23d676dc8859a2d5fb49a6d195219378dd234fd361d71'),(22,'valentina','cosio','leon','Estudiante','2125100192@soy.utj.edu.mx','03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4'),(23,'Marlene Alumno','Mora','Olmos','Estudiante','marmora1@soy.utj.edu.mx','5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5'),(24,'Marlene Profesor','Mora','Olmoms','Profesor','marmora2@utj.edu.mx','5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5'),(25,'Marlene Laboratorista','Mora','Olmos','Laboratorista','marmora3@utj.edu.mx','5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5'),(26,'Iván','Garcia Moreno','X','Estudiante','2125100191@soy.utj.edu.mx','03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4'),(27,'Luis Manuel','Perez','Arce','Profesor','lumanuelPearce@utj.edu.mx','03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4');
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
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-21  9:54:39
