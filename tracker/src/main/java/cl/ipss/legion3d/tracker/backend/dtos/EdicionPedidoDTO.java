package cl.ipss.legion3d.tracker.backend.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/**
 * Este archivo se llama EdicionPedidoDTO porque funciona como el formulario maestro 
 * o la "llave maestra" de administración. Cuando el equipo necesita corregir un error 
 * tipográfico, actualizar medidas o cambiar fechas directamente desde el panel de control, 
 * este archivo atrapa todos esos datos editados y los transporta de forma segura al servidor.
 * 
 * A nivel técnico es un DTO (Data Transfer Object) de gran tamaño. 
 * Su objetivo es consolidar y validar la información modificada del cliente, 
 * del pedido y de los requerimientos técnicos antes de sobreescribir los registros en la base de datos real.
 * Esto previene que un error de tipeo en el dashboard corrompa la información vital del proyecto.
 * 
 * La anotación @Data de Lombok se encarga de generar el código repetitivo en segundo plano.
 */
@Data
@Schema(description = "DTO para la edición completa de un pedido desde el dashboard")
public class EdicionPedidoDTO {
    // --- Datos del Cliente ---
    
    /**
     * Identificación del cliente. 
     * Se limita a 100 caracteres para evitar desbordamientos en la base de datos o ingresos maliciosos.
     */
    // La anotación @Size valida que el nombre no exceda el límite establecido.
    @Schema(description = "Nombre del cliente", example = "Carlos Soto")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;

    /**
     * Canal principal de comunicación. 
     * La anotación @Email valida internamente que la cadena de texto contenga un formato correcto.
     */
    /// Se limita a 150 caracteres para evitar desbordamientos o intentos de inyección de código.
    @Schema(description = "Correo electrónico", example = "carlos.soto@email.com")
    @Email(message = "Debe ser un correo válido")
    @Size(max = 150, message = "El email es demasiado largo")
    private String email;

    /**
     * Número de contacto para logística o consultas urgentes.
     */
    // Se limita a 20 caracteres para permitir formatos internacionales sin exceder el espacio de almacenamiento.
    @Schema(description = "Teléfono de contacto", example = "+56987654321")
    @Size(max = 20, message = "El teléfono es demasiado largo")
    private String telefono;

    /**
     * Identificador tributario o nacional del cliente para efectos de facturación.
     */
    // Se limita a 15 caracteres para acomodar formatos de RUT chilenos y otros identificadores internacionales.
    @Schema(description = "RUT del cliente", example = "12.345.678-9")
    @Size(max = 15, message = "El RUT es demasiado largo")
    private String rut;

    // --- Datos del Pedido ---
    
    /**
     * Descripción general de lo que el cliente quiere lograr.
     */
    // Se limita a 255 caracteres para permitir una descripción detallada sin exceder el espacio de almacenamiento.
    @Schema(description = "Servicio solicitado", example = "Modelado 3D e Impresión")
    @Size(max = 255, message = "La descripción del servicio es demasiado larga")
    private String servicioSolicitado;

    /**
     * Monto que la administración decide cobrar tras la edición. 
     * @PositiveOrZero permite que existan trabajos gratuitos (costo cero) por garantías o piezas de cortesía.
     */
    // Se limita a 10 dígitos para evitar problemas de almacenamiento y asegurar que el precio sea razonable.
    @Schema(description = "Precio final acordado", example = "85000.0")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    private Double precioFinal;
    
    /**
     * Fecha pactada para tener el trabajo listo.
     * @JsonFormat asegura que el servidor entienda exactamente el formato de fecha que envía el frontend.
     */
    // No se valida con @Future o @Past para permitir flexibilidad en la edición, aunque el equipo de control de calidad puede revisar manualmente fechas que no tengan sentido.
    @Schema(description = "Fecha estimada de entrega (yyyy-MM-dd)", example = "2024-05-30")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaEntregaEstimada;

    // --- Detalles Técnicos (Ingeniería y Logística) ---
    
    @Schema(description = "Ancho en mm", example = "150.5")
    @PositiveOrZero(message = "El ancho debe ser un valor positivo o cero")
    private Double medidaAncho;

    /**
     * Eje Z del modelo.
     */
    // Se limita a 5 dígitos enteros y 2 decimales para permitir medidas precisas sin exceder el espacio de almacenamiento.
    @Schema(description = "Alto en mm", example = "200.0")
    @PositiveOrZero(message = "El alto debe ser un valor positivo o cero")
    private Double medidaAlto;

