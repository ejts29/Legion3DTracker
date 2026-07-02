package cl.ipss.legion3d.tracker.backend.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Este archivo se llama PresupuestoDTO porque es el documento oficial que nuestro sistema 
 * le presenta al cliente una vez que Luis ha evaluado los requerimientos técnicos. 
 * Su función es empaquetar y transportar el costo total del trabajo y el plazo de tiempo en el que 
 * nos comprometemos a entregarlo, permitiendo que el cliente tome la decisión final de aceptar el trato.
 * 
 * A nivel técnico, implementamos este DTO (Data Transfer Object) para encapsular estos dos datos críticos 
 * y llevarlos de forma estructurada desde nuestro panel de administración hacia nuestra base de datos, 
 * actualizando el pedido a su fase de "COTIZADO".
 * 
 * La anotación @Data de Lombok nos genera los métodos repetitivos, y @Schema documenta nuestra API para Swagger.
 */
@Data
@Schema(description = "DTO para formalizar el presupuesto y fecha de entrega hacia el cliente")
public class PresupuestoDTO {
    
    /**
     * Almacena el valor monetario definitivo que Luis ha calculado para el proyecto de impresión 3D.
     */
    @Schema(description = "Monto final pactado para el trabajo", example = "55000.0")
    // (aqui modificar al desplegar a produccion: si se requiere manejo de divisas internacionales, cambiar Double por BigDecimal para mayor precisión financiera)
    private Double precioFinal;
    
    /**
     * Registra el día exacto en que nos comprometemos a tener la pieza lista para el cliente.
     * Aquí aplicamos @JsonFormat con el patrón "dd-MM-yyyy" por una razón técnica y comercial vital: 
     * asegurar nuestro formato de fechas local (día-mes-año). 
     * 
     * Si no forzamos este patrón explícitamente en el DTO, nuestro servidor backend (Spring Boot) 
     * y el navegador del cliente podrían sufrir problemas de interpretación intentando leer la fecha 
     * en formato estadounidense (mes-día-año). Sin esta validación, un plazo pactado para el 10 de mayo (10-05) 
     * podría guardarse erróneamente en nuestra base de datos como el 5 de octubre (05-10). 
     * Con esta línea, garantizamos que todo nuestro ecosistema hable exactamente el mismo idioma temporal.
     */
    @Schema(description = "Fecha estimada de entrega (dd-MM-yyyy)", example = "15-06-2024")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate fechaEntregaEstimada;
}

