package cl.ipss.legion3d.tracker.backend.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Este archivo se llama MensajeDTO porque es nuestro canal de comunicación directa desde el panel de control. 
 * Su función es capturar los correos electrónicos manuales o las notificaciones personalizadas que nuestro administrador (Luis) 
 * necesita enviarle al cliente en cualquier etapa del proyecto (por ejemplo, para consultar una duda sobre el diseño 
 * o informar sobre un ajuste en las medidas).
 * 
 * A nivel técnico, implementamos este DTO (Data Transfer Object) para empaquetar el texto del mensaje, 
 * llevándolo de forma segura desde nuestra interfaz gráfica hasta el backend, donde nuestro servicio de correos 
 * se encargará de despacharlo hacia la bandeja de entrada del destinatario.
 * 
 * La anotación @Data de Lombok nos genera los métodos repetitivos, y @Schema documenta nuestra API para Swagger.
 */
@Data
@Schema(description = "DTO para el envío de mensajes personalizados o notificaciones al cliente")
public class MensajeDTO {
    
    /**
     * El título del correo electrónico. 
     * Nos permite que el administrador defina claramente de qué trata la notificación antes de que el cliente abra el mensaje.
     */
    @Schema(description = "Asunto del correo electrónico", example = "Actualización sobre su pedido de engranajes")
    private String asunto; 
    
    /**
     * El texto principal o la explicación detallada que nuestro equipo quiere transmitir.
     * Es el cuerpo literal del correo donde Luis escribe las instrucciones, alertas o actualizaciones de estado.
     */
    @Schema(description = "Contenido del mensaje a enviar", example = "Hola Juan, te informamos que hemos optimizado el diseño STL para una mejor resistencia.")
    private String cuerpoMensaje; 
}