    /**
     * Eje Y del modelo.
     */
    // Se limita a 5 dígitos enteros y 2 decimales para permitir medidas precisas sin exceder el espacio de almacenamiento.
    @Schema(description = "Profundidad en mm", example = "100.0")
    @PositiveOrZero(message = "La profundidad debe ser un valor positivo o cero")
    private Double medidaProfundidad;

    /**
     * Volumen de producción de la pieza. @Min asegura que al menos se imprima una copia.
     */
    // Se limita a 5 dígitos para permitir cantidades razonables sin exceder el espacio de almacenamiento.
    @Schema(description = "Cantidad de copias", example = "5")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidadUnidades;

    /**
     * Tipo de filamento o resina seleccionada para la máquina (PLA, PETG, Resina, etc).
     */
    // Se limita a 50 caracteres para permitir nombres descriptivos sin exceder el espacio de almacenamiento.
    @Schema(description = "Material de fabricación", example = "PLA+")
    @Size(max = 50, message = "El material es demasiado largo")
    private String materialSolicitado;

    /**
     * Tonalidad final requerida por el cliente.
     */
    // Se limita a 50 caracteres para permitir nombres descriptivos sin exceder el espacio de almacenamiento.
    @Schema(description = "Color de la pieza", example = "Gris Espacial")
    @Size(max = 50, message = "El color es demasiado largo")
    private String colorSolicitado;

    /**
     * Operador logístico o método de retiro pactado.
     */
    // Se limita a 100 caracteres para permitir nombres descriptivos sin exceder el espacio de almacenamiento.
    @Schema(description = "Método de entrega", example = "Starken")
    @Size(max = 100, message = "El método de entrega es demasiado largo")
    private String metodoEntrega;

    /**
     * Ubicación geográfica principal para envíos territoriales.
     */
    // Se limita a 100 caracteres para permitir nombres descriptivos sin exceder el espacio de almacenamiento.
    @Schema(description = "Región de despacho", example = "Metropolitana")
    @Size(max = 100, message = "La región es demasiado larga")
    private String region;

    /**
     * Subdivisión territorial para afinar el costo del envío logístico.
     */
    // Se limita a 100 caracteres para permitir nombres descriptivos sin exceder el espacio de almacenamiento.
    @Schema(description = "Comuna de despacho", example = "Providencia")
    @Size(max = 100, message = "La comuna es demasiada larga")
    private String comuna;

    /**
     * Dirección exacta de destino para generar la etiqueta de despacho.
     */
    // Se limita a 255 caracteres para permitir direcciones detalladas sin exceder el espacio de almacenamiento.
    @Schema(description = "Calle y número de domicilio", example = "Av. Providencia 1234")
    @Size(max = 255, message = "La dirección es demasiado larga")
    private String calleYNumero;

    /**
     * Bandera booleana que le indica al equipo de diseño si el cliente enviará una pieza original física para su clonado.
     */
    // No se valida con @NotNull para permitir que el equipo de control de calidad revise manualmente casos donde esta información no tenga sentido o se haya dejado en blanco.
    @Schema(description = "¿Tiene pieza física?", example = "true")
    private Boolean tienePiezaFisica;

    /**
     * Ruta virtual segura (como un enlace a Drive) donde se almacena el archivo STL u OBJ definitivo listo para la laminadora.
     */
    // Se limita a 255 caracteres para permitir enlaces detallados sin exceder el espacio de almacenamiento.
    @Schema(description = "Link al archivo final", example = "http://link.com/file.stl")
    private String linkArchivoFinal;

    private String linkFormularioIngenieria;

    // Nuevos campos de Detalles Tecnicos (Paridad DB)
    private String presupuestoEstimado;
    private String diasEntrega;
    private Boolean toleranciaCheck;
    private Boolean esCopiaExacta;
    private String entornoUso;
    private String deptoCasaOficina;
    private String tipoEnvioStarken;

    // Campos para Auditoría Dividida
    private String justificacionCliente;
    private String justificacionFinanzas;
    private String justificacionIngenieria;

    // Si estás usando constructores o getters/setters manuales en lugar de Lombok,
    // asegúrate de generar el getLinkFormularioIngenieria() y setLinkFormularioIngenieria().
}