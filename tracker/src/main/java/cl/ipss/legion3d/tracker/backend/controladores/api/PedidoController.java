package cl.ipss.legion3d.tracker.backend.controladores.api;
// el package indica la ubicación de esta clase dentro del proyecto

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import cl.ipss.legion3d.tracker.backend.dtos.*;
import cl.ipss.legion3d.tracker.backend.entidades.*;
import cl.ipss.legion3d.tracker.backend.repositorios.*;
import cl.ipss.legion3d.tracker.backend.servicios.EmailService;
import cl.ipss.legion3d.tracker.backend.servicios.PedidoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import cl.ipss.legion3d.tracker.backend.servicios.PagoService;
import cl.ipss.legion3d.tracker.backend.servicios.GoogleDriveService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
//import java.util.concurrent.CompletableFuture;
//import java.text.DecimalFormat;
//import java.time.format.DateTimeFormatter;

// import: Trae las librerías necesarias para usar clases de otros paquetes sin escribir su ruta completa.

/**
 * PROPÓSITO DE LA CLASE
 * Esta clase funciona como un controlador REST especializado en la gestión
 * operativa de los pedidos.
 * Su labor central consiste en procesar los cambios de estado en la máquina de
 * estados,
 * recibir la información técnica de las piezas y coordinar el envío de
 * notificaciones automáticas
 * por correo electrónico hacia los clientes.
 */

// DEFINICIÓN DEL CONTROLADOR Y RUTAS
// Se utilizan las anotaciones @RestController y @RequestMapping para establecer
// que la clase
// es un componente de entrada en el ecosistema de Spring Boot, organizando la
// ruta base para acceder a los endpoints.
@RestController
@RequestMapping("/api/v1/pedidos")
// DOCUMENTACIÓN Y ORGANIZACIÓN
// Se incluye la etiqueta @Tag de Swagger para describir y categorizar las
// funciones de la API
// dentro de la documentación técnica, facilitando su lectura y navegación.
@Tag(name = "Gestión de Pedidos", description = "Operaciones principales para el ciclo de vida del pedido, desde la recepción técnica hasta la entrega final.")
public class PedidoController {

    // INYECCIÓN DE DEPENDENCIAS
    // La anotación @Autowired se emplea para vincular automáticamente los servicios
    // y repositorios
    // que el controlador necesita para funcionar (interactuar con la base de datos
    // y enviar correos)
    // sin requerir instanciaciones manuales.
    @Autowired
    private PedidoRepository pedidoRepo;
    @Autowired
    private HistorialEstadoRepository historialRepo;
    @Autowired
    private DetallesTecnicosRepository detallesRepo;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ClienteRepository clienteRepo;
    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private PagoService pagoService;
    @Autowired
    private GoogleDriveService driveService;

    // ETAPA 2 y ETAPA 7: CAMBIOS DE ESTADO SIMPLES Y TRIAGE
    // la etapa 2 se refiere a los cambios de estado que realiza Luis para avanzar
    // el pedido, mientras que la etapa 7 se enfoca en el triage técnico que realiza
    // el
    // equipo para evaluar la factibilidad del proyecto. Ambos procesos implican
    // actualizar
    // el estado del pedido y enviar notificaciones por correo electrónico al
    // cliente según corresponda.

    // @Operation y @ApiResponses son anotaciones de Swagger que se utilizan para
    // documentar los endpoints de la API.

