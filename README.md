
---

# Sistema Legión 3D Tracker

Proyecto desarrollado como parte de la Asignatura **Taller de Integración Técnico** para la obtención del Título Profesional de Técnico En Programación Y Análisis De Sistemas.

El sistema es una plataforma web tipo *middleware* desarrollada para la empresa **Diseño y Prototipado SPA (Legión 3D)**. Su función principal es resolver el cuello de botella operativo en la captura de requerimientos técnicos de impresión 3D, conectando la captación de prospectos en WordPress con un panel administrativo privado robusto para automatizar cotizaciones, estados de producción y comunicación con el cliente.

### La Problemática

Originalmente, el cliente llenaba un formulario de texto libre en WordPress con ideas vagas ("quiero imprimir una pieza"). Esto obligaba al administrador del taller a perseguir al cliente por WhatsApp para obtener dimensiones, materiales y resistencias mecánicas, generando un flujo de datos desordenado y pérdida de tiempo.

---

## Objetivo

Entregar una aplicación web que permita:

* **Capturar prospectos automáticamente:** Recibir solicitudes desde WordPress mediante un Webhook sin alterar el SEO de la página original.
* **Triaje y Máquina de Estados:** Validar la factibilidad de impresión y mover el pedido por 9 estados inmutables (Nueva, Triaje, Cotización, Producción, Entregado, etc.).
* **Portal del Cliente:** Obligar al cliente a ingresar datos técnicos exactos (X, Y, Z, Material, Tolerancias) mediante un enlace único.
* **Automatización de Notificaciones:** Enviar correos electrónicos automáticos (API de Gmail) en cada cambio de fase operativa.
* **Almacenamiento Zero-DB:** Delegar la carga de archivos pesados (STL, OBJ, PDF) al servidor de correo electrónico para mantener la base de datos ligera.

---

## Tecnologías utilizadas

* **Java 21**
* **Spring Boot 3.5.13**
* **Spring Web (REST + MVC)**
* **Spring Data JPA & Hibernate**
* **Spring Security** (Protección de panel administrativo)
* **Thymeleaf + Bootstrap 5.3.2** (Renderizado del lado del servidor y UI responsiva)
* **JavaScript Vanilla & Fetch API** (Radar asíncrono cada 15 seg. y utilidades)
* **MySQL 8.0**
* **Gmail API / JavaMailSender** (Notificaciones automáticas)
* **Lombok & Maven**
* **Postman & JMeter** (Pruebas de estrés y QA)

---

## Arquitectura general

El proyecto sigue una arquitectura MVC con estricta separación de responsabilidades:

* `backend/`
* `config/` → Filtros de Spring Security y configuración de Swagger.
* `controladores/api/` → Webhooks que reciben el JSON desde WordPress.
* `controladores/web/` → Controladores Thymeleaf para el Dashboard y Cliente.
* `dtos/` → Objetos de transferencia para validar datos del formulario.
* `entidades/` → Entidades JPA (Pedido, Cliente, DetallesTecnicos, HistorialEstado).
* `servicios/` → Lógica de la máquina de estados y envíos de correo.


* `resources/`
* `static/` → CSS global, iconos y scripts JS (`main.js` para el radar).
* `templates/` → Vistas HTML (Módulo de triaje, cotizaciones, historial, portal del cliente).



---

## Despliegue en la Nube ☁️

El sistema se encuentra desplegado utilizando infraestructura de alta disponibilidad a costo cero (Serverless):

