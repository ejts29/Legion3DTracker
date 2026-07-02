package cl.ipss.legion3d.tracker.backend.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Este archivo se llama FormularioTecnicoDTO porque funciona como nuestro recolector maestro de requerimientos técnicos.
 * Cuando un cliente nos contacta inicialmente y avanzamos en la conversación, usamos este formulario 
 * para capturar todos los detalles finos del proyecto (medidas, materiales, tolerancias y logística).
 * 
 * A nivel técnico, este DTO (Data Transfer Object) nos permite recibir toda esa información extensa 
 * desde el frontend o la API de manera ordenada y segura. Así evitamos exponer nuestras entidades 
 * de base de datos directamente y garantizamos que el equipo reciba la información bien estructurada 
 * antes de que Luis inicie la cotización formal o pasemos a la fase de modelado 3D.
 * 
 * La anotación @Data de Lombok nos genera los métodos repetitivos, y @Schema documenta nuestra API para Swagger.
 */
@Data
@Schema(description = "DTO para recibir la información técnica detallada del pedido tras el contacto inicial")
public class FormularioTecnicoDTO {
    
    // --- DATOS DE CONTACTO ---
    
    /**
     * Teléfono directo del cliente. Lo guardamos aquí para que nuestro equipo pueda resolver dudas rápidas 
     * de ingeniería o coordinar detalles urgentes del diseño sin depender solo del correo electrónico.
     */
    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @JsonProperty("telefonoContacto")
    @Schema(description = "Teléfono de contacto directo", example = "+56912345678")
    private String telefonoContacto;

    // --- MEDIDAS Y UNIDADES ---
    
    /**
     * Dimensiones en el eje X de la pieza. Nos permite calcular el volumen de impresión y estimar el tiempo de máquina.
     */
    @Schema(description = "Ancho de la pieza (mm)", example = "50.0")
    private Double medidaAncho;
    
    /**
     * Dimensiones en el eje Z de la pieza.nos ayuda a entender la complejidad de la impresión y si necesitamos soportes o estructuras adicionales.
     */
    @Schema(description = "Alto de la pieza (mm)", example = "120.0")
    private Double medidaAlto;
    
    /**
     * Dimensiones en el eje Y de la pieza.nos da una idea del volumen total y si la pieza cabe en nuestras máquinas sin necesidad de dividirla en partes.
     */
    @Schema(description = "Profundidad de la pieza (mm)", example = "30.0")
    private Double medidaProfundidad;
    
    /**
     * Define cuántas copias idénticas debemos meter en la cama de impresión.nos ayuda a organizar la producción y a calcular el costo total del proyecto, ya que imprimir 10 unidades no es lo mismo que imprimir 1.
     */
    @Schema(description = "Cantidad de unidades a fabricar", example = "10")
    private Integer cantidadUnidades;

    // --- ESPECIFICACIONES TÉCNICAS ---
    
    /**
     * Tiempo límite acordado con el cliente. Nos ayuda a organizar la prioridad en nuestra cola de impresión.nos permite gestionar las expectativas del cliente y coordinar con nuestro equipo de producción para cumplir con los plazos establecidos.
     */
    @Schema(description = "Días estimados para la entrega", example = "5 días hábiles")
    private String diasEntrega;
    
    /**
     * Bandera que nos indica si el cliente nos enviará un objeto real para que nosotros le hagamos ingeniería inversa o clonado. nos ayuda a entender si debemos preparar nuestro proceso de escaneo 3D o si trabajaremos solo con archivos digitales proporcionados por el cliente.
     */
    @Schema(description = "Indica si el cliente posee la pieza física para replicar")
    private boolean tienePiezaFisica; 
    
    /**
     * Detalla si nosotros debemos intervenir el diseño original (por ejemplo: "hacer el orificio 2mm más ancho"). nos ayuda a planificar las modificaciones necesarias antes de iniciar la impresión.
     */
    @Schema(description = "Indica si el diseño requiere modificaciones técnicas", example = "Ajustar diámetro de eje")
    private String necesitaModificacion;
    
    /**
     * Confirmación crítica para nuestro equipo de calidad. Nos asegura que el cliente entiende y acepta 
     * el margen de error milimétrico y las líneas de capa propias de la impresión 3D.
     */
    @Schema(description = "Confirmación de tolerancia aceptada")
    private boolean toleranciaCheck; 
    
