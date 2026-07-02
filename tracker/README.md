# Legión 3D Tracker - Middleware (Software de mediación) Logístico y Operativo

    **Legión 3D Tracker** es un sistema de Middleware (Software de mediación) de Gestión Logística y Operativa diseñado a medida para centralizar, automatizar y auditar el ciclo de vida de los proyectos en un taller de impresión 3D comercial.

-----

## El Problema vs.  La Solución

**El Problema de Negocio:**
Previo a la implementación de este sistema, la operación del taller sufría de fragmentación en las comunicaciones. Los clientes solicitaban cotizaciones vía web, pero el seguimiento, la recepción de archivos pesados (modelos 3D), la validación de pagos y el envío de comprobantes de despacho se realizaban manualmente mediante una mezcla caótica de correos electrónicos y mensajes de WhatsApp. Esto generaba cuellos de botella administrativos, pérdida de información y tiempos de respuesta lentos.

**La Solución (Legión 3D Tracker):**
Se desarrolló un sistema centralizado que actúa como el "cerebro" logístico del taller. El Tracker captura los datos iniciales de forma silenciosa, unifica la comunicación técnica y financiera, automatiza la notificación de estados mediante correos auditados y proporciona una trazabilidad total State Machine(o Maquina de estados) desde la idea inicial hasta el despacho físico o digital de la pieza.

-----

## Cómo funciona: El Ciclo de Vida del Pedido

El sistema modela un flujo de negocio estricto que protege la integridad operativa:

* **1. Captura (Webhook):** El cliente llena un formulario en la web de WordPress. Sin que el cliente abandone la página, los datos viajan al Tracker o Rastreador.
* **2. Triage (Revisión):** El taller recibe el proyecto y revisa la viabilidad técnica. Si no es factible, se rechaza y el cliente es notificado automáticamente.
* **3. Presupuestado:** Se envía una cotización formal al cliente (con costo y fecha estimada) junto con adjuntos técnicos.
* **4. Pago Enviado:** El cliente, a través de su enlace único de seguimiento, notifica el pago.
* **5. En Producción:** El taller valida el pago y mueve el proyecto a las máquinas de impresión.
* **6. Despachado:** El taller finaliza el proceso. El sistema discrimina inteligentemente si es una entrega **Física** (generando correos con códigos de Starken) o **Digital** (enviando accesos directos de Drive/Nube) Todo esto se hace De manera automática Envíos de correos Cambios de Estado Notificaciones de pago ETC.

-----

## Stack Tecnológico

El proyecto ha sido construido utilizando estándares de la industria para asegurar escalabilidad y mantenibilidad:

* **Backend:** Java 17+, Spring Boot 3.x, Maven.
* **Frontend:** HTML5, CSS3, JavaScript (Vanilla para asincronismo mediante Fetch API), Bootstrap 5 (UI/UX Responsivo), Thymeleaf (Renderizado *Server-Side*).
* **Base de Datos:** MySQL 8. Base de datos relacional robusta (tablas principales: `pedidos`, `clientes`, `detalles_tecnicos`, `historial_estados`) modelada en MySQL Workbench.
* **Testing & API:** Postman (Validación de integraciones REST y Webhooks).

-----

## Análisis de Arquitectura y Código Fuente

El sistema se basa en un patrón arquitectónico **MVC (Modelo-Vista-Controlador)** con un diseño multicapa. A continuación, se detallan los hitos arquitectónicos del proyecto:

### 1\. La Integración Desacoplada con WordPress (Webhook)

**Regla de Negocio:** *Prohibido alterar o modificar la Landing Page comercial en WordPress.*
Para respetar esta restricción, se implementó una arquitectura orientada a eventos. El formulario nativo de WordPress actúa como un *emisor* que dispara un JSON. Nuestro `WebhookController.java` actúa como un *receptor* (Endpoint API) que captura la data de forma asíncrona, creando el registro inicial sin exponer la lógica de base de datos.

