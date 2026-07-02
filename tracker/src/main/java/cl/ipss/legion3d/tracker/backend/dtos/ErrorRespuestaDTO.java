package cl.ipss.legion3d.tracker.backend.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Este archivo se llama ErrorRespuestaDTO porque es el encargado de dar la cara cuando algo sale mal en nuestro sistema. 
 * Su función es atrapar cualquier fallo del servidor (como cuando buscamos un pedido que no existe o ingresamos un dato inválido) 
 * y transformarlo en un mensaje estructurado, limpio y profesional. De esta forma, evitamos que nuestro frontend 
 * o la pantalla del cliente se rompan mostrando código ilegible y, en su lugar, les entregamos una alerta clara.
 * 
 * A nivel técnico, implementamos este DTO diseñado específicamente para el manejo global de nuestras excepciones.
 * Estandarizar las respuestas de error es una práctica que nos facilita enormemente la depuración 
 * y nos permite mejorar la experiencia final de nuestros usuarios.
 * 
 * La anotación @Data de Lombok nos genera los métodos necesarios, y @Schema nos documenta el formato para Swagger.
 */
@Data
@Schema(description = "DTO estándar para estructurar y devolver respuestas de error limpias desde la API")
public class ErrorRespuestaDTO {
    
    /**
     * Registra el momento exacto en el que ocurrió la falla. 
     * Es vital para que podamos cruzar este dato con los logs de nuestro servidor 
     * y encontrar la causa raíz del problema rápidamente investigando a esa hora específica.
     */
    @Schema(description = "Fecha y hora del error", example = "2024-05-23T15:30:00")
    private LocalDateTime fecha;
    
    /**
     * El código numérico estándar de la web que utilizamos para indicar qué tipo de problema ocurrió.
     * Por ejemplo, 404 (No encontrado), 400 (Petición incorrecta por datos malos) o 500 (Error interno de nuestro servidor).
     */
    @Schema(description = "Código de estado HTTP", example = "404")
    private int estadoHttp;
    
    /**
     * El nombre técnico o categoría del error asociado al código HTTP.
     * Nos sirve para que nuestro sistema frontend sepa cómo clasificar la falla.
     */
    @Schema(description = "Tipo de error técnico", example = "Not Found")
    private String tipoError;
    
    /**
     * El mensaje humano que leerá el administrador (Luis) o el cliente en la pantalla de nuestra aplicación.
     * En lugar de mostrarles un error crudo de nuestra base de datos, les entregamos una explicación clara como "El pedido con ID 10 no existe".
     */
    @Schema(description = "Mensaje comprensible para el usuario o desarrollador", example = "El pedido con ID 10 no existe.")
    private String mensajeAmigable;
    
    // (aqui modificar al desplegar a produccion: evaluar si exponer la 'rutaAfectada' es un riesgo de seguridad en el entorno final)
    /**
     * Indica el endpoint o la URL exacta que intentó ejecutar la acción fallida.
     * Nos ayuda a saber qué parte precisa de nuestra API está fallando.
     */
    @Schema(description = "URL donde ocurrió el error", example = "/api/pedidos/10")
    private String rutaAfectada;

    /**
     * Constructor personalizado que armamos para instanciar el objeto de error rápidamente desde cualquier parte de nuestro código donde ocurra una excepción.
     * Al usarlo, capturamos automáticamente la fecha y hora actual,
     * ahorrándonos la tarea de pasar ese parámetro de tiempo manualmente cada vez que detectamos un fallo.
     */
    public ErrorRespuestaDTO(int estadoHttp, String tipoError, String mensajeAmigable, String rutaAfectada) {
        this.fecha = LocalDateTime.now(); // Guardamos el momento exacto del error de forma automática
        this.estadoHttp = estadoHttp;
        this.tipoError = tipoError;
        this.mensajeAmigable = mensajeAmigable;
        this.rutaAfectada = rutaAfectada;
    }
}