-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: dblegion3dtracker
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `clientes`
--

DROP TABLE IF EXISTS `clientes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `clientes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `telefono` varchar(50) DEFAULT NULL,
  `rut` varchar(50) DEFAULT NULL,
  `activo` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cliente_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `clientes`
--

LOCK TABLES `clientes` WRITE;
/*!40000 ALTER TABLE `clientes` DISABLE KEYS */;
INSERT INTO `clientes` VALUES (1,'efren tovar','ejts29@gmail.com','+56952480417','25698445-8',1);
/*!40000 ALTER TABLE `clientes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `detalles_tecnicos`
--

DROP TABLE IF EXISTS `detalles_tecnicos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `detalles_tecnicos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pedido_id` bigint NOT NULL,
  `activo` tinyint(1) DEFAULT '1',
  `medida_ancho` double DEFAULT NULL,
  `medida_alto` double DEFAULT NULL,
  `medida_profundidad` double DEFAULT NULL,
  `cantidad_unidades` int DEFAULT NULL,
  `dias_entrega` varchar(100) DEFAULT NULL,
  `drive_file_id` varchar(255) DEFAULT NULL,
  `tiene_pieza_fisica` tinyint(1) DEFAULT '0',
  `necesita_modificacion` varchar(255) DEFAULT NULL,
  `es_copia_exacta` tinyint(1) DEFAULT '0',
  `material_solicitado` varchar(100) DEFAULT NULL,
  `color_solicitado` varchar(100) DEFAULT NULL,
  `entorno_uso` varchar(100) DEFAULT NULL,
  `presupuesto_estimado` varchar(100) DEFAULT NULL,
  `metodo_entrega` varchar(100) DEFAULT NULL,
  `tolerancia_check` tinyint(1) DEFAULT '0',
  `rut` varchar(50) DEFAULT NULL,
  `telefono_contacto` varchar(50) DEFAULT NULL,
  `region` varchar(100) DEFAULT NULL,
  `comuna` varchar(100) DEFAULT NULL,
  `calle_y_numero` varchar(255) DEFAULT NULL,
  `depto_casa_oficina` varchar(255) DEFAULT NULL,
  `informacion_adicional` varchar(1000) DEFAULT NULL,
  `tipo_envio_starken` varchar(100) DEFAULT NULL,
  `link_archivo_final` varchar(1000) DEFAULT NULL,
  `link_formulario_ingenieria` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_detalles_pedido_id` (`pedido_id`),
  CONSTRAINT `fk_detalles_pedido` FOREIGN KEY (`pedido_id`) REFERENCES `pedidos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `detalles_tecnicos`
--

LOCK TABLES `detalles_tecnicos` WRITE;
/*!40000 ALTER TABLE `detalles_tecnicos` DISABLE KEYS */;
INSERT INTO `detalles_tecnicos` VALUES (1,1,1,1,2,3,25,'1 Semana',NULL,0,'Ya tengo el archivo 3D listo para imprimir',1,'TPU/ABSMAX','Gris','Exterior Intemperie/Sol/UV','Entre $50.000 y $300.000 CLP','Retiro en Taller',0,'25698445-8','+56952480417','','','','','Está el formulario dos ',NULL,'https://www.magnific.com/es/fotos-vectores-gratis/carro-deportivo',NULL),(2,2,1,1,1,1,25,'2 Semanas','129YWyF8RVaBkpYHftoQhXWMpbCMTbYru',1,'Modificar diseño (Cambiar geometría original)',0,'Sakata/BLAB','Negro','Contacto con alimentos','Entre $500.000 y $1.000.000 CLP','Retiro en Taller',1,'25698445-8','+56952480417','','','','','hhhhhhhhhhhhhhhhhhhhhhhhhhh',NULL,'https://www.printables.com/model/42917-boton-button|https://drive.google.com/file/d/129YWyF8RVaBkpYHftoQhXWMpbCMTbYru/view?usp=drivesdk',NULL);
/*!40000 ALTER TABLE `detalles_tecnicos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `historial_estados`
--

DROP TABLE IF EXISTS `historial_estados`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `historial_estados` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pedido_id` bigint NOT NULL,
  `estado_anterior` varchar(100) DEFAULT NULL,
  `estado_nuevo` varchar(100) NOT NULL,
  `comentario` text,
  `fecha_cambio` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_historial_pedido` (`pedido_id`),
  CONSTRAINT `fk_historial_pedido` FOREIGN KEY (`pedido_id`) REFERENCES `pedidos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `historial_estados`
--

LOCK TABLES `historial_estados` WRITE;
/*!40000 ALTER TABLE `historial_estados` DISABLE KEYS */;
INSERT INTO `historial_estados` VALUES (1,1,NULL,'NUEVA','Ingreso automatizado desde Webhook WordPress.','2026-06-17 15:13:02'),(2,1,'NUEVA','PENDIENTE_TECNICOS',NULL,'2026-06-17 15:19:29'),(3,1,'SOLICITUD','EN REVISIÓN',NULL,'2026-06-18 15:23:50'),(4,1,'SOLICITUD','EN REVISIÓN',NULL,'2026-06-18 15:24:26'),(5,1,'SOLICITUD','EN REVISIÓN',NULL,'2026-06-18 15:24:43'),(6,1,'EN_EVALUACION','COTIZACION',NULL,'2026-06-18 15:25:54'),(7,1,'COTIZACION','PRESUPUESTADO',NULL,'2026-06-18 15:50:28'),(8,2,NULL,'NUEVA','Ingreso automatizado desde Webhook WordPress.','2026-06-30 11:50:48'),(9,2,'NUEVA','PENDIENTE_TECNICOS',NULL,'2026-06-30 19:28:17'),(10,2,'SOLICITUD','EN REVISIÓN',NULL,'2026-06-30 19:29:37'),(11,2,'SOLICITUD','EN REVISIÓN',NULL,'2026-06-30 20:28:11'),(12,2,'EN_EVALUACION','COTIZACION',NULL,'2026-06-30 20:43:25'),(13,2,'COTIZACION','PRESUPUESTADO',NULL,'2026-06-30 20:45:31'),(14,2,'PRESUPUESTADO','PAGO_ENVIADO',NULL,'2026-06-30 20:46:57'),(15,1,'PRESUPUESTADO','PAGO_ENVIADO',NULL,'2026-06-30 22:42:43'),(16,2,'PAGO_ENVIADO','EN_PRODUCCION',NULL,'2026-06-30 23:57:44'),(17,2,'EN_PRODUCCION','LISTO_PARA_ENTREGA',NULL,'2026-07-01 00:06:11'),(18,2,'LISTO_PARA_ENTREGA','AUDITORIA_PAGO_REGISTRADA',NULL,'2026-07-01 00:07:20'),(19,2,'LISTO_PARA_ENTREGA','ENTREGADO',NULL,'2026-07-01 00:08:56'),(20,1,'PAGO_ENVIADO','EN_PRODUCCION',NULL,'2026-07-01 00:09:25'),(21,1,'EN_PRODUCCION','LISTO_PARA_ENTREGA',NULL,'2026-07-01 00:09:29'),(22,1,'LISTO_PARA_ENTREGA','ENTREGADO',NULL,'2026-07-01 00:09:39');
/*!40000 ALTER TABLE `historial_estados` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pagos`
--

DROP TABLE IF EXISTS `pagos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pagos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pedido_id` bigint NOT NULL,
  `monto` double NOT NULL,
  `fecha_pago` datetime DEFAULT CURRENT_TIMESTAMP,
  `metodo_pago` varchar(100) NOT NULL,
  `referencia_comprobante` varchar(1000) DEFAULT NULL,
  `concepto` varchar(255) DEFAULT 'Abono',
  `drive_file_id` varchar(255) DEFAULT NULL,
  `origen_registro` varchar(50) DEFAULT 'CLIENTE',
  PRIMARY KEY (`id`),
  KEY `fk_pago_pedido` (`pedido_id`),
  CONSTRAINT `fk_pago_pedido` FOREIGN KEY (`pedido_id`) REFERENCES `pedidos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pagos`
--

LOCK TABLES `pagos` WRITE;
/*!40000 ALTER TABLE `pagos` DISABLE KEYS */;
INSERT INTO `pagos` VALUES (1,2,100000,'2026-07-01 02:36:41','Transferencia Bancaria',NULL,'? Validación de Abono1',NULL,'CLIENTE'),(2,2,100000,'2026-07-01 03:59:02','Efectivo / Presencial','https://drive.google.com/file/d/1eQbtvckL48QUigy6FWrjrJ7420E1CPoG/view?usp=drivesdk','? Comprobante de Abono Inicial','1eQbtvckL48QUigy6FWrjrJ7420E1CPoG','CLIENTE'),(3,2,9800000,'2026-07-01 04:08:33','Transferencia Bancaria',NULL,'? Comprobante de Pago Final',NULL,'CLIENTE'),(4,1,1000000,'2026-07-01 04:09:11','Transferencia Bancaria',NULL,'? Pago Final / Liquidación (100%)',NULL,'CLIENTE');
/*!40000 ALTER TABLE `pagos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pedidos`
--

DROP TABLE IF EXISTS `pedidos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pedidos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cliente_id` bigint NOT NULL,
  `codigo_seguimiento` varchar(100) NOT NULL,
  `servicio_solicitado` varchar(255) DEFAULT NULL,
  `tiene_archivo_inicial` tinyint(1) DEFAULT '0',
  `link_archivo_inicial` varchar(1000) DEFAULT NULL,
  `mensaje_original` text,
  `origen_contacto` varchar(100) DEFAULT NULL,
  `estado_actual` varchar(100) DEFAULT 'NUEVA',
  `precio_final` double DEFAULT NULL,
  `link_comprobante_pago` varchar(1000) DEFAULT NULL,
  `link_comprobante_envio` varchar(1000) DEFAULT NULL,
  `fecha_entrega_estimada` date DEFAULT NULL,
  `mensaje_triage` text,
  `fecha_vencimiento_presupuesto` date DEFAULT NULL,
  `activo` tinyint(1) DEFAULT '1',
  `pedido_padre_id` bigint DEFAULT NULL,
  `es_garantia` tinyint(1) DEFAULT '0',
  `fecha_creacion` datetime DEFAULT CURRENT_TIMESTAMP,
  `fecha_entrega_real` date DEFAULT NULL,
  `descuento_porcentaje` double DEFAULT '0',
  `precio_original` double DEFAULT '0',
  `anotaciones_internas` text,
  `desglose_costos` text,
  `notas_auditoria` text,
  `justificacion_cliente` text,
  `resumen_financiero_operador` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pedido_codigo` (`codigo_seguimiento`),
  KEY `fk_pedido_cliente` (`cliente_id`),
  CONSTRAINT `fk_pedido_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `clientes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pedidos`
--

LOCK TABLES `pedidos` WRITE;
/*!40000 ALTER TABLE `pedidos` DISABLE KEYS */;
INSERT INTO `pedidos` VALUES (1,1,'LEG-43F4','Impresión 3D',1,'https://drive.google.com/file/d/1paH4kXWyLQHlNMa2NvlNlsJ3dXHHmp10/view?usp=drive_link','Hola, necesito imprimir esta pieza en PLA negro. Son 5 unidades y me gustaría saber el tiempo estimado.','Instagram','ENTREGADO',1000000,'https://drive.google.com/file/d/1jawPY4QUQ48fNr0l7873GT-x48qa2TGv/view?usp=drivesdk','11111111','2026-06-19','Pieza finalizada en producción.','2026-07-09',1,NULL,0,'2026-06-17 19:13:03',NULL,0,1000000,'411111',NULL,NULL,NULL,NULL),(2,1,'LEG-F9CA','Impresión 3D',0,NULL,NULL,'Manual Luis','ENTREGADO',10000000,'https://drive.google.com/file/d/1_TYD7srrsrNW3R-ctEiTjddqtuNr943s/view?usp=drivesdk','111','2026-07-11','Comprobante cargado por el cliente desde el portal.','2026-07-21',1,NULL,0,'2026-06-30 15:50:49',NULL,10,11111111,'gggggggggggggggggggg',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `pedidos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'ROLE_ADMIN'),(2,'ROLE_USER');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `activo` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios`
--

LOCK TABLES `usuarios` WRITE;
/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */;
/*!40000 ALTER TABLE `usuarios` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuarios_roles`
--

DROP TABLE IF EXISTS `usuarios_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios_roles` (
  `usuario_id` bigint NOT NULL,
  `rol_id` bigint NOT NULL,
  PRIMARY KEY (`usuario_id`,`rol_id`),
  KEY `fk_ur_rol` (`rol_id`),
  CONSTRAINT `fk_ur_rol` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios_roles`
--

LOCK TABLES `usuarios_roles` WRITE;
/*!40000 ALTER TABLE `usuarios_roles` DISABLE KEYS */;
/*!40000 ALTER TABLE `usuarios_roles` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-01 11:03:24
