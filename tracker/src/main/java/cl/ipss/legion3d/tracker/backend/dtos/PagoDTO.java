package cl.ipss.legion3d.tracker.backend.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Este archivo se llama PagoDTO porque es la estructura que diseñamos para gestionar la validación financiera de nuestro sistema.
 * 
 * En la versión actual que vamos a desplegar, el flujo de pago funciona mediante un botón en el frontend 
 * que simula esta acción para hacer avanzar nuestra máquina de estados. Sabemos que de momento esto se quedará así. 
 * Sin embargo, programamos este DTO dejando estratégicamente la puerta abierta para una futura actualización. 
 * Lo hicimos de esta manera porque es lo que corresponde a nivel de buena arquitectura: dejamos el "molde" listo hoy, 
 * para que el día de mañana, cuando integremos un sistema real de subida de archivos, nuestro backend ya esté 
 * preparado para recibir y procesar ese dato sin tener que reescribir la base del código.
 * 
 * La anotación @Data de Lombok nos genera los métodos repetitivos, y @Schema documenta nuestra API para Swagger.
 */
@Data
@Schema(description = "DTO para que el cliente suba el comprobante de transferencia")
public class PagoDTO {


    /**
     * Almacena la ruta virtual (URL) del comprobante.
     * Por ahora, como definimos para esta versión, recibe un dato simulado por nuestro botón para mantener el flujo operativo. 
     * En una futura actualización, este mismo campo será el encargado de atrapar el enlace real del archivo alojado 
     * en un bucket de la nube, entregándole a Luis la prueba visual y definitiva del pago de sus clientes.
     */
        // (aqui modificar al desplegar a produccion: implementar validación de extensión de archivo (JPG/PNG/PDF) para evitar inyección de archivos maliciosos)
    @Schema(description = "Enlace al comprobante de pago subido", example = "https://firebasestorage.googleapis.com/v0/b/ejemplo/comprobante.jpg")
    private String linkComprobantePago;
}