```java
// Snippet: cl.ipss.legion3d.tracker.backend.controladores.api.WebhookController
@PostMapping("/nuevo-lead")
public ResponseEntity<String> recibirLeadWordPress(@RequestBody WebhookLeadDTO leadInfo) {
    try {
        // El servicio procesa el DTO y ensambla el Cliente y el Pedido
        pedidoService.crearPedidoDesdeWebhook(leadInfo);
        return ResponseEntity.ok("Lead procesado e ingresado al Triage exitosamente");
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error en el procesamiento");
    }
}
```

### 2\. Seguridad y Control de Acceso (Interceptor)

Para evitar la complejidad innecesaria de Spring Security (dado que es un sistema de uso interno para un solo operador), se diseñó un esquema de autenticación ágil protegido por un filtro de intercepción a nivel de Servlet (`SecurityInterceptor.java`). Esto bloquea cualquier petición no autorizada a las rutas de gestión.

```java
// Snippet: cl.ipss.legion3d.tracker.backend.config.SecurityInterceptor
@Component
public class SecurityInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        // Bloqueo estricto: Si no hay sello de sesión de administrador, se expulsa al login
        if (session.getAttribute("admin_session") == null) {
            response.sendRedirect("/login");
            return false; // Detiene la propagación del Request
        }
        return true;
    }
}
```

### 3\. Asincronismo y Experiencia de Usuario (Frontend)

El proyecto utiliza Thymeleaf para la carga inicial de vistas, pero delega las interacciones complejas (cambios de estado, subida de archivos) a JavaScript puro mediante la API Fetch (peticion). Esto evita el refresco de la página. Se incluye protección de "Primera Línea" para evitar colapsar el servidor con archivos excesivamente grandes.

```javascript
// Snippet: src/main/resources/static/js/main.js
// Validación en tiempo real del límite de carga (Payload)
if (estado === "PRESUPUESTADO" && inputArchivo && inputArchivo.files.length > 0) {
    const tamañoEnMB = inputArchivo.files[0].size / (1024 * 1024);
    // El servidor Spring Boot tiene un límite de 20MB. Frenamos la petición en el navegador.
    if (tamañoEnMB > 20) {
        alert(" El archivo es demasiado pesado (" + tamañoEnMB.toFixed(1) + " MB). El límite máximo es 20 MB.");
        return; // Interrupción temprana
    }
}
```

-----

## Estructura de Carpetas (Paquetes)

El código fuente está rigurosamente organizado por dominio de responsabilidad:

* **`cl.ipss.legion3d.tracker.backend.config/`**: Configuraciones globales, inyección de dependencias web y el Interceptor de Seguridad.
* **`cl.ipss.legion3d.tracker.backend.controladores/`**: Subdividido lógicamente en `api` (Endpoints REST para Webhooks y Fetch de JS) y `web` (Controladores clásicos que devuelven vistas Thymeleaf).
* **`cl.ipss.legion3d.tracker.backend.dtos/`**: *Data Transfer Objects* para recibir cargas útiles del cliente sin exponer las entidades de la base de datos.
* **`cl.ipss.legion3d.tracker.backend.entidades/`** y **`repositorios/`**: Mapeo relacional JPA/Hibernate de la base de datos y sus interfaces CRUD (Spring Data JPA).
* **`cl.ipss.legion3d.tracker.backend.servicios/`**: Capa transaccional que contiene el Core de la lógica de negocio (ej. cálculos de fechas hábiles, gestión SMTP).
* **`src/main/resources/`**: Contiene los archivos estáticos (CSS, JS) y las plantillas HTML (Thymeleaf) subdivididas en vistas de `clientes` y `solicitudes`.

-----

-----

