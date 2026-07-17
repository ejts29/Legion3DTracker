package cl.ipss.legion3d.tracker.backend.servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.scheduling.annotation.Async;
import java.util.Properties;

/**
 * Este archivo se llama EmailService y es el sistema circulatorio de nuestra aplicación.
 * Como ya definimos que el correo electrónico es nuestra llave principal de identificación 
 * y contacto con el cliente, esta clase es la encargada de orquestar toda la comunicación oficial.
 * 
 * Al estar anotada con @Service, el framework de Spring Boot asume el control de su ciclo de vida, 
 * permitiéndonos inyectar esta herramienta en cualquier parte de nuestra máquina de estados donde 
 * necesitemos disparar una notificación.
 */
@Service
public class EmailService {

    @Autowired
    private GmailApiService gmailApiService;

    /**
     * ENVÍO SIMPLE (Texto Plano)
     * Se usa para notificaciones rápidas y automatizadas, como el acuse de recibo cuando el cliente 
     * llena el formulario inicial en WordPress o cuando su pieza entra a la etapa de impresión.
     * 
     * A nivel de arquitectura, la anotación @Async es una de nuestras mejores decisiones de rendimiento. 
     * Le indica a nuestro servidor que envíe este correo en segundo plano (en un hilo separado). 
     * Gracias a esto, ni el panel de Luis ni la pantalla del cliente se quedarán congelados 
     * esperando los segundos que tarda el servidor de Google o Outlook en procesar el mensaje.
     */
    @Async
    public void enviarCorreoSimple(String destinatario, String asunto, String mensaje) {
        try {
            Session session = Session.getInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // (aqui modificar al desplegar a produccion: cambiar el remitente por el correo
            // oficial de notificaciones del cliente)
            helper.setFrom("notificaciones@legion3d.cl"); // Remitente oficial
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(mensaje);

            gmailApiService.enviarCorreoMime(mimeMessage);
            System.out.println(">>> [EMAIL SIMPLE GMAIL API] Enviado a: " + destinatario);

        } catch (Exception e) {
            System.err.println("⚠️ Error en envío simple Gmail API: " + e.getMessage());
        }
    }

    /**
     * ENVÍO CON ADJUNTO (Imágenes, PDFs, etc.)
     * Este es el método estrella y fundamental para la Etapa 4 de nuestro modelo de negocio.
     * Cuando Luis aprueba un proyecto y genera la cotización, utiliza este canal para enviarle 
     * al cliente el presupuesto formal junto con fotos técnicas o renders explicativos de las piezas.
     */
    public void enviarCorreoConAdjunto(String destinatario, String asunto, String mensaje, MultipartFile archivo) {
        try {
            // Creamos un mensaje "Mime", un protocolo de internet que nos permite construir un correo 
            // estructurado en múltiples partes para soportar tanto el texto como los archivos pesados.
            Session session = Session.getInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);

            // MimeMessageHelper es el asistente de Spring para configurar este correo complejo.
            // El valor 'true' activa explícitamente el modo "multipart" (permite adjuntos)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // (aqui modificar al desplegar a produccion: asegurar que el remitente coincida
            // con el configurado en application.properties)
            helper.setFrom("notificaciones@legion3d.cl");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(mensaje);

            // Filtro de seguridad: Verificamos que el archivo realmente exista y no esté corrupto 
            // antes de intentar pegarlo en el cuerpo del correo.
            if (archivo != null && !archivo.isEmpty()) {
                // Se extrae el nombre original del archivo para que el cliente lo reconozca al descargarlo
                helper.addAttachment(archivo.getOriginalFilename(), archivo);
                System.out.println(">>> [ADJUNTO GMAIL API] Pegando archivo: " + archivo.getOriginalFilename());
            }

            // Realizamos el envío físico a través de la API de Gmail
            gmailApiService.enviarCorreoMime(mimeMessage);
            System.out.println(">>> [EMAIL COMPLEJO GMAIL API] Enviado con éxito a: " + destinatario);

        } catch (Exception e) {
            // Si el envío falla por un problema externo (como un archivo excediendo el peso límite de la API
            // o una caída de red), capturamos el error para no hacer colapsar nuestra aplicación de golpe.
            System.err.println("⚠️ Falló el envío con adjunto Gmail API: " + e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo con la foto adjunta", e);
        }
    }

    /**
     * FIRMA CORPORATIVA
     * Método centralizado para mantener una identidad visual profesional y estandarizada 
     * en cada comunicación que sale de nuestro taller hacia los clientes.
     */
public String obtenerFirmaCorporativa() {
        // (aqui modificar al desplegar a produccion: actualizar la firma con el nombre
        // real de la empresa, dirección y sitio web definitivo)
        return "\n\n--------------------------------------------------\n" +
                "LEGIÓN 3D - Manufactura Avanzada\n" +
                "Santiago, Chile\n" +
                "www.legion3d.cl\n" +
                "--------------------------------------------------\n" +
                "Este es un mensaje generado automáticamente. Legión 3D Tracker B";
    }
}