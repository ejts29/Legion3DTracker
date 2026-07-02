-- 1. Seleccionar explícitamente la base de datos
USE dblegion3dtracker;

-- 2. Desactivar la revisión de claves foráneas para permitir el borrado
SET FOREIGN_KEY_CHECKS = 0;

-- 3. Vaciar las tablas en orden para evitar errores de restricción
TRUNCATE TABLE `historial_estados`;
TRUNCATE TABLE `pagos`;
TRUNCATE TABLE `detalles_tecnicos`;
TRUNCATE TABLE `pedidos`;
TRUNCATE TABLE `clientes`;
TRUNCATE TABLE `usuarios_roles`;
TRUNCATE TABLE `usuarios`;
TRUNCATE TABLE `roles`;

-- 4. Reactivar la revisión de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;