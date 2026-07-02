package cl.ipss.legion3d.tracker.backend.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Este archivo se llama CambioEstadoDTO porque su misión es capturar los datos exactos 
 * cada vez que un pedido necesita cambiar de fase. Funciona como el puente directo que conecta 
 * los botones de la interfaz gráfica (cuando el equipo avanza o rechaza una solicitud) con los 
 * controladores del backend que ejecutan y guardan ese nuevo estado en la base de datos.
 * 
 * DTO (Data Transfer Object) utilizado para gestionar las transiciones de estado de los pedidos.
 * 
 * Un DTO es un patrón de diseño estructural que actúa como un contenedor de datos plano. 
 * Su propósito principal es encapsular y transportar la información desde el cliente (frontend o API) 
 * hacia el controlador (backend) sin exponer las entidades reales de la base de datos. 
 * Esto aumenta la seguridad y desacopla la arquitectura del sistema.
 * 
 * En el contexto del Legión 3D Tracker, esta clase captura la intención del equipo (como Luis) 
 * de avanzar un pedido en el flujo de trabajo (por ejemplo, de "A_COTIZACION" a "PRESUPUESTADO") 
 * permitiendo además adjuntar notas operativas vitales para el historial.
 * 
 * La anotación @Data (de Lombok) genera automáticamente constructores, getters, setters y métodos base en tiempo de compilación.
 */
@Data
@Schema(description = "Objeto de transferencia para solicitar y registrar el cambio de fase de un pedido en el flujo de impresión 3D")
public class CambioEstadoDTO {
    
    /**
     * Define la fase exacta a la que pasará el requerimiento.
     * La anotación @NotBlank de Jakarta Validation actúa como un primer filtro de seguridad, 
     * garantizando que la petición HTTP sea rechazada automáticamente si este campo viene nulo o vacío.
     */
    @Schema(description = "Identificador del nuevo estado del pedido (ej. EN_PRODUCCION, A_COTIZACION, LISTO_PARA_ENTREGA)", example = "EN_PRODUCCION")
    @NotBlank(message = "El nuevo estado es un parámetro obligatorio para realizar la transición")
    private String nuevoEstado;
    
    /**
     * Campo de texto opcional para registrar el contexto del cambio de estado.
     * Es una pieza clave para la trazabilidad del proyecto, ya que permite al equipo de diseño 
     * o a Luis dejar anotaciones técnicas, motivos de rechazo, especificaciones de material 
     * o apuntes directos sobre la cotización en la bitácora del pedido.
     */
    @Schema(description = "Mensaje opcional para alimentar el historial del pedido (Notas de triage, detalles de ingeniería o comentarios comerciales)", example = "Modelo validado, requiere soportes en árbol. Pasa a producción.")
    private String mensajeTriage; 
}