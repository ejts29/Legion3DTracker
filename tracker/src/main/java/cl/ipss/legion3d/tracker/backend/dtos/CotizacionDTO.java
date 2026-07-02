package cl.ipss.legion3d.tracker.backend.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Este archivo se llama CotizacionDTO porque su función es actuar como una "encomienda" digital 
 * que viaja exclusivamente desde el dashboard de administración hacia el servidor. 
 * Su objetivo principal es transportar el precio exacto que Luis ha calculado para un trabajo de impresión, 
 * permitiendo que el sistema actualice el pedido y avance a la fase de cobro.
 * 
 * A nivel técnico, es un DTO (Data Transfer Object). Un DTO es un patrón de diseño estructural 
 * que funciona como un contenedor de datos plano. Su propósito es encapsular la información 
 * y llevarla de forma segura desde el cliente (frontend) hacia el controlador (backend), 
 * validando los datos antes de que toquen la base de datos real.
 * 
 * La anotación @Data (de Lombok) genera automáticamente constructores, getters, setters y métodos base en tiempo de compilación.
 */
@Data
@Schema(description = "DTO para el envío de la cotización final al cliente")
public class CotizacionDTO {

    /**
     * Representa el valor monetario final que se le cobrará al cliente por el servicio.
     * Utilizamos validaciones estrictas de Jakarta para proteger la integridad del negocio:
     * La anotación @NotNull asegura que no se pueda enviar una cotización vacía,
     * y @Positive garantiza que el sistema rechace valores numéricos irreales (evitando errores graves como cobrar -$5000).
     */
    @Schema(description = "Monto final a cobrar al cliente", example = "45000.0")
    @NotNull(message = "El precio final es obligatorio")
    @Positive(message = "El precio debe ser mayor a cero")
    private Double precioFinal;
    
    /**
     * Campo de texto opcional destinado al uso de administración.
     * Sirve para que Luis deje un registro claro sobre qué incluye exactamente ese precio 
     * (por ejemplo, tiempo extra de diseño, post-procesado con lija, tipo de resina especial, etc.).
     */
    @Schema(description = "Comentario administrativo sobre el trabajo", example = "Incluye post-procesado con lija y acabado mate.")
    private String comentarioAdmin;
}