🔗 **[Acceso a la Aplicación Desplegada (Legión 3D Tracker)](https://legion3dtracker-kwa7.onrender.com)**

* **Backend / Servidor:** Alojado en **Render.com**. Render detecta los *commits* en la rama `main` de GitHub y realiza el despliegue continuo de manera automática (CI/CD).
* **Base de Datos:** Alojada en **Aiven.io**. Instancia administrada de MySQL 8.0 que centraliza la persistencia relacional del taller de forma segura.

---

## Roles y seguridad

La seguridad se implementa con Spring Security:

* `ROLE_ADMIN`
* Acceso total al Dashboard interno (`/dashboard`, `/solicitudes/**`).
* Autenticación tradicional mediante formulario de login.


* `CLIENTE (Acceso por Token URL)`
* No requiere contraseña. Accede a su expediente únicamente a través del enlace dinámico generado por el sistema: `/tracking/{codigoSeguimiento}`.



### Usuarios de demostración

**Administrador (Dueño del Taller)**

* **Usuario:** `pedidolegion3d@gmail.com`
* **Clave:** `123`

*(Al ingresar, se visualizan los contadores, el historial de facturación y las tarjetas operativas).*

---

## Requisitos previos (Entorno Local)

* JDK 21
* Maven 3.9+ (o usar `./mvnw` incluido)
* MySQL 8.0 local instalado
* IDE recomendado: VS Code / IntelliJ / Eclipse

---

## Configuración de la base de datos (Local)

1. Crear una base de datos vacía en MySQL llamada:
```sql
CREATE DATABASE DBLegion3DTracker
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

```


2. Revisar/editar el archivo:
`src/main/resources/application.properties`
Asegúrate de ajustar tu usuario y clave local. El sistema generará las tablas automáticamente gracias a JPA (Asegúrate de que `ddl-auto` esté en `update` para el primer inicio).
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/DBLegion3DTracker?serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root

```



---

## Ejecución del proyecto

Desde la carpeta raíz del proyecto:

### Opción 1: Maven Wrapper

```bash
./mvnw spring-boot:run
# En Windows
mvnw.cmd spring-boot:run

```

### Opción 2: JAR ejecutable

```bash
./mvnw clean package -DskipTests
java -jar target/tracker-0.0.1-SNAPSHOT.jar

```

La aplicación quedará disponible en:

* **Backend / Frontend:** `http://localhost` (o el puerto 8080 si corres localmente).

---

## Endpoints principales y Flujo Operativo

El sistema opera bajo un flujo híbrido (API + Vistas Web):

### 1. Ingesta de Datos (El Webhook)

* `POST /api/v1/webhook/nuevo-lead`
* Recibe el JSON disparado desde Contact Form 7 (WordPress).
* Crea el Cliente, inicializa el Pedido en estado `NUEVA` y activa la alerta roja en el Radar del Dashboard.



### 2. Triaje y Panel Admin (MVC)

* `GET /dashboard` → Vista principal y métricas de KPIs.
* `GET /solicitudes/triaje` → Tabla dinámica con buscador asíncrono para aprobar o rechazar ideas vagas.
* `POST /solicitudes/actualizar-estado-flujo` → Cambia el estado del pedido, guarda el registro en el `HistorialEstado` y dispara un Email SMTP asíncrono al cliente.

### 3. Portal del Cliente

* `GET /tracking/{id}` → Línea de tiempo visual. Muestra la fase actual (Evaluación, Cotización, Producción).
* `POST /seguimiento/guardar-datos` → Formulario 2. El cliente envía las medidas exactas (X, Y, Z), material (PETG, PLA, ABS) y enlaces a Google Drive con el STL.

---

## Pruebas con Postman

En la raíz del proyecto (carpeta `V0_DOCUMENTACION_PROYECTO/Pruebas_y_QA`) se incluye la colección:

* `postman_collection_Legion3D_Tracker_FINAL_Completa.json`

### Para usarla:

1. Importar a Postman.
2. Ejecutar la carpeta **"Escenario 1: Flujo Normal"** para simular la llegada de un Webhook desde WordPress.
3. El sistema creará los pedidos de prueba en la base de datos para que los gestiones en el panel administrativo.
*(Nota: El sistema fue sometido a pruebas de estrés con 100 usuarios concurrentes, logrando 0% de error).*

---

## Créditos

Proyecto desarrollado por:

* **Efren Tovar Silva** 

Empresa Beneficiaria: **Diseño y Prototipado SPA (Legión 3D) - Luis Lobo.**
