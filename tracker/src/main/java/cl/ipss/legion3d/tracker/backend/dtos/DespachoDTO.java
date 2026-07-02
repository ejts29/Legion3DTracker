package cl.ipss.legion3d.tracker.backend.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Este archivo se llama DespachoDTO porque su función principal es actuar como la hoja de ruta final del proceso logístico. 
 * Su objetivo es capturar y transportar la información de entrega (como números de seguimiento de Starken o enlaces de descarga de modelos) 
 * desde el panel de administración hacia el servidor cuando un pedido de impresión 3D está listo para llegar al cliente.
 * 
 * A nivel técnico, es un DTO (Data Transfer Object). Un DTO es un patrón de diseño estructural 
 * que funciona como un contenedor de datos plano. Su propósito principal es encapsular la información 
 * y llevarla de forma segura desde el cliente (frontend o API) hacia el controlador (backend), 
 * desacoplando la capa de presentación de la base de datos.
 * 
 * La anotación @Data (de Lombok) genera automáticamente constructores, getters, setters y métodos base en tiempo de compilación.
 */
@Data
@Schema(description = "DTO para registrar los datos de despacho y entrega del pedido")
public class DespachoDTO {
    
    /**
     * Almacena la URL o el número de seguimiento de la empresa de transportes.
     * Es fundamental para que el cliente pueda rastrear su paquete físico (por ejemplo, mediante Starken, Chilexpress o entrega particular).
     */
    @Schema(description = "Enlace al comprobante de envío de Starken o transportista", example = "https://starken.cl/seguimiento/123456789")
    private String linkComprobanteEnvio;
    
    /**
     * Diseñado para manejar la entrega de productos virtuales.
     * En casos donde el cliente compró un diseño 3D o un servicio mixto, aquí se guarda el enlace seguro 
     * hacia la carpeta de Google Drive o Mega que contiene los archivos finales.
     */
    @Schema(description = "Enlace a archivos digitales (Drive/Mega) para envíos mixtos", example = "https://drive.google.com/drive/folders/ejemplo")
    private String linkArchivoDigital;
    
    /**
     * Registra la decisión final sobre la logística.
     * Sirve para indicar cómo se entregó realmente el producto, ya que en ocasiones lo que el cliente selecciona 
     * al inicio puede cambiar por decisiones operativas de Luis durante el cierre del pedido.
     */
    @Schema(description = "Método de entrega real utilizado", example = "Starken")
    private String metodoRealLuis;
}