    @Operation(summary = "Cambiar estado del pedido", description = "Actualiza el estado actual de un pedido y dispara notificaciones automáticas al cliente según la transición.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe")
    })
    /**
     * ENDPOINT: Cambiar Estado (PATCH)
     * Este método es el motor de la máquina de estados del sistema. Recibe un ID y
     * un DTO
     * con el nuevo estado. Actualiza la base de datos, registra el historial de
     * auditoría
     * y dispara notificaciones por correo electrónico personalizadas según la fase.
     */

    // El endpoint es un PATCH porque estamos actualizando parcialmente el recurso
    // (solo el estado y mensaje de triage).
    // Recibe el ID del pedido a modificar como PathVariable y un DTO con el nuevo
    // estado y mensaje de triage en el cuerpo de la solicitud.
    // El método busca el pedido por ID, valida la transición de estado para evitar
    // saltos lógicos, actualiza el estado, guarda el historial y envía correos
    // según el nuevo estado.La respuesta es un mensaje de éxito o error dependiendo
    // del resultado de la operación.

    // @patchmapping @PathVariable @RequestBody @Valid @Parameter son anotaciones de
    // Spring y Swagger que se utilizan para definir el endpoint, los parámetros de
    // la solicitud y su validación, así como para documentar la API de manera clara
    // y estructurada.

    @PatchMapping("/{id}/estado")
    public ResponseEntity<String> cambiarEstado(
            @Parameter(description = "ID del pedido a modificar", example = "1") @PathVariable Long id,
            @Parameter(description = "Objeto con el nuevo estado y mensaje de triage") @Valid @RequestBody CambioEstadoDTO dto) {

        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty())
            return ResponseEntity.status(404).body("Error: El pedido no existe.");

        Pedido pedido = pedidoOpt.get();
        String estadoAntiguo = pedido.getEstadoActual();

        // Delegamos el cambio de estado al servicio protegido (Tarea 2)
        try {
            pedido = pedidoService.actualizarEstado(id, dto.getNuevoEstado());
        } catch (IllegalStateException e) {
            // Si el servicio bloquea la transición por reglas de negocio, informamos el
            // conflicto
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(e.getMessage());
        }

        if (dto.getMensajeTriage() != null) {
            pedido.setMensajeTriage(dto.getMensajeTriage());
            pedidoRepo.save(pedido); // Actualizamos solo el mensaje si cambió
        }

        // Creación automática de terreno técnico
        if (dto.getNuevoEstado().equals("PENDIENTE_TECNICOS") || dto.getNuevoEstado().equals("COTIZACION")) {
            DetallesTecnicos detalles = pedido.getDetallesTecnicos();
            if (detalles == null) {
                detalles = new DetallesTecnicos();
                detalles.setPedido(pedido);
                detalles.setActivo(true);
                detallesRepo.save(detalles);
                pedido.setDetallesTecnicos(detalles);
            } else {
                detalles.setActivo(true);
                detallesRepo.save(detalles);
            }
        }

        // Ya no es necesario pedidoRepo.save(pedido) ni guardarHistorial aquí
        // pues el Service ya lo hizo de forma blindada.

        String correoCliente = pedido.getCliente().getEmail();
        String nombreCliente = pedido.getCliente().getNombre();

        // -----------------------------------------------------------
        // ETAPA 2: TRIAJE APROBADO (Solicitud de Ingeniería)
        // -----------------------------------------------------------
        if (dto.getNuevoEstado().equals("PENDIENTE_TECNICOS")) {
            String msj = "Hola " + nombreCliente
                    + ",\n\n¡Excelentes noticias! Hemos evaluado tu solicitud y es completamente factible.\n"
                    + "Para poder generar tu cotización formal, necesitamos que nos indiques las medidas y especificaciones técnicas.\n\n"
                    + "Mensaje del taller: "
                    + (dto.getMensajeTriage() != null ? dto.getMensajeTriage() : "Todo en orden para avanzar.") + "\n\n"
                    + "➡️ Ingresa aquí para llenar el formulario técnico de tu pieza: \nhttp://localhost:8080/tracking/"
                    + pedido.getCodigoSeguimiento() + "/formulario\n"
                    + obtenerFirmaCorporativa();
            try {
                emailService.enviarCorreoSimple(correoCliente, "🎉 Tu proyecto es factible", msj);
            } catch (Exception e) {
                System.err.println("Error enviando correo de aprobación técnica: " + e.getMessage());
            }
        }

        // -----------------------------------------------------------
        // ETAPA 2: TRIAJE RECHAZADO
        // -----------------------------------------------------------
        else if (dto.getNuevoEstado().equals("RECHAZADA")) {
            String motivo = dto.getMensajeTriage() != null ? dto.getMensajeTriage()
                    : "Inviabilidad técnica o capacidad de producción superada.";
            String msj = "Estimado/a " + nombreCliente + ",\n\n"
                    + "Agradecemos tu interés en Legión 3D.\n\n"
                    + "Tras una evaluación técnica, lamentamos informarte que en esta oportunidad no podremos fabricar tu pieza.\n"
                    + "Motivo: 👉 " + motivo + "\n\n"
                    + "Agradecemos tu comprensión."
                    + obtenerFirmaCorporativa();
            try {
                emailService.enviarCorreoSimple(correoCliente, "🟠 Actualización sobre tu solicitud", msj);
            } catch (Exception e) {
                System.err.println("Error enviando correo de rechazo: " + e.getMessage());
            }
        }

        // -----------------------------------------------------------
        // ETAPA 8: PRODUCTO FABRICADO (Control de Calidad OK)
        // -----------------------------------------------------------
        else if (dto.getNuevoEstado().equals("LISTO_PARA_ENTREGA")) {
            Optional<DetallesTecnicos> detallesOpt = detallesRepo.findByPedidoId(pedido.getId());
            String metodoElegido = detallesOpt.isPresent() ? detallesOpt.get().getMetodoEntrega() : "";
            String servicio = pedido.getServicioSolicitado() != null ? pedido.getServicioSolicitado().toLowerCase()
                    : "";
            String articulo = (servicio.contains("diseño") || servicio.contains("ingeniería")) ? "proyecto" : "pieza";
            String icono = (servicio.contains("diseño") || servicio.contains("ingeniería")) ? "💻" : "📦";

            String msjListo;

            Double saldoNeto = pagoService.calcularSaldoPendiente(pedido.getId());
            Double saldoBruto = Math.round(saldoNeto * 1.19 * 100.0) / 100.0;
            java.text.DecimalFormat formateaMoneda = new java.text.DecimalFormat("###,###");

            if (saldoBruto > 5.0) {
                // ESCENARIO A: PIEZA TERMINADA FÍSICAMENTE, PERO CON DEUDA PENDIENTE
                msjListo = "Hola " + nombreCliente + ",\n\n"
                        + "¡Tenemos excelentes noticias! La fabricación de tu " + articulo
                        + " ha concluido con éxito y ya está físicamente terminad@ en nuestro taller " + icono + ".\n\n"
                        + "Como acordamos, para poder liberar la pieza y proceder con el despacho (o retiro presencial), requerimos la confirmación del pago de tu saldo final.\n\n"
                        + "💰 SALDO FINAL PENDIENTE: $" + formateaMoneda.format(saldoBruto) + "\n\n"
                        + "➡️ Por favor, ingresa a tu portal seguro para notificar este pago:\n"
                        + "http://localhost:8080/tracking/" + pedido.getCodigoSeguimiento() + "\n\n"
                        + "Apenas administración verifique el pago, la pieza será liberada de inmediato. ¡Gracias por confiar en Legión 3D!";

            } else {
                // ESCENARIO B: PIEZA TERMINADA Y 100% PAGADA (Vía Libre)
                if (metodoElegido.equalsIgnoreCase("Retiro en Taller")) {
                    msjListo = "Hola " + nombreCliente + ",\n\n"
                            + "¡Tu " + articulo + " ya está list@ " + icono + "!\n"
                            + "Al tener tu pedido pagado al 100%, la pieza ya se encuentra liberada. Te invitamos a acercarte a nuestro taller en Portugal 1348 para retirarla.\n\n"
                            + "📍 Mapa: https://www.google.com/maps?q=Portugal+1348,+Santiago\n\n"
                            + "¡Te esperamos!";
                } else if (metodoElegido.contains("Digital")) {
                    msjListo = "Hola " + nombreCliente + ",\n\n"
                            + "¡La fase de desarrollo ha concluido! Tu " + articulo + " digital ya está finalizado "
                            + icono + ".\n"
                            + "Estamos preparando los archivos en la nube. Muy pronto te entregaremos el enlace de descarga oficial.\n\n"
                            + "Saludos cordiales,";
                } else {
                    msjListo = "Hola " + nombreCliente + ",\n\n"
                            + "¡Tu " + articulo + " ya está list@ y empaquetad@ " + icono + "!\n"
                            + "Como tu pedido está 100% pagado, ya se encuentra liberado para logística. Estamos gestionando el despacho y pronto te enviaremos tu número de seguimiento.\n\n"
                            + "Si prefieres no esperar el envío, también puedes retirarlo en nuestro taller en Portugal 1348.\n\n"
                            + "Saludos cordiales,";
                }
            }

            try {
                emailService.enviarCorreoSimple(correoCliente,
                        "¡Tu " + articulo.toUpperCase() + " está list@! " + icono,
                        msjListo + obtenerFirmaCorporativa());
            } catch (Exception e) {
                System.err.println("Error enviando correo de producto terminado: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("Estado actualizado exitosamente a: " + dto.getNuevoEstado());
    }

    /**
     * ENDPOINT: Aprobar Pedido (PATCH)
     * Este método implementa la transición lógica hacia la etapa de cotización.
     * Realiza dos tareas críticas: actualiza el estado y asegura la existencia
     * del registro en Detalles Técnicos para que el equipo comercial pueda
     * trabajar.
     */
    @Operation(summary = "Aprobar pedido (Triage)", description = "Cambia el estado a COTIZACION y crea automáticamente el registro en detalles técnicos.")
    @PatchMapping("/{id}/aprobar")
    @Transactional
    public ResponseEntity<String> aprobarPedido(@PathVariable Long id) {
        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty())
            return ResponseEntity.status(404).body("Error: El pedido no existe.");

        Pedido pedido = pedidoOpt.get();
        String estadoAntiguo = pedido.getEstadoActual();

        // 1. Actualización de Estado (Estandarización Legión)
        pedido.setEstadoActual("COTIZACION");
        System.out.println("--- [DEBUG] APROBANDO PEDIDO ---");
        System.out.println("ID: " + id + " | Nuevo Estado: COTIZACION");

        // 2. Herencia de Datos y Sincronización de Tablas
        DetallesTecnicos detalles = pedido.getDetallesTecnicos();
        if (detalles == null) {
            detalles = new DetallesTecnicos();
            detalles.setPedido(pedido);
            detalles.setActivo(true);
            detallesRepo.save(detalles);
            pedido.setDetallesTecnicos(detalles);
            System.out.println("✅ DetallesTecnicos creados y vinculados para Pedido " + id);
        } else {
            detalles.setActivo(true);
            detallesRepo.save(detalles);
            System.out.println("✅ DetallesTecnicos existentes reactivados para Pedido " + id);
        }

        pedidoRepo.save(pedido);

        // 3. Trazabilidad Total
        guardarHistorial(pedido, estadoAntiguo, "COTIZACION");

        return ResponseEntity.ok("Pedido aprobado. Se ha habilitado la gestión técnica en el panel de cotizaciones.");
    }

    /**
     * ENDPOINT: Descargar Archivo Físico (GET)
     * Actúa como un proxy de descarga inteligente. Determina la fuente del archivo
     * (Google Drive o Link Externo) y entrega el recurso al navegador con el
     * prefijo del código de seguimiento (LEG-XXXX_) para mantener la trazabilidad
     * en el taller.
     */
    @Operation(summary = "Descargar archivo exacto con prefijo", description = "Descarga el archivo correspondiente al botón presionado: wps, tec o links. Soporta Google Drive y enlaces externos.")
    @GetMapping("/{id}/descargar")
    public void descargarArchivoFisico(
            @PathVariable Long id,
            @RequestParam(value = "origen", required = false) String origen,
            @RequestParam(value = "link", required = false) String link,
            HttpServletResponse response) throws IOException {

        System.out.println("--- [DEBUG] INICIANDO DESCARGA CON ORIGEN ---");
        System.out.println("Pedido ID: " + id);
        System.out.println("Origen solicitado: " + origen);
        System.out.println("Link recibido: " + link);

        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty()) {
            enviarErrorJson(response, 404, "PEDIDO_NO_ENCONTRADO", "El pedido solicitado no existe.");
            return;
        }

        Pedido pedido = pedidoOpt.get();
        DetallesTecnicos detalles = pedido.getDetallesTecnicos();

        String trackingCode = pedido.getCodigoSeguimiento();
        String origenSeguro = normalizarOrigenArchivo(origen);

        String fuente = null;
        String driveId = null;

        /*
         * PRIORIDAD 1:
         * Usar SIEMPRE el link exacto que viene desde el botón presionado.
         * Esto evita que WPS, TEC y LINKS descarguen el mismo archivo por fallback.
         */
        if (link != null && !link.isBlank() && !"null".equalsIgnoreCase(link)) {
            fuente = link.trim();
        }

        /*
         * PRIORIDAD 2:
         * Si no llegó link desde el frontend, usamos fallback según origen.
         */
        if (fuente == null || fuente.isBlank()) {
            if ("wps".equals(origenSeguro)) {
                fuente = pedido.getLinkArchivoInicial();

            } else if ("tec".equals(origenSeguro) && detalles != null) {
                if (detalles.getDriveFileId() != null && !detalles.getDriveFileId().isBlank()) {
                    driveId = detalles.getDriveFileId();
                } else {
                    fuente = detalles.getLinkArchivoFinal();
                }

            } else if ("links".equals(origenSeguro) && detalles != null) {
                fuente = detalles.getLinkArchivoFinal();
            }
        }

        if ((fuente == null || fuente.isBlank()) && (driveId == null || driveId.isBlank())) {
            enviarErrorJson(
                    response,
                    404,
                    "ARCHIVO_NO_CONFIGURADO",
                    "No existe archivo o enlace asociado para el origen solicitado.");
            return;
        }

        try {
            /*
             * Si la fuente es Google Drive, extraemos el ID y descargamos con
             * GoogleDriveService.
             */
            if (driveId == null && fuente != null) {
                driveId = extraerIdDeDriveFlexible(fuente);
            }

            if (driveId != null && !driveId.isBlank()) {
                descargarDesdeGoogleDrive(response, driveId, trackingCode, origenSeguro);
                return;
            }

            /*
             * Si no es Drive, intentamos descargarlo como URL externa:
             * Dropbox, Mega directo, enlaces públicos, etc.
             */
            descargarDesdeUrlExterna(response, fuente, trackingCode, origenSeguro);

        } catch (Exception e) {
            System.err.println("❌ ERROR EN DESCARGA: " + e.getMessage());

            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

            if (msg.contains("403") || msg.contains("forbidden") || msg.contains("permission")
                    || msg.contains("access")) {
                enviarErrorJson(
                        response,
                        403,
                        "ARCHIVO_PRIVADO",
                        "El archivo existe, pero no tiene permisos públicos de lectura. El cliente debe compartirlo como 'cualquier persona con el enlace puede ver'.");
            } else if (msg.contains("401") || msg.contains("unauthorized")) {
                enviarErrorJson(
                        response,
                        401,
                        "NO_AUTORIZADO",
                        "El proveedor de nube exige autorización. El cliente debe liberar el acceso público al archivo.");
            } else if (msg.contains("404") || msg.contains("not found")) {
                enviarErrorJson(
                        response,
                        404,
                        "ARCHIVO_NO_ENCONTRADO",
                        "El archivo no fue encontrado. Puede estar eliminado, movido o el enlace puede estar incompleto.");
            } else {
                enviarErrorJson(
                        response,
                        500,
                        "ERROR_DESCARGA",
                        "No fue posible descargar el archivo: " + e.getMessage());
            }
        }
    }

    private String normalizarOrigenArchivo(String origen) {
        if (origen == null || origen.isBlank()) {
            return "archivo";
        }

        String limpio = origen.trim().toLowerCase();

        if ("wps".equals(limpio))
            return "wps";
        if ("tec".equals(limpio))
            return "tec";
        if ("links".equals(limpio))
            return "links";

        return "archivo";
    }

    private String extraerIdDeDriveFlexible(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }

        /*
         * Formatos soportados:
         * https://drive.google.com/file/d/ID/view
         * https://drive.google.com/open?id=ID
         * https://drive.google.com/uc?id=ID
         * ID plano de Drive
         */
        Pattern pattern = Pattern.compile("(?:/d/|id=|folders/)([\\w-]+)");
        Matcher matcher = pattern.matcher(link);

        if (matcher.find()) {
            return matcher.group(1);
        }

        if (!link.contains("/") && link.length() > 20 && link.matches("[\\w-]+")) {
            return link;
        }

        return null;
    }

    private void descargarDesdeGoogleDrive(
            HttpServletResponse response,
            String driveId,
            String trackingCode,
            String origenSeguro) throws IOException {

        com.google.api.services.drive.model.File metadata = driveService.obtenerMetadata(driveId);
        if (metadata != null && "application/vnd.google-apps.folder".equals(metadata.getMimeType())) {
            response.sendRedirect("https://drive.google.com/drive/folders/" + driveId);
            return;
        }

        String originalName = metadata.getName() != null ? metadata.getName() : "archivo_drive";
        String finalName = construirNombreDescarga(trackingCode, origenSeguro, originalName);

        response.setContentType(
                metadata.getMimeType() != null ? metadata.getMimeType() : "application/octet-stream");

        response.setHeader("Content-Disposition", "attachment; filename=\"" + finalName + "\"");

        if (metadata.getSize() != null) {
            response.setContentLengthLong(metadata.getSize());
        }

        try (java.io.OutputStream out = response.getOutputStream()) {
            driveService.descargarArchivo(driveId, out);
            out.flush();
        }

        System.out.println("✅ Descarga Drive OK: " + finalName);
    }

    private void descargarDesdeUrlExterna(
            HttpServletResponse response,
            String fuente,
            String trackingCode,
            String origenSeguro) throws IOException, InterruptedException {

        if (fuente == null || fuente.isBlank()) {
            throw new IOException("Link externo vacío.");
        }

        String urlNormalizada = normalizarUrlDescargaExterna(fuente);

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(urlNormalizada))
                .header("User-Agent", "Mozilla/5.0 Legión3DTracker")
                .GET()
                .build();

        java.net.http.HttpResponse<byte[]> cloudResponse = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofByteArray());

        int status = cloudResponse.statusCode();

        if (status == 401) {
            throw new IOException("401 Unauthorized: el archivo requiere autenticación.");
        }

        if (status == 403) {
            throw new IOException("403 Forbidden: el archivo no tiene permisos públicos.");
        }

        if (status == 404) {
            throw new IOException("404 Not Found: el archivo no fue encontrado.");
        }

        if (status < 200 || status >= 300) {
            throw new IOException("Error HTTP externo: " + status);
        }

        String contentType = cloudResponse.headers()
                .firstValue("Content-Type")
                .orElse("application/octet-stream");

        String originalName = obtenerNombreDesdeUrlOHeader(fuente, cloudResponse);
        String finalName = construirNombreDescarga(trackingCode, origenSeguro, originalName);

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + finalName + "\"");
        response.setContentLength(cloudResponse.body().length);

        try (java.io.OutputStream out = response.getOutputStream()) {
            out.write(cloudResponse.body());
            out.flush();
        }

        System.out.println("✅ Descarga externa OK: " + finalName);
    }

    private String normalizarUrlDescargaExterna(String url) {
        if (url == null) {
            return "";
        }

        String normalizada = url.trim();

        /*
         * Dropbox:
         * Forzamos descarga directa cambiando dl=0 por dl=1.
         */
        if (normalizada.contains("dropbox.com")) {
            if (normalizada.contains("dl=0")) {
                normalizada = normalizada.replace("dl=0", "dl=1");
            } else if (!normalizada.contains("dl=1")) {
                normalizada += normalizada.contains("?") ? "&dl=1" : "?dl=1";
            }
        }

        return normalizada;
    }

    private String obtenerNombreDesdeUrlOHeader(
            String fuente,
            java.net.http.HttpResponse<byte[]> cloudResponse) {

        Optional<String> contentDisposition = cloudResponse.headers().firstValue("Content-Disposition");

        if (contentDisposition.isPresent() && contentDisposition.get().contains("filename=")) {
            String header = contentDisposition.get();
            String filename = header.substring(header.indexOf("filename=") + 9)
                    .replace("\"", "")
                    .replace("'", "")
                    .trim();

            if (!filename.isBlank()) {
                return filename;
            }
        }

        try {
            String path = java.net.URI.create(fuente).getPath();
            if (path != null && path.contains("/")) {
                String nombre = path.substring(path.lastIndexOf("/") + 1);
                if (nombre != null && !nombre.isBlank()) {
                    return nombre;
                }
            }
        } catch (Exception ignored) {
        }

        return "archivo_externo";
    }

    private String construirNombreDescarga(String trackingCode, String origenSeguro, String originalName) {
        String tracking = (trackingCode != null && !trackingCode.isBlank())
                ? trackingCode.trim().toUpperCase()
                : "LEG-SIN-CODIGO";

        String origen = (origenSeguro != null && !origenSeguro.isBlank())
                ? origenSeguro.trim().toLowerCase()
                : "archivo";

        /*
         * Prefijos oficiales:
         * wps = archivo inicial de WordPress
         * tec = archivo técnico de Drive / formulario técnico
         * links = enlace externo manual
         */
        if (!origen.equals("wps") && !origen.equals("tec") && !origen.equals("links")) {
            origen = "archivo";
        }

        String nombreSeguro = (originalName != null && !originalName.isBlank())
                ? originalName.trim()
                : "archivo";

        /*
         * Limpieza básica del nombre original.
         * Conserva extensión, puntos, guiones y guiones bajos.
         */
        nombreSeguro = nombreSeguro
                .replaceAll("[áàäâÁÀÄÂ]", "a")
                .replaceAll("[éèëêÉÈËÊ]", "e")
                .replaceAll("[íìïîÍÌÏÎ]", "i")
                .replaceAll("[óòöôÓÒÖÔ]", "o")
                .replaceAll("[úùüûÚÙÛÜ]", "u")
                .replaceAll("[ñÑ]", "n")
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");

        /*
         * Evita duplicar prefijos si el archivo ya venía renombrado.
         *
         * Ejemplos:
         * LEG-BE28-tec-IMG.jpg -> IMG.jpg
         * LEG-BE28_wps_IMG.jpg -> IMG.jpg
         * tec-IMG.jpg -> IMG.jpg
         * links-archivo.pdf -> archivo.pdf
         */
        nombreSeguro = nombreSeguro.replaceFirst("^" + java.util.regex.Pattern.quote(tracking) + "[-_]+", "");
        nombreSeguro = nombreSeguro.replaceFirst("^(wps|tec|links|archivo)[-_]+", "");

        if (nombreSeguro.isBlank()) {
            nombreSeguro = "archivo";
        }

        /*
         * Resultado final obligatorio:
         * LEG-XXXX-wps-nombre_original.ext
         * LEG-XXXX-tec-nombre_original.ext
         * LEG-XXXX-links-nombre_original.ext
         */
        return tracking + "-" + origen + "-" + nombreSeguro;
    }
    // --------------------------------------------------------------------------
    // ETAPA 3: RECEPCIÓN DE DATOS TÉCNICOS (CLIENTE LLENA INGENIERÍA)
    // --------------------------------------------------------------------------
    // Este bloque se activa cuando el cliente completa el formulario técnico
    // (formulario2).
    // El sistema captura medidas, material, color y método de entrega definidos por
    // el usuario.
    //
    // Lógica del Proceso:
    // 1. Recepción: El controlador captura el DTO proveniente del formulario
    // frontal.
    // 2. Persistencia: Se guarda la información en la base de datos (Entidad
    // DetallesTecnicos).
    // 3. Notificación: Envía un correo de confirmación al cliente con su código de
    // seguimiento
    // y un mensaje informativo mientras el equipo de Legión 3D prepara el
    // presupuesto final.

    @Operation(summary = "Recibir detalles técnicos", description = "Captura las medidas, material, color y método de entrega definidos por el cliente en el formulario de ingeniería.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingeniería guardada correctamente"),
            @ApiResponse(responseCode = "404", description = "Error al procesar el formulario o pedido no encontrado")
    })
    /**
     * ENDPOINT: Recepción de Formulario Técnico (POST)
     * Se activa cuando el cliente llena los detalles de ingeniería de su pieza.
     * Utiliza el 'PedidoService' para centralizar la lógica de mapeo y
     * actualización de los datos del cliente y los detalles técnicos.
     */
    @PostMapping("/{codigo}/detalles-tecnicos")
    public ResponseEntity<String> recibirFormularioTecnico(
            // El código de seguimiento se utiliza para identificar el pedido asociado a los
            // detalles técnicos que se están enviando. El DTO contiene toda la información
            // técnica que el cliente ha proporcionado en el formulario.
            @Parameter(description = "Código de seguimiento único del pedido", example = "LEG-A1B2") @PathVariable String codigo,
            // El DTO 'FormularioTecnicoDTO' debe contener campos como medidas, material,
            // color y método de entrega. La anotación @RequestBody indica que esta
            // información se espera en el cuerpo de la solicitud HTTP.
            @Parameter(description = "DTO con toda la información técnica del formulario") @RequestBody @Valid FormularioTecnicoDTO dto) {

        // VALIDACIÓN INICIAL: El servicio se encarga de validar que el código de
        // seguimiento exista y que los datos técnicos sean coherentes antes de proceder
        // a guardar. Si algo falla, se captura la excepción y se retorna un error 404
        // con un mensaje descriptivo.
        try {
            // PROCESAMIENTO INTEGRAL: El servicio se encarga de persistir los detalles
            // técnicos
            // y actualizar datos críticos del cliente (como Teléfono y RUT).
            Pedido pedido = pedidoService.guardarDetallesTecnicos(codigo, dto);

            // TRAZABILIDAD: Registramos la transición del pedido hacia el estado de
            // revisión.
            guardarHistorial(pedido, "SOLICITUD", "EN REVISIÓN");

            // COMUNICACIÓN AUTOMÁTICA: Construimos el mensaje de confirmación para dar
            // tranquilidad al cliente sobre la recepción de sus datos.
            // este el mensaje numero se envía inmediatamente después de guardar los
            // detalles técnicos, asegurando que el cliente reciba una confirmación oportuna
            // sin importar el resultado del proceso de presupuesto.

            // correo de confirmación de recepción de datos técnicos (correo n°3)
            // El mensaje es enviado despues que el cliente completa el formulario técnico
            // (Formulario2), confirmando que sus datos.
            String msjConfirmacion = "Hola " + pedido.getCliente().getNombre() + ",\n\n"
                    + "Tu requerimiento técnico ha sido acogido con éxito.\n"
                    + "Por favor, espera mientras preparamos tu cotización o presupuesto. Tu Código de Seguimiento es # ➡️ "
                    + pedido.getCodigoSeguimiento() + ".\n"
                    + obtenerFirmaCorporativa();
            // El correo se envía utilizando el servicio de email, asegurando que el cliente
            // reciba una notificación clara y profesional sobre el estado de su solicitud
            // técnica.
            try {
                emailService.enviarCorreoSimple(pedido.getCliente().getEmail(), "⚙️ Datos Técnicos Recibidos",
                        msjConfirmacion);
            } catch (Exception e) {
                System.err.println("Error enviando correo de confirmación técnica: " + e.getMessage());
            }
            // RESPUESTA FINAL: Si todo el proceso se ejecuta sin problemas, se devuelve un
            // mensaje de éxito al cliente confirmando que su ingeniería ha sido guardada
            // correctamente y que su teléfono ha sido actualizado en el sistema.
            return ResponseEntity.ok("Ingeniería guardada correctamente y teléfono actualizado.");

            // CONTROL DE EXCEPCIONES: Si ocurre cualquier error durante el proceso (como un
            // código de seguimiento inexistente o datos inválidos), se captura la excepción
            // y se retorna un error 404 con un mensaje descriptivo para facilitar el
            // soporte técnico.
        } catch (Exception e) {
            // CONTROL DE EXCEPCIONES: En caso de error (ej. código inexistente), se retorna
            // un 404
            // informando la naturaleza del fallo para facilitar el soporte técnico.
            return ResponseEntity.status(404).body("Error al procesar el formulario: " + e.getMessage());
        }
    }

    // --------------------------------------------------------------------------
    // ETAPA 4: GESTIÓN DE PRESUPUESTO (ENVÍO DE COTIZACIÓN Y PLAZOS)
    // --------------------------------------------------------------------------
    // Este bloque permite a Luis cargar el presupuesto formal, definir el precio
    // final y establecer los tiempos de validez de la oferta.
    //
    // Lógica del Proceso:
    // 1. Carga de Archivos: Se recibe el PDF del presupuesto y una imagen
    // referencial
    // del proyecto mediante 'MultipartFile'.
    // 2. Cálculo de Vencimiento: Se utiliza el método 'calcularDiasHabiles(15)'
    // para
    // determinar la fecha límite de la oferta, excluyendo fines de semana/festivos.
    // 3. Notificación Unificada: Se envía un correo al cliente que consolida:
    // - El precio total del servicio.
    // - El PDF adjunto con el desglose.
    // - La foto del diseño para validación visual.
    // 4. Seguridad: Solo se permite este envío si el pedido está en estado
    // "COTIZACION_PENDIENTE".

    @Operation(summary = "Enviar presupuesto", description = "Define el precio final, la fecha de entrega y adjunta una cotización formal para el cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Presupuesto enviado y guardado"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe"),
            @ApiResponse(responseCode = "500", description = "Error al enviar el correo")
    })
    /**
     * ENDPOINT: Envío de Presupuesto (POST)
     * Permite a Luis definir el precio y la fecha de entrega. Procesa la subida de
     * un
     * archivo adjunto (cotización) y genera un correo dinámico con cálculos de
     * vencimiento.
     */
    @PostMapping("/{id}/presupuesto")
    public ResponseEntity<String> enviarPresupuesto(
            @Parameter(description = "ID del pedido a presupuestar", example = "1") @PathVariable Long id,
            @Parameter(description = "Monto NETO del trabajo", example = "45000.0") @RequestParam("precioFinal") Double precioFinal,
            @Parameter(description = "Precio Original sin descuento", required = false) @RequestParam(value = "precioOriginal", required = false) Double precioOriginal,
            @Parameter(description = "Porcentaje de descuento", required = false) @RequestParam(value = "descuentoPorcentaje", required = false) Double descuentoPorcentaje,
            @Parameter(description = "Fecha de entrega en formato dd-MM-yyyy", example = "30-05-2024") @RequestParam(value = "fechaEntregaEstimada", required = false) String fechaEntregaStr,
            @Parameter(description = "Comentario adicional para el correo", example = "El diseño requiere soportes especiales.") @RequestParam(value = "mensajeAdicional", required = false) String mensajeAdicional,
            @Parameter(description = "Desglose de costos interno", required = false) @RequestParam(value = "anotacionesInternas", required = false) String anotacionesInternas,
            @Parameter(description = "Archivo PDF o imagen de la cotización") @RequestParam(value = "archivoAdjunto", required = false) org.springframework.web.multipart.MultipartFile archivoAdjunto) {

        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty())
            return ResponseEntity.status(404).body("Error: El pedido no existe.");

        Pedido pedido = pedidoOpt.get();
        String estadoAntiguo = pedido.getEstadoActual();

        // 1. Cálculos de Fechas y Precios
        LocalDate fechaVencimiento = calcularDiasHabiles(15);
        pedido.setPrecioFinal(precioFinal);
        pedido.setDescuentoPorcentaje(descuentoPorcentaje != null ? descuentoPorcentaje : 0.0);

        // APLICA EL RECÁLCULO INVERSO (Antes del save del repositorio)
        if (pedido.getDescuentoPorcentaje() > 0 && pedido.getDescuentoPorcentaje() < 100) {
            double precioOriginalCalculado = pedido.getPrecioFinal() / (1 - (pedido.getDescuentoPorcentaje() / 100.0));
            pedido.setPrecioOriginal(Math.round(precioOriginalCalculado * 1.0) / 1.0);
        } else {
            pedido.setPrecioOriginal(pedido.getPrecioFinal());
        }

        pedido.setAnotacionesInternas(anotacionesInternas);
        pedido.setDesgloseCostos(anotacionesInternas);
        pedido.setFechaVencimientoPresupuesto(fechaVencimiento);

        java.time.format.DateTimeFormatter formateadorFecha = java.time.format.DateTimeFormatter
                .ofPattern("dd-MM-yyyy");
        String textoEntrega = "";

        if (fechaEntregaStr != null && !fechaEntregaStr.isEmpty()) {
            LocalDate fechaEntrega = LocalDate.parse(fechaEntregaStr, formateadorFecha);
            pedido.setFechaEntregaEstimada(fechaEntrega);
            String entregaBonita = fechaEntrega.format(formateadorFecha);
            textoEntrega = "Fecha estimada de entrega (una vez confirmado el pago): " + entregaBonita + "\n\n";
        }

        // 2. Cálculo de IVA y Bruto para el correo
        double neto = pedido.getPrecioFinal();
        double iva = Math.round(neto * 0.19);
        double bruto = neto + iva;

        java.text.DecimalFormat formateaMoneda = new java.text.DecimalFormat("###,###.##");
        String netoBonito = formateaMoneda.format(neto);
        String ivaBonito = formateaMoneda.format(iva);
        String brutoBonito = formateaMoneda.format(bruto);
        String vencimientoBonito = fechaVencimiento.format(formateadorFecha);

        // 3. Construcción del Correo Dinámico (Marketing + Tracking + Firma)
        StringBuilder msj = new StringBuilder();
        msj.append("Hola ").append(pedido.getCliente().getNombre()).append(",\n\n");
        msj.append("REF: Proyecto / Cotización N° ").append(pedido.getCodigoSeguimiento()).append("\n\n");

        if (mensajeAdicional != null && !mensajeAdicional.trim().isEmpty()) {
            msj.append("Mensaje de nuestro taller sobre tu pieza:\n");
            msj.append(mensajeAdicional).append("\n\n");
            msj.append("--------------------------------------------------\n\n");
        }

        msj.append("Tu cotización ha sido generada con el siguiente desglose:\n\n");

        if (descuentoPorcentaje != null && descuentoPorcentaje > 0) {
            // FÓRMULA DE MARKETING: Calcula el precio inflado para mostrar el ahorro
            double brutoReferencia = (precioFinal / (1 - (descuentoPorcentaje / 100.0))) * 1.19;

            msj.append("❌ Precio referencial antes del descuento (con IVA incluido): $")
                    .append(formateaMoneda.format(Math.round(brutoReferencia)))
                    .append("\n");

            msj.append("🎁 Descuento comercial aplicado: ")
                    .append(descuentoPorcentaje)
                    .append("% OFF\n");

            msj.append("--------------------------------------------------\n");
        }

        msj.append("- Valor Neto Final: $").append(netoBonito).append("\n")
                .append("- IVA (19%): $").append(ivaBonito).append("\n")
                .append("- TOTAL A PAGAR (BRUTO): $").append(brutoBonito).append("\n\n")
                .append("Válido hasta: ").append(vencimientoBonito).append("\n")
                .append(textoEntrega) // Esto incluye la fecha estimada de entrega
                .append("\n➡️ Para ver el detalle y subir tu comprobante de pago, ingresa aquí: ")
                .append("http://localhost:8080/tracking/").append(pedido.getCodigoSeguimiento()).append("\n\n")
                .append(obtenerFirmaCorporativa());

        // 3. Envío del Correo: Se intenta enviar el correo con el mensaje construido y
        // el archivo adjunto (si existe). Si el envío es exitoso, se confirma que el
        // presupuesto ha sido enviado y guardado. Si ocurre un error durante el envío
        // del correo, se captura la excepción y se retorna un error 500, informando al
        // cliente que hubo un problema con la comunicación y que no se guardó el cambio
        // de estado para evitar confusiones.
        // 1. COMMIT INCONDICIONAL EN BASE DE DATOS
        // Guardamos el estado ANTES de intentar el correo. Esto blinda la operación:
        // si el SMTP de Gmail falla por timeout, la base de datos ya está segura.
        pedido.setEstadoActual("PRESUPUESTADO");
        pedidoRepo.save(pedido);
        guardarHistorial(pedido, estadoAntiguo, "PRESUPUESTADO");

        // 2. DELEGACIÓN DE CORREO TOLERANTE A FALLOS
        try {
            if (archivoAdjunto != null && !archivoAdjunto.isEmpty()) {
                emailService.enviarCorreoConAdjunto(pedido.getCliente().getEmail(), "💰 Cotización Legión 3D",
                        msj.toString(), archivoAdjunto);
            } else {
                emailService.enviarCorreoSimple(pedido.getCliente().getEmail(), "💰 Cotización Legión 3D",
                        msj.toString());
            }
            return ResponseEntity.ok("Presupuesto enviado y guardado exitosamente.");
        } catch (Exception e) {
            // El fallo de red se captura sin provocar Rollback. El proceso de negocio
            // sobrevive.
            System.err.println("CRÍTICO (IGNORADO): Fallo en SMTP para pedido " + pedido.getCodigoSeguimiento() + ".");
            return ResponseEntity.ok(
                    "✅ Presupuesto GUARDADO en el sistema. Hubo un error de conexión con Gmail; el correo no salió, favor notificar al cliente vía WhatsApp.");
        }
    }

    // -----------------------------------------------------------------
    // ETAPA 6A: RECEPCIÓN DE COMPROBANTE Y ALARMA FINANCIERA (PAGO ENVIADO)
    // --------------------------------------------------
    // se desarrollo este bloque como un punto de control crítico.
    // Cuando el usuario notifica su transferencia, el sistema corta el avance
    // automático
    // hacia la etapa de impresión. En su lugar, generamos una alerta visual
    // (semáforo verde)
    // para que la administración revise los fondos de manera humana antes de
    // comprometer
    // recursos y material del taller.

    @Operation(summary = "Registrar pago del cliente", description = "El cliente notifica que ha realizado la transferencia y adjunta el enlace del comprobante subido.")
    @ApiResponse(responseCode = "200", description = "Comprobante registrado y alerta activada")
    /**
     * ENDPOINT: Registro de Pago (PATCH)
     * Este método recibe la notificación del comprobante del cliente.
     * Su función principal es transicionar el pedido al estado de alerta
     * 'PAGO_ENVIADO',
     * bloqueando el flujo de producción hasta que el área de finanzas apruebe la
     * operación.
     */
    @PatchMapping("/{id}/pago")
    public ResponseEntity<String> registrarPago(
            @Parameter(description = "ID del pedido", example = "1") @PathVariable Long id,
            @Parameter(description = "DTO con el link al comprobante") @RequestBody PagoDTO dto) {

        // 1. VALIDACIÓN INICIAL: Verificamos la existencia del pedido en la base de
        // datos
        // para proteger la integridad referencial antes de hacer cualquier CAMBIO.
        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty())
            return ResponseEntity.status(404).body("Error: El pedido no existe.");

        Pedido pedido = pedidoOpt.get();
        String estadoAntiguo = pedido.getEstadoActual();

        // 2. CAPTURA DEL COMPROBANTE (ESTRATEGIA DE ESCALABILIDAD):
        // Almacenamos el enlace del documento que el cliente adjuntó en el DTO.
        // Como equipo decidimos pivotar hacia el uso de un botón de WhatsApp en el
        // frontend
        // para la recepción real del comprobante (ahorrando costos de almacenamiento en
        // servidor
        // y asegurando una comunicación más rápida). Sin embargo, dejamos este campo y
        // lógica
        // completamente operativos en el backend por si el negocio requiere volver a
        // implementar
        // la carga directa de archivos en el futuro.
        pedido.setLinkComprobantePago(dto.getLinkComprobantePago());

        // 3. PUNTO DE CONTROL CRÍTICO (LA SOLUCIÓN ARQUITECTÓNICA):
        // Solo permitimos el retroceso a PAGO_ENVIADO si el pedido viene de etapas
        // iniciales.
        // Si ya está en producción o listo, guardamos el link pero no alteramos el
        // flujo.
        if ("PRESUPUESTADO".equals(estadoAntiguo) || "EXPIRADO".equals(estadoAntiguo)) {
            pedido.setEstadoActual("PAGO_ENVIADO");
            guardarHistorial(pedido, estadoAntiguo, "PAGO_ENVIADO");
        }

        // 4. PERSISTENCIA Y TRAZABILIDAD: Guardamos los cambios en el repositorio
        // para respaldar cada movimiento financiero.
        pedidoRepo.save(pedido);

        // 5. COMUNICACIÓN ESTRATÉGICA CON EL CLIENTE:
        // Adaptamos la redacción de este correo para manejar las expectativas del
        // usuario.
        // Le informamos claramente que el pago está bajo un proceso de verificación
        // humana,
        // evitando ansiedades sobre el inicio inmediato de la fabricación de su pieza.
        String msj = "Hola " + pedido.getCliente().getNombre() + ",\n\n"
                + "Hemos recibido tu comprobante de pago para el pedido # ➡️ " + pedido.getCodigoSeguimiento() + ".\n"
                + "Nuestro equipo de Finanzas lo está verificando en este momento. 🔍\n"
                // (aqui modificar al desplegar a produccion: reemplazar localhost:8080 por el
                // dominio oficial)
                + "➡️ Consulta el estado de tu pedido aquí: ➡️ http://localhost:8080/tracking/"
                + pedido.getCodigoSeguimiento()
                + obtenerFirmaCorporativa();

        // ASUNTO DEL CORREO
        try {
            emailService.enviarCorreoSimple(pedido.getCliente().getEmail(), "🪙🧾 Pago Recibido - En Revisión ", msj);
        } catch (Exception e) {
            System.err.println("Error enviando correo de recepción de pago: " + e.getMessage());
        }

        // RESPUESTA FINAL: Confirmamos al cliente que su comprobante ha sido recibido
        // y que el proceso de verificación está en marcha, estableciendo una
        // comunicación
        // transparente y profesional.
        return ResponseEntity.ok("Comprobante recibido. Activando alarma en el Dashboard de Finanzas.");
    }

    // ESTE ENDPOINT ES UNA ESTRUCTURA DE SOPORTE.
    // Se dejó configurado porque como equipo decidimos que el cliente envíe el
    // comprobante
    // por WhatsApp en lugar de subir archivos al portal. Lo mantenemos aquí
    // documentado
    // y listo por si se desea implementar una pasarela de pagos automatizada a
    // futuro.
    @PatchMapping("/{codigo}/pagos")
    public ResponseEntity<String> registrarPagoPorCodigo(@PathVariable String codigo, @RequestBody String linkPago) {
        return ResponseEntity.ok("Endpoint de pago listo para implementación.");
    }

    // -----------------------------------------------
    // ETAPA 8: CIERRE DE CICLO Y DESPACHO DE LA PIEZA (HITO FINAL)
    // --------------------------------------------------------------------------
    // Este bloque representa el fin del flujo de producción. Se activa cuando el
    // producto está verificado y listo para salir del taller de Legión 3D.
    //
    // Lógica del Proceso:
    // 1. Validación de Salida: Confirma que el pedido ha pasado por todas las
    // etapas previas (impresión, post-procesado y control de calidad).
    // 2. Ejecución Logística: Según la elección del cliente, el sistema gestiona:
    // - Generación de datos para despacho físico (Starken/Chilexpress).
    // - Instrucciones de retiro presencial en taller.
    // - Habilitación de enlaces para descarga de activos digitales.
    // 3. Notificación de Cierre: Envía el último correo (n°5) con la información
    // necesaria para que el cliente reciba su pieza o proyecto.

    /**
     * ENDPOINT: Despacho y Cierre de Ciclo (PATCH)
     * Este es el último paso operativo. Finaliza el pedido registrando el método
     * real aplicado por administración y envía el correo final de entrega.
     */
    @Operation(summary = "Finalizar y despachar", description = "Registra la salida del producto del taller.")
    @ApiResponse(responseCode = "200", description = "Ciclo cerrado y correo de entrega enviado")
    @PatchMapping("/{id}/despachar")
    public ResponseEntity<?> despacharPedido(
            @Parameter(description = "ID del pedido a despachar", example = "1") @PathVariable Long id,
            @Parameter(description = "Datos logísticos enviados desde el modal frontal") @RequestBody java.util.Map<String, String> payload) {

        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty())
            return ResponseEntity.status(404).body("Error: El pedido no existe.");

        Pedido pedido = pedidoOpt.get();
        String estadoAntiguo = pedido.getEstadoActual();

        // Extracción directa y segura de las variables enviadas por el frontend
        String ordenLuis = payload.get("metodoEntregaReal");
        String linkFinal = payload.get("linkComprobanteEnvio");

        if (ordenLuis == null || ordenLuis.isBlank())
            ordenLuis = "STARKEN";

        String fileId = extraerIdDeDrive(linkFinal);
        String finalMailLink = linkFinal; // El link original para el correo
        String voucherDriveLink = null; // Link del voucher generado o copiado

        if (linkFinal != null && !linkFinal.isBlank()) {
            if (fileId != null) {
                try {
                    String nuevoNombre = pedido.getCodigoSeguimiento() + "-starken-"
                            + (pedido.getCliente().getNombre() != null
                                    ? pedido.getCliente().getNombre().replace(" ", "_")
                                    : "envio");
                    GoogleDriveService.DriveUploadResult result = driveService.copiarArchivo(fileId, nuevoNombre,
                            "1WTuwjfjGtxi9kyp9BMp1KWpDvShgvLIn");
                    if (result != null) {
                        voucherDriveLink = result.getWebViewLink();
                        finalMailLink = result.getWebViewLink(); // Si es Drive, actualizamos para el cliente
                    }
                } catch (Exception e) {
                    System.err
                            .println("⚠️ Fallo en automatización de copiado de Drive al despachar: " + e.getMessage());
                }
            } else if (!ordenLuis.equalsIgnoreCase("RETIRO")) {
                // GENERACIÓN DE VOUCHER LOGÍSTICO AUTOMÁTICO EN MEMORIA (.txt)
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("==================================================\n");
                    sb.append("       VOUCHER DE DESPACHO LOGÍSTICO - LEGION 3D\n");
                    sb.append("==================================================\n");
                    sb.append("Código de Seguimiento : ").append(pedido.getCodigoSeguimiento()).append("\n");
                    sb.append("Cliente               : ")
                            .append(pedido.getCliente() != null ? pedido.getCliente().getNombre() : "N/A").append("\n");
                    sb.append("Método de Entrega     : ").append(ordenLuis).append("\n");
                    sb.append("Detalle del Despacho  : ").append(linkFinal).append("\n");
                    sb.append("Servicio Solicitado   : ")
                            .append(pedido.getServicioSolicitado() != null ? pedido.getServicioSolicitado() : "N/A")
                            .append("\n");
                    sb.append("Fecha y Hora          : ")
                            .append(java.time.LocalDateTime.now()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .append("\n");
                    sb.append("==================================================\n");

                    String voucherText = sb.toString();
                    byte[] bytes = voucherText.getBytes(java.nio.charset.StandardCharsets.UTF_8);

                    try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(bytes)) {
                        String fileName = "voucher_despacho_" + pedido.getCodigoSeguimiento() + ".txt";
                        GoogleDriveService.DriveUploadResult result = driveService.subirComprobantePago(
                                bis, fileName, "text/plain", bytes.length, "1WTuwjfjGtxi9kyp9BMp1KWpDvShgvLIn");
                        voucherDriveLink = result.getWebViewLink();
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error al generar/subir voucher logístico: " + e.getMessage());
                }
            }
        }

        // REGLA ESTRICTA DE NEGOCIO: Guardamos intacto el código de tracking original
        // del operador
        pedido.setLinkComprobanteEnvio(linkFinal);
        pedido.setEstadoActual("ENTREGADO");
        pedidoRepo.save(pedido);
        guardarHistorial(pedido, estadoAntiguo, "ENTREGADO");

        String servicio = pedido.getServicioSolicitado() != null ? pedido.getServicioSolicitado().toLowerCase() : "";
        String articulo = (servicio.contains("diseño") || servicio.contains("ingeniería")) ? "proyecto" : "pieza";
        String mensajeCorreo;

        // BIFURCACIÓN LOGÍSTICA PARA NOTIFICACIONES
        if (ordenLuis.equalsIgnoreCase("RETIRO")) {
            pedido.setLinkComprobanteEnvio("Retiro en Taller");
            pedidoRepo.save(pedido); // Actualizamos el estado de texto para retiro
            mensajeCorreo = "Hola " + pedido.getCliente().getNombre() + ",\n\n"
                    + "¡Tu pedido ha sido entregado exitosamente de forma presencial en nuestro taller!\n"
                    + "Esperamos que disfrutes tu " + articulo + ". ¡Gracias por elegir a Legión 3D!";

        } else if (ordenLuis.equalsIgnoreCase("DIGITAL")) {
            mensajeCorreo = "Hola " + pedido.getCliente().getNombre() + ",\n\n"
                    + "¡Tu " + articulo
                    + " ha sido entregado exitosamente de forma digital! Puedes descargar tus archivos finales en el siguiente enlace ➡️:\n"
                    + "🔗 " + finalMailLink + "\n\n"
                    + "¡Gracias por confiar tu idea a Legión 3D!";

        } else if (ordenLuis.equalsIgnoreCase("MIXTO")) {
            mensajeCorreo = "Hola " + pedido.getCliente().getNombre() + ",\n\n"
                    + "Tu " + articulo
                    + " va en camino físicamente y, además, te compartimos el enlace con los respaldos digitales ➡️:\n\n"
                    + "📦 Información Logística: " + finalMailLink + "\n\n"
                    + "¡Gracias por confiar tu proyecto integral a Legión 3D!";

        } else {
            // CASO POR DEFECTO: STARKEN / CHILEXPRESS
            mensajeCorreo = "Hola " + pedido.getCliente().getNombre() + ",\n\n"
                    + "Tu " + articulo
                    + " ha sido despachad@ y va en camino. Puedes rastrear tu pedido con el siguiente Número de rastreo ➡️: \n"
                    + "🚚 " + finalMailLink + "\n\n" // Cliente recibe su código original intacto
                    + " Diríjase a La página oficial de Starken o Chilexpress e ingrese el código de rastreo para mayor información"
                    + "\n\n"
                    + "https://starken.cl/seguimiento" + "\n"
                    + "https://www.chilexpress.cl" + "\n\n"
                    + "¡Gracias por elegir a Legión 3D!";
        }

        try {
            emailService.enviarCorreoSimple(pedido.getCliente().getEmail(), "🤝 Pedido Finalizado y Entregado",
                    mensajeCorreo + obtenerFirmaCorporativa());
        } catch (Exception e) {
            System.err.println("Error enviando correo de cierre: " + e.getMessage());
        }

        // RESPUESTA EN FORMATO JSON
        java.util.Map<String, String> responseMap = new java.util.HashMap<>();
        responseMap.put("mensaje", "¡Ciclo cerrado! Método aplicado: " + ordenLuis.toUpperCase());
        if (voucherDriveLink != null) {
            responseMap.put("voucherDriveLink", voucherDriveLink);
        }
        return ResponseEntity.ok(responseMap);
    }

    // -----------------------
    // HERRAMIENTAS ADMINISTRATIVAS Y DE MANTENIMIENTO (DASHBOARD)
    // ---------------------------------------------------------------------------
    // Este bloque contiene endpoints diseñados para facilitar la gestión interna de
    // los pedidos desde el dashboard administrativo. Permiten a los administradores
    // enviar correos personalizados, editar pedidos de forma integral y acceder a
    // la lista completa de pedidos para monitoreo y soporte.

    @Operation(summary = "Enviar correo manual", description = "Permite a administración enviar un mensaje libre al cliente vinculado a su pedido y código de seguimiento.")
    @ApiResponse(responseCode = "200", description = "Mensaje enviado exitosamente")
    // Este endpoint es una herramienta de soporte diseñada para que los
    // administradores puedan comunicarse directamente con los clientes a través del
    // sistema, utilizando el código de seguimiento para mantener la trazabilidad de
    // la comunicación. Permite enviar mensajes personalizados sin depender de las
    // automatizaciones predefinidas, lo que es útil para casos especiales o
    // comunicaciones urgentes.
    @PostMapping("/{id}/enviar-correo")
    public ResponseEntity<String> enviarCorreoPersonalizado(
            @Parameter(description = "ID del pedido", example = "1") @PathVariable Long id,
            @Parameter(description = "DTO con asunto y cuerpo del mensaje") @RequestBody MensajeDTO dto) {

        // VALIDACIÓN INICIAL: Verificamos que el pedido exista antes de intentar enviar
        // el correo. Si no se encuentra, se retorna un error 404 para proteger la
        // integridad del proceso y evitar intentos de comunicación con pedidos
        // inexistentes.
        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty())
            return ResponseEntity.status(404).body("Error: El pedido no existe.");

        Pedido pedido = pedidoOpt.get();
        Cliente cliente = pedido.getCliente();

        // CONSTRUCCIÓN DEL MENSAJE: El asunto del correo se enriquece con el código de
        // seguimiento y el RUT del cliente (si está disponible) para facilitar la
        // identificación rápida del pedido en la bandeja de entrada del cliente. El
        // cuerpo del mensaje se arma combinando el texto proporcionado en el DTO con
        // una firma corporativa para mantener la profesionalidad de la comunicación.
        String rutCliente = cliente.getRut() != null ? cliente.getRut() : "SIN-RUT";
        String asuntoTrazable = dto.getAsunto() + " | [Ref: #" + pedido.getCodigoSeguimiento() + " | RUT: " + rutCliente
                + "]";
        String mensajeArmado = "Hola " + cliente.getNombre() + ",\n\n" + dto.getCuerpoMensaje() + "\n"
                + obtenerFirmaCorporativa();

        emailService.enviarCorreoSimple(cliente.getEmail(), asuntoTrazable, mensajeArmado);

        // Dejamos un registro en la auditoría para saber que hubo comunicación manual
        guardarHistorial(pedido, pedido.getEstadoActual(), "CORREO_MANUAL_ENVIADO");

        return ResponseEntity.ok("Mensaje enviado exitosamente.");
    }

    // REGLA DE NEGOCIO CRÍTICA: Este endpoint permite realizar una edición integral
    // del pedido, abarcando datos del cliente, información base del pedido y
    // detalles técnicos, todo en una sola operación. Esto es fundamental para
    // corregir errores de tipeo o actualizar parámetros sin alterar la máquina de
    // estados, manteniendo la integridad del flujo de producción.
    /**
     * PROXY DE SEGURIDAD: Visor de Evidencia
     * Este endpoint resuelve el bloqueo de CORS/X-Frame de Google Drive.
     * El servidor descarga la imagen usando sus credenciales y la sirve
     * directamente al frontend para una visualización fluida.
     */
    @GetMapping("/ver-comprobante")
    public void verComprobante(@RequestParam String link, jakarta.servlet.http.HttpServletResponse response) {
        try {
            String fileId = extraerIdDeDrive(link);
            if (fileId == null) {
                response.setStatus(400);
                return;
            }

            com.google.api.services.drive.model.File metadata = driveService.obtenerMetadata(fileId);
            if (metadata != null && "application/vnd.google-apps.folder".equals(metadata.getMimeType())) {
                response.sendRedirect("https://drive.google.com/drive/folders/" + fileId);
                return;
            }
            response.setContentType(metadata.getMimeType());
            response.setHeader("Content-Disposition", "inline; filename=\"" + metadata.getName() + "\"");

            driveService.descargarArchivo(fileId, response.getOutputStream());
        } catch (Exception e) {
            response.setStatus(500);
        }
    }

    private String extraerIdDeDrive(String link) {
        if (link == null)
            return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:/d/|id=|folders/)([\\w-]+)");
        java.util.regex.Matcher matcher = pattern.matcher(link);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Operation(summary = "Obtener pedido por Código de Seguimiento", description = "Busca un pedido por su código LEG-XXXX para visualización rápida.")
    @GetMapping("/tracking/{codigo}")
    public ResponseEntity<?> obtenerPedidoPorTracking(@PathVariable String codigo) {
        return pedidoRepo.findByCodigoSeguimiento(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // REGLA DE NEGOCIO CRÍTICA: Obtener todos los pedidos
    @Operation(summary = "Listar todos los pedidos", description = "Obtiene la lista completa de pedidos registrados en el sistema.")
    @GetMapping(produces = "application/json")
    public ResponseEntity<List<Pedido>> obtenerTodosLosPedidos() {
        return ResponseEntity.ok(pedidoRepo.findAllActive());
    }

    // Obtener pedido por ID
    @Operation(summary = "Obtener pedido por ID", description = "Busca un pedido específico por su identificador único.")
    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<Pedido> obtenerPedidoPorId(
            @Parameter(description = "ID del pedido", example = "1") @PathVariable Long id) {
        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isPresent()) {
            Double saldo = pagoService.calcularSaldoPendiente(id);
            return ResponseEntity.ok()
                    .header("X-Saldo-Pendiente", String.valueOf(saldo))
                    .body(pedidoOpt.get());
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Editar pedido integral", description = "Permite modificar datos del cliente, del pedido y detalles técnicos en una sola operación desde el dashboard.")
    @ApiResponse(responseCode = "200", description = "Actualizado correctamente")
    @PutMapping("/{id}")
    public ResponseEntity<java.util.Map<String, String>> editarPedidoCompleto(
            @Parameter(description = "ID del pedido a editar", example = "1") @PathVariable Long id,
            @Parameter(description = "DTO con todos los campos editables") @Valid @RequestBody EdicionPedidoDTO dto) {

        // ENDPOINT DE CONTROL TOTAL: Permite corregir errores de tipeo del cliente
        // o ajustar parámetros técnicos sin alterar la máquina de estados.
        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty())
            return ResponseEntity.status(404).body(java.util.Map.of("error", "El pedido no existe."));

        // EXTRACCIÓN DE DATOS
        Pedido pedido = pedidoOpt.get();
        Cliente cliente = pedido.getCliente();

        // 1. Actualización de datos del Cliente (Identidad)
        if (dto.getNombre() != null)
            cliente.setNombre(dto.getNombre());
        if (dto.getEmail() != null)
            cliente.setEmail(dto.getEmail());
        if (dto.getTelefono() != null)
            cliente.setTelefono(dto.getTelefono());
        if (dto.getRut() != null)
            cliente.setRut(dto.getRut());
        clienteRepo.save(cliente);

        // 2. Actualización de datos base del Pedido (Finanzas y Servicio)
        if (dto.getServicioSolicitado() != null)
            pedido.setServicioSolicitado(dto.getServicioSolicitado());
        if (dto.getPrecioFinal() != null)
            pedido.setPrecioFinal(dto.getPrecioFinal());
        if (dto.getFechaEntregaEstimada() != null)
            pedido.setFechaEntregaEstimada(dto.getFechaEntregaEstimada());

        // Mapear justificaciones de auditoría a la entidad Pedido
        if (dto.getJustificacionCliente() != null)
            pedido.setJustificacionCliente(dto.getJustificacionCliente());
        if (dto.getJustificacionFinanzas() != null)
            pedido.setNotasAuditoria(dto.getJustificacionFinanzas());
        if (dto.getJustificacionIngenieria() != null)
            pedido.setResumenFinancieroOperador(dto.getJustificacionIngenieria());

        pedidoRepo.save(pedido);

        // 3. Actualización profunda de Detalles Técnicos (Upsert Garantizado)
        DetallesTecnicos detalles = detallesRepo.findByPedidoId(pedido.getId()).orElse(new DetallesTecnicos());
        if (detalles.getId() == null) {
            detalles.setPedido(pedido);
            detalles.setActivo(true);
        }

        if (dto.getMedidaAncho() != null)
            detalles.setMedidaAncho(dto.getMedidaAncho());
        if (dto.getMedidaAlto() != null)
            detalles.setMedidaAlto(dto.getMedidaAlto());
        if (dto.getMedidaProfundidad() != null)
            detalles.setMedidaProfundidad(dto.getMedidaProfundidad());
        if (dto.getCantidadUnidades() != null)
            detalles.setCantidadUnidades(dto.getCantidadUnidades());
        if (dto.getMaterialSolicitado() != null)
            detalles.setMaterialSolicitado(dto.getMaterialSolicitado());
        if (dto.getColorSolicitado() != null)
            detalles.setColorSolicitado(dto.getColorSolicitado());
        if (dto.getMetodoEntrega() != null)
            detalles.setMetodoEntrega(dto.getMetodoEntrega());
        if (dto.getRegion() != null)
            detalles.setRegion(dto.getRegion());
        if (dto.getComuna() != null)
            detalles.setComuna(dto.getComuna());
        if (dto.getCalleYNumero() != null)
            detalles.setCalleYNumero(dto.getCalleYNumero());
        if (dto.getTienePiezaFisica() != null)
            detalles.setTienePiezaFisica(dto.getTienePiezaFisica());
        if (dto.getLinkArchivoFinal() != null)
            detalles.setLinkArchivoFinal(dto.getLinkArchivoFinal());
        if (dto.getLinkFormularioIngenieria() != null)
            detalles.setLinkFormularioIngenieria(dto.getLinkFormularioIngenieria());

        // Campos Fase 1
        if (dto.getPresupuestoEstimado() != null)
            detalles.setPresupuestoEstimado(dto.getPresupuestoEstimado());
        if (dto.getDiasEntrega() != null)
            detalles.setDiasEntrega(dto.getDiasEntrega());
        if (dto.getToleranciaCheck() != null)
            detalles.setToleranciaCheck(dto.getToleranciaCheck());
        if (dto.getEsCopiaExacta() != null)
            detalles.setEsCopiaExacta(dto.getEsCopiaExacta());
        if (dto.getEntornoUso() != null)
            detalles.setEntornoUso(dto.getEntornoUso());
        if (dto.getDeptoCasaOficina() != null)
            detalles.setDeptoCasaOficina(dto.getDeptoCasaOficina());
        if (dto.getTipoEnvioStarken() != null)
            detalles.setTipoEnvioStarken(dto.getTipoEnvioStarken());

        detallesRepo.save(detalles);
        pedido.setDetallesTecnicos(detalles);
        pedidoRepo.save(pedido);

        // REGISTRO DE AUDITORÍA DIVIDIDA
        StringBuilder justificacionConsolidada = new StringBuilder("EDICIÓN MAESTRA:\n");
        if (dto.getJustificacionCliente() != null && !dto.getJustificacionCliente().isBlank())
            justificacionConsolidada.append("- Cliente: ").append(dto.getJustificacionCliente()).append("\n");
        if (dto.getJustificacionFinanzas() != null && !dto.getJustificacionFinanzas().isBlank())
            justificacionConsolidada.append("- Finanzas: ").append(dto.getJustificacionFinanzas()).append("\n");
        if (dto.getJustificacionIngenieria() != null && !dto.getJustificacionIngenieria().isBlank())
            justificacionConsolidada.append("- Ingeniería: ").append(dto.getJustificacionIngenieria()).append("\n");

        String comentarioFinal = justificacionConsolidada.toString().trim();
        if (comentarioFinal.equals("EDICIÓN MAESTRA:"))
            comentarioFinal = "Edición integral administrativa (Sin detalle).";

        guardarHistorial(pedido, pedido.getEstadoActual(), comentarioFinal);

        return ResponseEntity.ok(java.util.Map.of("mensaje", "Actualizado correctamente."));
    }

    @Operation(summary = "Liquidar Saldo a $0", description = "Genera un abono administrativo para cuadrar saldos antes de una entrega forzada.")
    @PostMapping("/{id}/liquidar-saldo")
    public ResponseEntity<java.util.Map<String, String>> liquidarSaldoAdmin(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload) {
        String justificacion = payload.getOrDefault("justificacion", "Liquidación administrativa por compensación.");
        pagoService.liquidarSaldoAdministrativo(id, justificacion);
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Saldo liquidado a $0 correctamente."));
    }

    // ----------------------------------------------------------------------
    // REGLA DE NEGOCIO CRÍTICA: ARCHIVADO Y ELIMINACIÓN DE PEDIDOS (SOFT DELETE)
    // -------------------------------------------------------------
    // Como equipo de desarrollo estructuramos este endpoint para gestionar la
    // limpieza
    // de expedientes en la base de datos sin perder la trazabilidad histórica.
    // Aunque por seguridad operativa la interfaz gráfica (frontend) tiene el botón
    // de eliminar bloqueado, esta ruta se mantiene en el backend como una puerta
    // para futuras automatizaciones de mantenimiento o limpieza masiva de datos
    // antiguos.

    @Operation(summary = "Archivar pedido", description = "Realiza una eliminación lógica (archivado) del pedido. Solo permitido para estados finales (ENTREGADO/RECHAZADA).")
    @ApiResponses(value = {
            // La respuesta 200 indica que el expediente ha sido archivado correctamente, lo
            // que significa que el registro se ha marcado como inactivo en la base de datos
            // sin eliminarlo físicamente, preservando así la trazabilidad histórica.
            @ApiResponse(responseCode = "200", description = "Expediente archivado"),
            @ApiResponse(responseCode = "400", description = "El pedido no está en un estado finalizable")
    })
    // @Transactional garantiza que si falla el guardado del historial o el borrado,
    // se revierta toda la operación para no dejar la base de datos inconsistente.
    // @DeleteMapping("/{id}") permite eliminar un pedido por su ID.
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarPedidoLogico(
            @Parameter(description = "ID del pedido a archivar", example = "1") @PathVariable Long id) {

        // 1. VALIDACIÓN DE EXISTENCIA: Verificamos que el registro exista.
        // Si no se encuentra, retornamos un error 404 para proteger la integridad
        // referencial antes de intentar cualquier operación.
        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty())
            return ResponseEntity.status(404).body("Error: El pedido no existe.");

        Pedido pedido = pedidoOpt.get();
        String estado = pedido.getEstadoActual();

        // 2. PROTECCIÓN DEL FLUJO DE PRODUCCIÓN:
        // Nuestro equipo definió una regla estricta para mantener la integridad del
        // flujo.
        // El sistema bloquea la eliminación de cualquier pedido que aún esté en proceso
        // de fabricación. Solo permitimos operar sobre expedientes que hayan alcanzado
        // un estado final definitivo (ENTREGADO o RECHAZADA). Esto garantiza que ningún
        // proceso de impresión o diseño quede interrumpido o "huérfano".

        if (!"ENTREGADO".equals(estado) && !"RECHAZADA".equals(estado)) {
            return ResponseEntity.status(400)
                    .body("Error: Solo puedes archivar pedidos en estado ENTREGADO o RECHAZADA.");
        }

        // 3. TRAZABILIDAD PREVIA A LA ELIMINACIÓN:
        // Dejamos un último registro en el historial de auditoría indicando
        // el destino final del expediente antes de proceder con su limpieza.
        guardarHistorial(pedido, estado, "ELIMINADO_LOGICAMENTE");

        // 4. EJECUCIÓN EN BASE DE DATOS (NUEVA LÓGICA EN CASCADA):
        // Delegamos la eliminación al servicio para asegurar que se desactiven
        // también los detalles técnicos asociados sin borrarlos físicamente.
        pedidoService.eliminarPedido(id);

        return ResponseEntity.ok("Expediente archivado y procesado correctamente.");
    }

    // ----------------------------------------------------------------
    // MÉTODOS AUXILIARES (HELPERS DE NEGOCIO Y AUDITORÍA)
    // ---------------------------------------------------------------------------

    /**
     * Método Helper: Guardar Historial
     * Aplicamos el principio de (no repetir código). Como equipo centralizamos
     * la creación de registros de auditoría en este único método. Así evitamos
     * código repetido y aseguramos que cada transición de estado en el sistema
     * quede guardada de manera uniforme para futuras auditorías de calidad.
     */
    private void guardarHistorial(Pedido pedido, String estadoAntiguo, String estadoNuevo) {
        HistorialEstado historial = new HistorialEstado();
        historial.setPedido(pedido);
        historial.setEstadoAnterior(estadoAntiguo);
        historial.setEstadoNuevo(estadoNuevo);
        historialRepo.save(historial);
    }

    /**
     * Método Helper: Calcular Días Hábiles
     * Lógica de negocio fundamental estructurada por nuestro equipo para la gestión
     * de expectativas del cliente y la planificación interna del taller.
     * 
     * Garantiza que los tiempos prometidos sean días laborables reales, protegiendo
     * a la administración de reclamos por retrasos en fines de semana. Para
     * lograrlo,
     * aplicamos un ciclo 'while' que itera día a día e incrementa el contador de
     * días
     * solo si la fecha evaluada no es sábado ni domingo. Esto asegura que el
     * cálculo
     * final de la fecha de entrega estimada sea totalmente preciso.
     */
    private LocalDate calcularDiasHabiles(int diasAAgregar) {
        LocalDate fecha = LocalDate.now();
        int diasAgregados = 0;
        while (diasAgregados < diasAAgregar) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY)
                diasAgregados++;
        }
        return fecha;
    }

    private String obtenerFirmaCorporativa() {
        // Plantilla estandarizada para el pie de página de todos los correos
        // electrónicos

        return "\n\n\n------------------------------------------\n\n\n"
                + "Términos y Condiciones - Legión 3D\n"
                + "🛡️ Garantía de 30 días contra errores de impresión o fallos de taller.\n\n"
                + "📄 Lee nuestras políticas completas aquí: https://legion3d.cl/terminos-y-condiciones/\n"
                + "------------------------------------------\n\n"
                + "Síguenos y contáctanos:\n"
                // (aqui modificar al desplegar a produccion: actualizar el número de WhatsApp
                // oficial del negocio)
                + "💬 WhatsApp: https://api.whatsapp.com/send/?phone=56967879555\n"
                // (aqui modificar al desplegar a produccion: actualizar el enlace al perfil de
                // Instagram corporativo)
                + "📸 Instagram: https://www.instagram.com/legion3d.cl/";
    }

    @Operation(summary = "Registrar pago manual", description = "Permite a administración registrar un pago manual (efectivo, transferencia) y actualizar el saldo.")
    @PostMapping("/{id}/pago-manual")
    public ResponseEntity<?> registrarPagoManual(
            @PathVariable Long id,
            @RequestParam Double monto,
            @RequestParam String metodoPago,
            @RequestParam String concepto,
            @RequestParam(value = "evidencia", required = false) org.springframework.web.multipart.MultipartFile evidencia) {
        try {
            Pago pagoRealizado = pagoService.registrarPagoManual(id, monto, metodoPago, concepto, evidencia);
            boolean requiereDescarga = pagoRealizado.getReferenciaComprobante() != null
                    && !pagoRealizado.getReferenciaComprobante().isBlank();
            return ResponseEntity.ok(java.util.Map.of(
                    "mensaje", "Pago registrado con éxito.",
                    "id", pagoRealizado.getId(),
                    "requiereDescarga", requiereDescarga));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("error", "Error al registrar el pago: " + e.getMessage()));
        }
    }

    @Operation(summary = "Marcar pedido como garantía", description = "Establece el flag de garantía en true y ajusta el precio a $0 para no afectar métricas netas.")
    @PatchMapping("/{id}/garantia")
    public ResponseEntity<?> marcarGarantia(@PathVariable Long id) {
        try {
            Pedido actualizado = pedidoService.marcarComoGarantia(id);
            return ResponseEntity.ok("Pedido marcado como GARANTÍA. Precio ajustado a $0.0");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al procesar la garantía: " + e.getMessage());
        }
    }

    @Operation(summary = "Generar Garantía (Clon)", description = "Crea un clon técnico exacto del pedido original con sufijo -G y coste $0.")
    @PostMapping("/{id}/generar-garantia")
    public ResponseEntity<?> generarGarantiaClon(@PathVariable Long id) {
        try {
            Pedido clon = pedidoService.prepararGarantia(id);
            return ResponseEntity.ok(java.util.Map.of("mensaje",
                    "Garantía generada con éxito bajo el código: " + clon.getCodigoSeguimiento()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("error", "Error al clonar el expediente: " + e.getMessage()));
        }
    }

    /**
     * ENDPOINT: Subida de Archivos Físicos Técnicos
     *
     * Este endpoint recibe el archivo que el cliente sube desde el Formulario
     * Técnico.
     *
     * Flujo lógico:
     * 1. Busca el pedido usando el código de seguimiento LEG-XXXX.
     * 2. Valida que el archivo no venga vacío.
     * 3. Sube el archivo a Google Drive usando GoogleDriveService.
     * 4. Guarda en la base de datos SOLO texto:
     * - linkArchivoFinal
     * - driveFileId
     *
     * Importante:
     * La base de datos NO guarda el archivo pesado.
     * Solo guarda el link y el ID de Drive para mantener MySQL liviano.
     */
    @PostMapping("/{codigo}/archivos")
    public ResponseEntity<?> subirArchivo(
            @PathVariable String codigo,
            @RequestParam("archivo3d") org.springframework.web.multipart.MultipartFile file) {

        Optional<Pedido> pedidoOpt = pedidoRepo.findByCodigoSeguimiento(codigo);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Pedido no encontrado");
        }

        Pedido pedido = pedidoOpt.get();

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Archivo vacío");
            }

            /*
             * SUBIDA A GOOGLE DRIVE
             *
             * GoogleDriveService se encarga de:
             * - limpiar el nombre del archivo
             * - anteponer el prefijo LEG-XXXX-tec-
             * - ubicarlo en la carpeta correspondiente de Drive
             * - devolver webViewLink y fileId
             */
            GoogleDriveService.DriveUploadResult result = driveService.subirArchivo(file, codigo);

            /*
             * REGISTRO EN DETALLES TÉCNICOS
             *
             * Aquí solo guardamos referencias livianas:
             * - linkArchivoFinal: URL visible del archivo en Drive
             * - driveFileId: ID interno de Google Drive para descargar/purgar después
             */
            DetallesTecnicos dt = pedido.getDetallesTecnicos();

            if (dt == null) {
                dt = new DetallesTecnicos();
                dt.setPedido(pedido);
                dt.setActivo(true);
            }

            /*
             * BLINDAJE CONTRA SOBREESCRITURA
             *
             * Caso normal:
             * - Si no había link previo, se guarda el link de Drive.
             *
             * Caso mixto:
             * - Si el cliente pegó un link externo manual y además subió archivo,
             * guardamos ambos usando "|":
             *
             * linkManual|linkDrive
             *
             * Así después el frontend puede separar:
             * - links = enlace externo manual
             * - tec = archivo subido a Drive
             */
            String linkPrevio = dt.getLinkArchivoFinal();

            if (linkPrevio != null
                    && !linkPrevio.trim().isEmpty()
                    && !linkPrevio.contains(result.getWebViewLink())) {

                dt.setLinkArchivoFinal(linkPrevio + "|" + result.getWebViewLink());

            } else {
                dt.setLinkArchivoFinal(result.getWebViewLink());
            }

            dt.setDriveFileId(result.getFileId());
            detallesRepo.save(dt);

            return ResponseEntity.ok(java.util.Map.of(
                    "url", result.getWebViewLink(),
                    "driveId", result.getFileId()));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al subir a Google Drive: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT: Sustitución Express de Comprobante de Pago
     *
     * Este endpoint permite reemplazar la evidencia visual del pago.
     *
     * Uso típico:
     * - Luis está en el panel de auditoría bancaria.
     * - Selecciona un nuevo comprobante.
     * - El sistema lo sube a Drive.
     * - Se actualiza pedido.linkComprobantePago.
     *
     * Importante:
     * Esto NO altera la tabla pagos.
     * Solo reemplaza el link visual del comprobante asociado al pedido.
     */
    @PostMapping("/{codigo}/comprobante-pago")
    public ResponseEntity<?> subirComprobantePago(
            @PathVariable String codigo,
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile file) {

        Optional<Pedido> pedidoOpt = pedidoRepo.findByCodigoSeguimiento(codigo);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Pedido no encontrado");
        }

        Pedido pedido = pedidoOpt.get();

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Archivo vacío");
            }

            /*
             * SUBIDA A DRIVE COMO COMPROBANTE DE PAGO
             *
             * GoogleDriveService.subirComprobantePago(...)
             * debe guardar el archivo con nomenclatura:
             *
             * LEG-XXXX-pago-nombre_original.ext
             */
            // Calculamos el correlativo (tamaño de la lista actual + 1) para satisfacer el
            // 3er parámetro del servicio
            int correlativo = pedido.getPagos().size() + 1;
            GoogleDriveService.DriveUploadResult result = driveService.subirComprobantePago(file, codigo, correlativo);

            /*
             * Guardamos solo el link en la tabla pedidos.
             * La imagen/PDF real queda en Google Drive.
             */
            pedido.setLinkComprobantePago(result.getWebViewLink());
            pedidoRepo.save(pedido);

            return ResponseEntity.ok(java.util.Map.of(
                    "url", result.getWebViewLink(),
                    "mensaje", "Evidencia actualizada correctamente"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al subir comprobante: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT: Descargar Comprobante de Pago
     *
     * Este endpoint descarga la evidencia bancaria desde Google Drive.
     *
     * Regla de nombre obligatoria:
     *
     * LEG-XXXX-pago-nombre_original.ext
     *
     * Ejemplo:
     *
     * LEG-41F9-pago-voucher_transferencia.png
     *
     * Importante:
     * Si el archivo ya viene con el prefijo LEG-XXXX-pago- desde Drive,
     * NO lo duplica.
     */
    @Operation(summary = "Descargar comprobante de pago", description = "Descarga la evidencia bancaria con prefijo LEG-XXXX-pago-nombre_original.ext.")
    @GetMapping("/{id}/descargar-pago")
    public void descargarComprobantePago(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty()) {
            enviarErrorJson(response, 404, "NO_ENCONTRADO", "Pedido no existe.");
            return;
        }

        Pedido pedido = pedidoOpt.get();
        String linkPago = pedido.getUltimoComprobante();

        if (linkPago == null || linkPago.isBlank()) {
            enviarErrorJson(response, 404, "SIN_EVIDENCIA", "El cliente no ha subido comprobante.");
            return;
        }

        String driveId = extraerIdDeDrive(linkPago);
        if (driveId == null) {
            enviarErrorJson(response, 400, "LINK_INVALIDO", "El enlace no pertenece a Drive.");
            return;
        }

        try {
            com.google.api.services.drive.model.File metadata = driveService.obtenerMetadata(driveId);

            String originalName = metadata.getName() != null ? metadata.getName() : "comprobante_pago";
            String finalName = construirNombrePagoDescarga(pedido.getCodigoSeguimiento(), originalName);

            response.setContentType(
                    metadata.getMimeType() != null
                            ? metadata.getMimeType()
                            : "application/octet-stream");

            /*
             * ESTE ES EL PUNTO CLAVE:
             *
             * Antes se calculaba finalName, pero se descargaba metadata.getName().
             * Ahora sí se usa finalName para que el archivo descargado llegue como:
             *
             * LEG-XXXX-pago-nombre_original.ext
             */
            response.setHeader(
                    org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + finalName + "\"");

            if (metadata.getSize() != null) {
                response.setContentLengthLong(metadata.getSize());
            }

            try (java.io.OutputStream out = response.getOutputStream()) {
                driveService.descargarArchivo(driveId, out);
                out.flush();
            }

            System.out.println("✅ Comprobante descargado: " + finalName);

        } catch (Exception e) {
            enviarErrorJson(response, 500, "ERROR_DESCARGA", "Fallo al descargar de la nube: " + e.getMessage());
        }
    }

    /**
     * HELPER: Construir nombre de descarga para comprobantes de pago.
     *
     * Objetivo:
     * Garantizar este formato:
     *
     * LEG-XXXX-pago-nombre_original.ext
     *
     * También evita duplicar prefijos si el archivo ya fue subido a Drive con
     * ese mismo nombre.
     */
    private String construirNombrePagoDescarga(String trackingCode, String originalName) {
        String tracking = (trackingCode != null && !trackingCode.isBlank())
                ? trackingCode.trim().toUpperCase()
                : "LEG-SIN-CODIGO";

        String nombreSeguro = (originalName != null && !originalName.isBlank())
                ? originalName.trim()
                : "comprobante_pago";

        /*
         * Limpieza básica:
         * - Quita tildes y ñ.
         * - Reemplaza espacios por "_".
         * - Conserva puntos, guiones, guiones bajos y extensión.
         */
        nombreSeguro = nombreSeguro
                .replaceAll("[áàäâÁÀÄÂ]", "a")
                .replaceAll("[éèëêÉÈËÊ]", "e")
                .replaceAll("[íìïîÍÌÏÎ]", "i")
                .replaceAll("[óòöôÓÒÖÔ]", "o")
                .replaceAll("[úùüûÚÙÛÜ]", "u")
                .replaceAll("[ñÑ]", "n")
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");

        /*
         * Evita duplicar nombres.
         *
         * Ejemplos:
         * LEG-41F9-pago-voucher.png -> voucher.png
         * pago-voucher.png -> voucher.png
         */
        nombreSeguro = nombreSeguro.replaceFirst("^" + java.util.regex.Pattern.quote(tracking) + "[-_]+", "");
        nombreSeguro = nombreSeguro.replaceFirst("^pago[-_]+", "");

        if (nombreSeguro.isBlank()) {
            nombreSeguro = "comprobante_pago";
        }

        return tracking + "-pago-" + nombreSeguro;
    }

    @Operation(summary = "Descargar comprobante de despacho", description = "Busca y descarga la evidencia logístico/Starken desde Google Drive.")
    @GetMapping("/{id}/descargar-despacho")
    public void descargarComprobanteEnvio(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        Optional<Pedido> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty()) {
            enviarErrorJson(response, 404, "NO_ENCONTRADO", "Pedido no existe.");
            return;
        }

        Pedido pedido = pedidoOpt.get();
        String driveId = null;

        // BÚSQUEDA DINÁMICA EN DRIVE (Opción A):
        try {
            String queryName = "voucher_despacho_" + pedido.getCodigoSeguimiento() + ".txt";
            driveId = driveService.buscarArchivoPorNombre(queryName);
        } catch (Exception e) {
            System.err.println("⚠️ Error buscando voucher en Google Drive: " + e.getMessage());
        }

        // Fallback final: Si sigue siendo nulo, intentamos extraer del campo de envío
        // directo
        if (driveId == null && pedido.getLinkComprobanteEnvio() != null) {
            driveId = extraerIdDeDrive(pedido.getLinkComprobanteEnvio());
        }

        if (driveId == null) {
            enviarErrorJson(response, 404, "SIN_VOUCHER", "No se encontró el voucher de despacho en Google Drive.");
            return;
        }

        try {
            com.google.api.services.drive.model.File metadata = driveService.obtenerMetadata(driveId);
            String finalName = "voucher_despacho_" + pedido.getCodigoSeguimiento() + ".txt";

            response.setContentType("text/plain");
            response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + finalName + "\"");

            if (metadata.getSize() != null) {
                response.setContentLengthLong(metadata.getSize());
            }

            try (java.io.OutputStream out = response.getOutputStream()) {
                driveService.descargarArchivo(driveId, out);
                out.flush();
            }

        } catch (Exception e) {
            enviarErrorJson(response, 500, "ERROR_DESCARGA", "Fallo al descargar de la nube: " + e.getMessage());
        }
    }

    /**
     * HELPER: Respuesta JSON de error.
     *
     * Se usa para que el frontend pueda leer errores controlados
     * y mostrar SweetAlert2 en vez de una pantalla blanca.
     */
    private void enviarErrorJson(
            jakarta.servlet.http.HttpServletResponse response,
            int status,
            String code,
            String message) throws java.io.IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        response.getWriter().write(
                String.format(
                        "{\"error\": \"%s\", \"message\": \"%s\"}",
                        code,
                        message));
    }

    // ----------------------------------------------------------------------
    // REGLA DE NEGOCIO CRÍTICA: Búsqueda Profunda en Historial
    // ----------------------------------------------------------------------

    /**
     * ENDPOINT: Búsqueda profunda en historial
     *
     * Permite consultar registros históricos usando año y texto de búsqueda.
     */
    @Operation(summary = "Búsqueda profunda en historial", description = "Extrae registros de años anteriores filtrados por tracking, RUT o cliente.")
    @GetMapping("/historico/buscar")
    public ResponseEntity<List<Pedido>> buscarHistorico(
            @RequestParam String anio,
            @RequestParam String query) {

        /*
         * Si el usuario selecciona TODO, mandamos 0 como valor auxiliar.
         * Si selecciona un año concreto, lo convertimos a Integer.
         */
        Integer anioNum = anio.equals("TODO") ? 0 : Integer.parseInt(anio);

        /*
         * Esta consulta depende de tu PedidoRepository.
         * Debe existir el método buscarEnBovedaHistorica(...)
         */
        List<Pedido> resultados = pedidoRepo.buscarEnBovedaHistorica(anio, anioNum, query);

        return ResponseEntity.ok(resultados);
    }

}