## Estructura de Carpetas
LEGION3DTRACKER
+---.github
|   \---java-upgrade
|       \---hooks
|           \---scripts
+---.vscode
\---tracker
    +---.mvn
    |   \---wrapper
    +---scratch
    +---src
    |   +---main
    |   |   +---java
    |   |   |   \---cl
    |   |   |       \---ipss
    |   |   |           \---legion3d
    |   |   |               \---tracker
    |   |   |                   \---backend
    |   |   |                       +---config
    |   |   |                       +---controladores
    |   |   |                       |   +---api
    |   |   |                       |   \---web
    |   |   |                       +---dtos
    |   |   |                       +---entidades
    |   |   |                       +---enumeraciones
    |   |   |                       +---excepciones
    |   |   |                       +---repositorios
    |   |   |                       \---servicios
    |   |   \---resources
    |   |       +---static
    |   |       |   +---css
    |   |       |   +---img
    |   |       |   \---js
    |   |       \---templates
    |   |           +---clientes
    |   |           +---layout
    |   |           \---solicitudes
    |   \---test
    |       \---java
    |           \---cl
    |               \---ipss
    |                   \---legion3d
    |                       \---tracker
    \---target
        +---classes
        |   +---cl
        |   |   \---ipss
        |   |       \---legion3d
        |   |           \---tracker
        |   |               \---backend
        |   |                   +---config
        |   |                   +---controladores
        |   |                   |   +---api
        |   |                   |   \---web
        |   |                   +---dtos
        |   |                   +---entidades
        |   |                   +---enumeraciones
        |   |                   +---excepciones
        |   |                   +---repositorios
        |   |                   \---servicios
        |   +---static
        |   |   +---css
        |   |   +---img
        |   |   \---js
        |   \---templates
        |       +---clientes
        |       +---layout
        |       \---solicitudes
        +---generated-sources
        |   \---annotations
        +---generated-test-sources
        |   \---test-annotations
        +---maven-status
        |   \---maven-compiler-plugin
        |       +---compile
        |       |   \---default-compile
        |       \---testCompile
        |           \---default-testCompile
        \---test-classes
            \---cl
                \---ipss
                    \---legion3d
                        \---tracker
-----

## ⚙️ Instalación y Puesta en Marcha

Para desplegar este entorno en desarrollo local:

1. **Clonar Repositorio:** Descarga el código fuente.
2. **Preparar Base de Datos:** En MySQL, ejecuta el script `dblegion3dtracker.sql` proporcionado en la raíz para generar la estructura de tablas relacionales.
3. **Configurar Variables de Entorno:** Navega a `src/main/resources/application.properties` y configura las credenciales locales:
      * `spring.datasource.url` / `username` / `password`
      * `spring.mail.username` / `password` (Credenciales SMTP o App Password de Gmail).
      * `app.admin.password` (Clave del interceptor de seguridad).
4. **Ejecutar Build:** Compila e inicia el servidor de Tomcat embebido.
      * Windows: `mvnw.cmd spring-boot:run`
      * Mac/Linux: `./mvnw spring-boot:run`
5. **Acceso:** Ingresa a `http://localhost:8080/login`

-----

## 🧪 Control de Calidad (QA) y Pruebas E2E

El software superó con éxito un Plan de Pruebas de Estrés y Flujo Crítico (E2E), demostrando solidez técnica frente a casos de borde:

* **Manejo Atómico de Correos (Falla Silenciosa):** El sistema garantiza la atomicidad; si una excepción en el protocolo SMTP ocurre por falta de red, la base de datos efectúa un *rollback* de estado, impidiendo "pedidos fantasmas".
* **Sanitización de Datos de Usuario:** Los scripts de Thymeleaf (`#strings.replace()`) depuran símbolos inválidos en números telefónicos para la construcción de la API de WhatsApp (`wa.me/`).
* **Protección Anti-Bypass Asíncrona:** Validado el rechazo del Tomcat frente a ingresos directos por URL a la zona de administración.

-----

## Créditos
* **Creadopor:** Efren Tovar Silva.
* **Creadopor:** Daniel Castro Troncoso
* **Creadopor:** Jeremy Sanhueza Gutiérrez
