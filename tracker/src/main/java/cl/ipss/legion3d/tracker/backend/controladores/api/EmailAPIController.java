package cl.ipss.legion3d.tracker.backend.controladores.api;
// el package indica la ubicación de esta clase dentro del proyecto

//import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import cl.ipss.legion3d.tracker.backend.servicios.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
// import: Trae las librerías necesarias para usar clases de otros paquetes sin escribir su ruta completa.
// Indica de dónde vienen las funciones para manejar solicitudes HTTP, procesar archivos subidos y construir respuestas.

// Esta clase es un controlador REST que maneja las solicitudes relacionadas con el envío de correos electrónicos.
@RestController
// La ruta base para todas las solicitudes en este controlador será
// "/api/v1/emails".
@RequestMapping("/api/v1/emails")
@Tag(name = "Módulo de Correos", description = "Endpoints para el envío de comunicaciones libres o vinculadas a pedidos")
public class EmailAPIController {

    // Inyecta el servicio de correo para poder usar sus métodos en este
    // controlador.
    @Autowired
    private EmailService emailService;

    // Este método maneja las solicitudes POST a "/api/v1/emails/enviar-libre" para
    // enviar
    // correos electrónicos con o sin ar chivos adjuntos.
    /**
     * ENDPOINT: Envío de Correo Libre
     * Este endpoint permite al administrador enviar mensajes personalizados a
     * cualquier dirección.
     * Procesa peticiones del tipo Multipart para soportar la subida de archivos
     * adjuntos.
     */

  
    @PostMapping("/enviar-libre")
    public ResponseEntity<?> enviarCorreoLibre(
        // Utilizamos Anotaciones @RequestParam para recibir los parámetros del formulario de envío de correo.
            @Parameter(description = "Correo electrónico del receptor", example = "cliente@ejemplo.com") @RequestParam("destinatario") String destinatario,

            // Utilizamos anotaciones @Parameter para documentar cada parámetro en Swagger, proporcionando una descripción y un ejemplo de valor esperado.
            @Parameter(description = "Título del mensaje", example = "Actualización de su proyecto 3D") @RequestParam("asunto") String asunto,
            @Parameter(description = "Cuerpo del mensaje en formato texto", example = "Hola, adjunto los renders de su pedido.") @RequestParam("mensaje") String mensaje,
            // (Opcional) Array de archivos para permitir múltiples adjuntos
            @Parameter(description = "Archivos adjuntos (Opcional)") @RequestParam(value = "adjuntos", required = false) MultipartFile[] adjuntos) {

        // Este bloque try-catch se utiliza para manejar cualquier excepción que pueda ocurrir durante el proceso de envío de correo, asegurando que el sistema responda con un mensaje de error adecuado en caso de fallos.

        // Validamos que los parámetros requeridos no sean nulos o vacíos antes de intentar enviar el correo. Si falta alguno, respondemos con un error 400 (Bad Request) indicando que faltan parámetros.
        try {
            if (destinatario == null || asunto == null || mensaje == null) {
                return ResponseEntity.badRequest().body("{\"error\": \"Faltan parámetros requeridos.\"}");
            }

            // Si hay adjuntos, usamos un método diferente en el EmailService
            if (adjuntos != null && adjuntos.length > 0) {
                // Iteramos por si mandó más de uno para adjuntar cada archivo al mismo correo
                // base
                for (MultipartFile archivo : adjuntos) {
                    emailService.enviarCorreoConAdjunto(destinatario, asunto, mensaje, archivo);
                }
                // Si se envió con adjuntos, respondemos con un mensaje específico
            } else {
                // Si no hay archivo, enviamos correo simple (texto plano solamente)
                emailService.enviarCorreoSimple(destinatario, asunto, mensaje);
            }

            // Si todo sale bien, respondemos con un mensaje de éxito en la respuesta HTTP 200 (OK)
            return ResponseEntity.ok().body("{\"message\": \"Correo enviado exitosamente.\"}");
            // Si ocurre algún error, respondemos con un mensaje de error genérico y un código de estado HTTP 500 (Internal Server Error)
        } catch (Exception e) {
            // En caso de error, se captura la excepción y se devuelve una respuesta con el mensaje de error incluido en el cuerpo de la respuesta.
            return ResponseEntity.status(500).body("{\"error\": \"Error interno: " + e.getMessage() + "\"}");
        }
    }
}