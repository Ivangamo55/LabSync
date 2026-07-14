-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1:3306
-- Tiempo de generación: 13-07-2026 a las 15:39:29
-- Versión del servidor: 8.3.0
-- Versión de PHP: 8.2.18

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `labsync_db`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `estudiante`
--

DROP TABLE IF EXISTS `estudiante`;
CREATE TABLE IF NOT EXISTS `estudiante` (
  `id_usuario` int NOT NULL,
  `matricula` varchar(50) NOT NULL,
  `carrera` varchar(100) NOT NULL,
  `turno` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `matricula` (`matricula`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `externo`
--

DROP TABLE IF EXISTS `externo`;
CREATE TABLE IF NOT EXISTS `externo` (
  `id_usuario` int NOT NULL,
  `institucion_origen` varchar(150) NOT NULL,
  `motivo_visita` text,
  PRIMARY KEY (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `inventario`
--

DROP TABLE IF EXISTS `inventario`;
CREATE TABLE IF NOT EXISTS `inventario` (
  `id_inventario` int NOT NULL AUTO_INCREMENT,
  `codigo` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_equipo` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_dispositivo` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `marca` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `modelo` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `no_serie` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `laboratorio` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ultimo_mantenimiento` date DEFAULT NULL,
  `observaciones` text COLLATE utf8mb4_unicode_ci,
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_inventario`),
  UNIQUE KEY `codigo` (`codigo`),
  UNIQUE KEY `no_serie` (`no_serie`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `inventario`
--

INSERT INTO `inventario` (`id_inventario`, `codigo`, `nombre_equipo`, `tipo_dispositivo`, `marca`, `modelo`, `no_serie`, `laboratorio`, `estado`, `ultimo_mantenimiento`, `observaciones`, `fecha_registro`) VALUES
(4, '2026A123456', 'Lenovo', 'Computadora', 'hp', '1', '25', 'M-13', 'En mantenimiento', '2026-07-08', '', '2026-07-01 19:14:39'),
(5, '24521', 'labsync', 'Computadora', 'hp', 'lenovo', '4168', 'M-02', 'Dado de baja', NULL, '', '2026-07-07 13:34:15'),
(6, '2219A401753', 'Lenovo', 'Computadora', 'Lenovo', '15957', '123456789', 'M-11', 'En mantenimiento', '2026-07-08', 'Es unacomputadora Only One', '2026-07-08 16:12:00');

-- --------------------------------------------------------

--
-- Catalogo de laboratorios disponible para reservaciones
--

DROP TABLE IF EXISTS `laboratorios`;
CREATE TABLE IF NOT EXISTS `laboratorios` (
  `id_laboratorio` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `capacidad` int NOT NULL DEFAULT 1,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_laboratorio`),
  UNIQUE KEY `uk_laboratorios_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `laboratorios` (`nombre`, `capacidad`, `activo`) VALUES
('PB-05', 30, 1),
('M-02', 30, 1),
('M-05', 30, 1),
('M-11', 30, 1),
('M-12', 30, 1),
('M-13', 30, 1),
('M-14', 30, 1),
('5-03', 25, 1),
('5-06', 25, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `laboratorista`
--

DROP TABLE IF EXISTS `laboratorista`;
CREATE TABLE IF NOT EXISTS `laboratorista` (
  `id_usuario` int NOT NULL,
  `turno` varchar(20) NOT NULL,
  `piso_encargado` varchar(50) NOT NULL,
  PRIMARY KEY (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Volcado de datos para la tabla `laboratorista`
--

INSERT INTO `laboratorista` (`id_usuario`, `turno`, `piso_encargado`) VALUES
(11, 'Matutino', 'PB - M');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `mantenimiento`
--

DROP TABLE IF EXISTS `mantenimiento`;
CREATE TABLE IF NOT EXISTS `mantenimiento` (
  `id_mantenimiento` int NOT NULL AUTO_INCREMENT,
  `id_falla` int DEFAULT NULL,
  `codigo_equipo` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre_equipo` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `laboratorio` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo_mantenimiento` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_programada` date DEFAULT NULL,
  `estado` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `responsable` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `observaciones` text COLLATE utf8mb4_unicode_ci,
  `fecha_registro` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_mantenimiento`),
  KEY `fk_mantenimiento_inventario_codigo` (`codigo_equipo`),
  KEY `idx_mantenimiento_id_falla` (`id_falla`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `mantenimiento`
--

INSERT INTO `mantenimiento` (`id_mantenimiento`, `id_falla`, `codigo_equipo`, `nombre_equipo`, `laboratorio`, `tipo_mantenimiento`, `fecha_programada`, `estado`, `responsable`, `observaciones`, `fecha_registro`) VALUES
(9, NULL, '2219A401753', 'Lenovo', 'M-11', 'Preventivo', '2026-07-13', 'Cancelado', 'Antonio', '', '2026-07-09 16:53:34'),
(10, NULL, '2026A123456', 'Lenovo', 'M-13', 'Preventivo', '2026-07-10', 'Pendiente', 'Antonio', '', '2026-07-10 03:17:11');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reporte_fallas`
--

DROP TABLE IF EXISTS `reporte_fallas`;
CREATE TABLE IF NOT EXISTS `reporte_fallas` (
  `id_falla` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int DEFAULT NULL,
  `id_inventario` int DEFAULT NULL,
  `id_reserva` int DEFAULT NULL,
  `codigo_equipo` varchar(50) DEFAULT NULL,
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
  KEY `fk_rf_reserva` (`id_reserva`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Volcado de datos para la tabla `reporte_fallas`
--

INSERT INTO `reporte_fallas` (`id_falla`, `id_usuario`, `id_inventario`, `id_reserva`, `codigo_equipo`, `nombre_equipo`, `laboratorio`, `reportado_por`, `rol_reportante`, `descripcion_falla`, `prioridad`, `estado`, `fecha_reporte`, `fecha_revision`, `observaciones`) VALUES
(1, NULL, NULL, NULL, 'PC-M05-001', 'Computadora de escritorio HP', 'M-05', 'Juan Pérez López', 'Profesor', 'El equipo no enciende correctamente.', 'Alta', 'Pendiente', '2026-07-10 01:49:13', '2026-07-09', 'Se reportó durante una práctica de programación.');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reservas`
--

DROP TABLE IF EXISTS `reservas`;
CREATE TABLE IF NOT EXISTS `reservas` (
  `id_reserva` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int DEFAULT NULL,
  `nombre_solicitante` varchar(100) NOT NULL,
  `rol_solicitante` varchar(50) NOT NULL,
  `laboratorio` varchar(100) NOT NULL,
  `actividad` varchar(150) NOT NULL,
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
  KEY `fk_reservas_usuario` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX `idx_reservas_disponibilidad`
  ON `reservas` (`laboratorio`, `fecha`, `horario`, `estado`);

--
-- Volcado de datos para la tabla `reservas`
--

INSERT INTO `reservas` (`id_reserva`, `id_usuario`, `nombre_solicitante`, `rol_solicitante`, `laboratorio`, `actividad`, `grado`, `grupo`, `turno`, `fecha`, `horario`, `cantidad_alumnos`, `estado`, `observaciones`, `fecha_registro`) VALUES
(1, NULL, 'Juan Pérez López', 'Profesor', 'M-05', 'Práctica de programación', '3', 'A', 'Matutino', '2026-07-07', '08:00 - 10:00', 25, 'Aprobada', 'Se solicita laboratorio para práctica de Java.', '2026-07-06 16:21:05');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuario`
--

DROP TABLE IF EXISTS `usuario`;
CREATE TABLE IF NOT EXISTS `usuario` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  `apellido_p` varchar(20) NOT NULL,
  `apellido_m` varchar(20) NOT NULL,
  `rol` varchar(15) NOT NULL,
  `correo` varchar(75) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Volcado de datos para la tabla `usuario`
--

INSERT INTO `usuario` (`id`, `nombre`, `apellido_p`, `apellido_m`, `rol`, `correo`, `password`) VALUES
(11, 'Antonio', 'Gonzalez', 'Torres', 'Laboratorista', 'labo@utj.edu.mx', '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4'),
(19, 'Marlene', 'Mora', 'Olmos', 'Profesor', 'marmora@utj.edu.mx', '5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5');

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `estudiante`
--
ALTER TABLE `estudiante`
  ADD CONSTRAINT `fk_estudiante_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `externo`
--
ALTER TABLE `externo`
  ADD CONSTRAINT `fk_externo_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `laboratorista`
--
ALTER TABLE `laboratorista`
  ADD CONSTRAINT `fk_laboratorista_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `mantenimiento`
--
ALTER TABLE `mantenimiento`
  ADD CONSTRAINT `fk_mantenimiento_inventario_codigo` FOREIGN KEY (`codigo_equipo`) REFERENCES `inventario` (`codigo`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_mantenimiento_reporte_falla` FOREIGN KEY (`id_falla`) REFERENCES `reporte_fallas` (`id_falla`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `reporte_fallas`
--
ALTER TABLE `reporte_fallas`
  ADD CONSTRAINT `fk_rf_inventario` FOREIGN KEY (`id_inventario`) REFERENCES `inventario` (`id_inventario`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_rf_reserva` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_rf_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `reservas`
--
ALTER TABLE `reservas`
  ADD CONSTRAINT `fk_reservas_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