    /**
     * Nos dice si el cliente necesita un clon funcional estricto o si tenemos libertad geométrica mientras cumpla su función.
     */
    @Schema(description = "Indica si requiere una copia exacta del original")
    private boolean esCopiaExacta;    
    
    /**
     * Tipo de plástico o resina que utilizaremos en nuestras máquinas (ej. PLA, PETG, TPU).
     */
    @Schema(description = "Material solicitado para impresión", example = "PETG")
    private String materialSolicitado;
    
    /**
     * Color final del filamento, o si requiere que nosotros apliquemos post-procesado con pintura.
     */
    @Schema(description = "Color deseado", example = "Negro Mate")
    private String colorSolicitado;
    
    /**
     * Dato clave para que el administrador recomiende el material correcto. Si nos dicen "Exterior", 
     * sabremos inmediatamente que debemos evitar materiales como el PLA normal que se deforman con el sol o calor.
     */
    @Schema(description = "Entorno donde se usará la pieza (Interior/Exterior/Calor)", example = "Exterior con exposición UV")
    private String entornoUso;
    
    /**
     * Rango de dinero que el cliente espera gastar. Le ayuda a Luis a evaluar la viabilidad y ajustar la propuesta comercial.
     */
    @Schema(description = "Presupuesto estimado por el cliente", example = "Entre 20k y 40k")
    private String presupuestoEstimado;
    
    /**
     * Operador logístico o forma de retiro que el cliente prefiere para recibir su proyecto terminado.
     */
    @Schema(description = "Método de entrega preferido", example = "Starken")
    private String metodoEntrega;

    // --- DIRECCIÓN  ---
    
    /**
     * Ubicación geográfica principal para calcular nuestros costos de envío territorial.
     */
    @Schema(description = "Región de destino", example = "Valparaíso")
    private String region;
    
    /**
     * Subdivisión territorial para afinar el costo del flete.
     */
    @Schema(description = "Comuna de destino", example = "Viña del Mar")
    private String comuna;
    
    /**
     * Dirección base para que nosotros generemos la etiqueta de despacho.
     */
    @Schema(description = "Calle y número", example = "Libertad 456")
    private String calleYNumero;
    
    /**
     * Datos secundarios del domicilio para asegurar la entrega.
     */
    @Schema(description = "Departamento, casa u oficina", example = "Depto 201")
    private String deptoCasaOficina;
    
    // (aqui modificar al desplegar a produccion: asegurar que el límite de caracteres en el formulario web coincida con la base de datos para evitar errores de truncado)
    /**
     * Instrucciones logísticas especiales para el repartidor (ej. "Tocar timbre fuerte" o "Dejar en conserjería").
     */
    @Schema(description = "Información adicional para el despacho", example = "Dejar en conserjería")
    private String informacionAdicional;
    
    /**
     * Especifica si debemos enviar la encomienda a una sucursal de la empresa de transportes o directo a la puerta del cliente.
     */
    @Schema(description = "Tipo de envío por Starken (Domicilio/Sucursal)", example = "Domicilio")
    private String tipoEnvioStarken; 

    /**
     * Captura la decisión del cliente: 'usarAnterior' o 'subirNuevo'.
     */
    @Schema(description = "Decisión sobre el archivo inicial (mantener o reemplazar)", example = "subirNuevo")
    private String modoArchivo;

    // (aqui modificar al desplegar a produccion: validar que los enlaces de archivos pesados .STL/.STEP tengan permisos de lectura para el servidor)
    /**
     * Ruta virtual segura (como un enlace a Drive) donde nuestro cliente nos deja el archivo STL u OBJ original.
     */
    @Schema(description = "Enlace al archivo final de diseño (STL/STEP)", example = "https://drive.google.com/file/d/ejemplo")
    private String linkArchivoFinal;

    /**
     * Identificador nacional del cliente. Por ahora lo capturamos como texto libre, 
     * permitiéndonos generar las etiquetas de despacho y la facturación sin bloquear el proceso inicial con validaciones complejas.
     */
    @NotBlank(message = "El RUT es obligatorio para la facturación")
    @JsonProperty("rut")
    @Schema(description = "RUT del cliente para facturación/despacho", example = "12.345.678-9")
    private String rut;
}